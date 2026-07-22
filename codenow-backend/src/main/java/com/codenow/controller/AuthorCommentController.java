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
/**
 * 作者评论管理控制器。
 * 提供作者对自身文章评论的查询和删除功能，管理员可操作所有作者的评论。
 */
public class AuthorCommentController {

    private final CommentService commentService;

    /**
     * 分页查询作者文章评论。
     * 支持按文章 ID 筛选，管理员可查看所有作者的评论。
     */
    @Operation(summary = "分页查询作者文章评论")
    @GetMapping
    public R<Page<BlogComment>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "文章 ID（可选）") @RequestParam(required = false) Long articleId) {
        //获取当前登录用户ID和管理员状态
        long userId = StpUtil.getLoginIdAsLong();
        boolean admin = StpUtil.hasRole("ADMIN");
        //调用业务层分页查询评论，管理员可查看所有作者的评论，普通作者只能查看自己文章的评论
        return R.ok(commentService.pageAuthorComments(pageNum, pageSize, articleId, userId, admin));
    }

    /**
     * 删除作者文章下的评论及其全部回复。
     */
    @OperationLog("作者删除文章评论")
    @Operation(summary = "删除作者文章下的评论及其全部回复")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        //获取当前登录用户ID和管理员状态
        long userId = StpUtil.getLoginIdAsLong();
        boolean admin = StpUtil.hasRole("ADMIN");
        //调用业务层删除评论及其所有子评论（含权限校验）
        commentService.deleteAuthorCommentWithChildren(id, userId, admin);
        //返回正确的响应结果
        return R.ok();
    }
}
