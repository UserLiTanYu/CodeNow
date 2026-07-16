package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codenow.annotation.OperationLog;
import com.codenow.annotation.RateLimit;
import com.codenow.common.IpUtils;
import com.codenow.common.CommentStatus;
import com.codenow.common.R;
import com.codenow.common.UserStatus;
import com.codenow.dto.CommentDTO;
import com.codenow.dto.CommentPageVO;
import com.codenow.entity.BlogComment;
import com.codenow.entity.CommentLike;
import com.codenow.entity.UserNotification;
import com.codenow.entity.SysUser;
import com.codenow.service.CommentService;
import com.codenow.service.BlogArticleService;
import com.codenow.service.SysUserService;
import com.codenow.service.CommentLikeService;
import com.codenow.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

@Tag(name = "评论管理")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final BlogArticleService articleService;
    private final SysUserService userService;
    private final CommentLikeService commentLikeService;
    private final UserNotificationService notificationService;

    @Operation(summary = "获取文章评论树", description = "返回指定文章的树形评论列表（仅已通过审核）")
    @GetMapping("/article/{articleId}")
    public R<CommentPageVO> getCommentTree(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long articleId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BlogComment> page = commentService.getCommentTree(articleId, pageNum, Math.min(pageSize, 100));
        page.getRecords().forEach(this::enrichPublicFields);
        return R.ok(new CommentPageVO(page, commentService.countApproved(articleId)));
    }

    @RateLimit(maxCount = 5, timeWindow = 60, message = "评论过于频繁，请 1 分钟后再试")
    @Operation(summary = "发表评论", description = "发表文章评论，支持回复某条评论（楼中楼），需登录")
    @PostMapping
    public R<Void> save(@Valid @RequestBody CommentDTO dto, HttpServletRequest request) {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        if (user == null || UserStatus.BANNED.equalsIgnoreCase(user.getStatus())) {
            StpUtil.logout();
            return R.error(403, "账号不可用");
        }
        if (articleService.getById(dto.getArticleId()) == null) {
            return R.error(404, "文章不存在");
        }
        long parentId = dto.getParentId() != null ? dto.getParentId() : CommentStatus.ROOT_PARENT_ID;
        BlogComment parent = null;
        if (parentId != CommentStatus.ROOT_PARENT_ID) {
            parent = commentService.getById(parentId);
            if (parent == null || !dto.getArticleId().equals(parent.getArticleId())) {
                return R.error(400, "父评论不存在或不属于当前文章");
            }
        }
        BlogComment comment = new BlogComment();
        comment.setArticleId(dto.getArticleId());
        comment.setParentId(parentId);
        comment.setUserId(user.getId());
        // 服务端 XSS 过滤：清除所有 HTML 标签，只保留纯文本
        comment.setContent(Jsoup.clean(dto.getContent(), Safelist.none()));
        comment.setNickname(Jsoup.clean(
                user.getNickname() == null || user.getNickname().isBlank() ? user.getUsername() : user.getNickname(),
                Safelist.none()));
        comment.setEmail(user.getEmail());
        comment.setIp(IpUtils.getRealIp(request));
        comment.setStatus(CommentStatus.APPROVED);
        commentService.save(comment);
        if (parent != null && parent.getUserId() != null && !parent.getUserId().equals(user.getId())) {
            UserNotification notification = new UserNotification();
            notification.setUserId(parent.getUserId());
            notification.setType("COMMENT_REPLY");
            notification.setTitle("你的评论收到了新回复");
            String content = comment.getContent();
            notification.setContent(content.substring(0, Math.min(content.length(), 200)));
            notification.setArticleId(comment.getArticleId());
            notification.setCommentId(comment.getId());
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            notificationService.save(notification);
        }
        return R.ok();
    }

    @PostMapping("/{id}/likes")
    public R<Void> like(@PathVariable Long id) {
        if (commentService.getById(id) == null) return R.error(404, "评论不存在");
        CommentLike like = new CommentLike();
        like.setCommentId(id);
        like.setUserId(StpUtil.getLoginIdAsLong());
        like.setCreateTime(LocalDateTime.now());
        try {
            commentLikeService.save(like);
        } catch (DuplicateKeyException ignored) {
            // 重复点赞按幂等成功处理。
        }
        return R.ok();
    }

    @DeleteMapping("/{id}/likes")
    public R<Void> unlike(@PathVariable Long id) {
        commentLikeService.remove(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, id)
                .eq(CommentLike::getUserId, StpUtil.getLoginIdAsLong()));
        return R.ok();
    }

    @Operation(summary = "分页查询评论（管理后台）", description = "分页查询所有评论，支持按文章筛选，需登录")
    @GetMapping
    public R<Page<BlogComment>> list(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "文章 ID（可选）") @RequestParam(required = false) Long articleId) {
        StpUtil.checkRole("ADMIN");
        return R.ok(commentService.pageComments(pageNum, pageSize, articleId));
    }

    @OperationLog("删除评论")
    @Operation(summary = "删除评论", description = "删除评论及其所有子评论，需登录")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "评论 ID", example = "1") @PathVariable Long id) {
        StpUtil.checkRole("ADMIN");
        commentService.deleteWithChildren(id);
        return R.ok();
    }

    private void enrichPublicFields(BlogComment comment) {
        comment.setEmail(null);
        comment.setIp(null);
        comment.setLikeCount(commentLikeService.count(new LambdaQueryWrapper<CommentLike>()
                .eq(CommentLike::getCommentId, comment.getId())));
        if (StpUtil.isLogin()) {
            long userId = StpUtil.getLoginIdAsLong();
            comment.setLiked(commentLikeService.count(new LambdaQueryWrapper<CommentLike>()
                    .eq(CommentLike::getCommentId, comment.getId())
                    .eq(CommentLike::getUserId, userId)) > 0);
            comment.setOwnedByCurrentUser(comment.getUserId() != null && userId == comment.getUserId());
        } else {
            comment.setLiked(false);
            comment.setOwnedByCurrentUser(false);
        }
        if (comment.getChildren() != null) {
            comment.getChildren().forEach(this::enrichPublicFields);
        }
    }
}
