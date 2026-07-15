package com.codenow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.OperationLog;
import com.codenow.annotation.RateLimit;
import com.codenow.common.IpUtils;
import com.codenow.common.CommentStatus;
import com.codenow.common.R;
import com.codenow.dto.CommentDTO;
import com.codenow.dto.CommentPageVO;
import com.codenow.entity.BlogComment;
import com.codenow.service.CommentService;
import com.codenow.service.BlogArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.web.bind.annotation.*;

@Tag(name = "评论管理")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final BlogArticleService articleService;

    @Operation(summary = "获取文章评论树", description = "返回指定文章的树形评论列表（仅已通过审核）")
    @GetMapping("/article/{articleId}")
    public R<CommentPageVO> getCommentTree(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long articleId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        Page<BlogComment> page = commentService.getCommentTree(articleId, pageNum, Math.min(pageSize, 100));
        return R.ok(new CommentPageVO(page, commentService.countApproved(articleId)));
    }

    @RateLimit(maxCount = 5, timeWindow = 60, message = "评论过于频繁，请 1 分钟后再试")
    @Operation(summary = "发表评论", description = "发表文章评论，支持回复某条评论（楼中楼），需登录")
    @PostMapping
    public R<Void> save(@Valid @RequestBody CommentDTO dto, HttpServletRequest request) {
        if (articleService.getById(dto.getArticleId()) == null) {
            return R.error(404, "文章不存在");
        }
        long parentId = dto.getParentId() != null ? dto.getParentId() : CommentStatus.ROOT_PARENT_ID;
        if (parentId != CommentStatus.ROOT_PARENT_ID) {
            BlogComment parent = commentService.getById(parentId);
            if (parent == null || !dto.getArticleId().equals(parent.getArticleId())) {
                return R.error(400, "父评论不存在或不属于当前文章");
            }
        }
        BlogComment comment = new BlogComment();
        comment.setArticleId(dto.getArticleId());
        comment.setParentId(parentId);
        // 服务端 XSS 过滤：清除所有 HTML 标签，只保留纯文本
        comment.setContent(Jsoup.clean(dto.getContent(), Safelist.none()));
        comment.setNickname(Jsoup.clean(dto.getNickname(), Safelist.none()));
        comment.setEmail(dto.getEmail());
        comment.setIp(IpUtils.getRealIp(request));
        comment.setStatus(CommentStatus.PENDING);
        commentService.save(comment);
        return R.ok();
    }

    @Operation(summary = "分页查询评论（管理后台）", description = "分页查询所有评论，支持按文章筛选，需登录")
    @GetMapping
    public R<Page<BlogComment>> list(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "文章 ID（可选）") @RequestParam(required = false) Long articleId) {
        return R.ok(commentService.pageComments(pageNum, pageSize, articleId));
    }

    @OperationLog("删除评论")
    @Operation(summary = "删除评论", description = "删除评论及其所有子评论，需登录")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "评论 ID", example = "1") @PathVariable Long id) {
        commentService.deleteWithChildren(id);
        return R.ok();
    }
}
