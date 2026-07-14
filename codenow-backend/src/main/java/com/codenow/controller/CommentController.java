package com.codenow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.OperationLog;
import com.codenow.annotation.RateLimit;
import com.codenow.common.R;
import com.codenow.dto.CommentDTO;
import com.codenow.entity.BlogComment;
import com.codenow.service.CommentService;
import cn.dev33.satoken.stp.StpUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "评论管理")
@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "获取文章评论树", description = "返回指定文章的树形评论列表（仅已通过审核）")
    @GetMapping("/article/{articleId}")
    public R<List<BlogComment>> getCommentTree(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long articleId) {
        return R.ok(commentService.getCommentTree(articleId));
    }

    @RateLimit(maxCount = 5, timeWindow = 60, message = "评论过于频繁，请 1 分钟后再试")
    @Operation(summary = "发表评论", description = "发表文章评论，支持回复某条评论（楼中楼）")
    @PostMapping
    public R<Void> save(@Valid @RequestBody CommentDTO dto, HttpServletRequest request) {
        BlogComment comment = new BlogComment();
        comment.setArticleId(dto.getArticleId());
        comment.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);
        comment.setContent(dto.getContent());
        comment.setNickname(dto.getNickname());
        comment.setEmail(dto.getEmail());
        comment.setIp(getClientIp(request));
        comment.setStatus(1); // 默认直接通过审核
        commentService.save(comment);
        return R.ok();
    }

    @Operation(summary = "分页查询评论（管理后台）", description = "分页查询所有评论，支持按文章筛选")
    @GetMapping
    public R<Page<BlogComment>> list(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "文章 ID（可选）") @RequestParam(required = false) Long articleId) {
        StpUtil.checkLogin();
        return R.ok(commentService.pageComments(pageNum, pageSize, articleId));
    }

    @OperationLog("删除评论")
    @Operation(summary = "删除评论", description = "删除评论及其所有子评论")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "评论 ID", example = "1") @PathVariable Long id) {
        StpUtil.checkLogin();
        commentService.deleteWithChildren(id);
        return R.ok();
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
