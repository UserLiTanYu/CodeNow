package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.entity.BlogComment;
import com.codenow.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "作者评论管理")
@RestController
@RequestMapping("/api/author/comments")
@RequiredArgsConstructor
public class AuthorCommentController {

    private final CommentService commentService;

    @Operation(summary = "分页查询作者文章评论")
    @GetMapping
    public R<Page<BlogComment>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "文章 ID（可选）") @RequestParam(required = false) Long articleId) {
        long userId = StpUtil.getLoginIdAsLong();
        boolean admin = StpUtil.hasRole("ADMIN");
        return R.ok(commentService.pageAuthorComments(pageNum, pageSize, articleId, userId, admin));
    }

    @OperationLog("作者删除文章评论")
    @Operation(summary = "删除作者文章下的评论及其全部回复")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        boolean admin = StpUtil.hasRole("ADMIN");
        commentService.deleteAuthorCommentWithChildren(id, userId, admin);
        return R.ok();
    }
}
