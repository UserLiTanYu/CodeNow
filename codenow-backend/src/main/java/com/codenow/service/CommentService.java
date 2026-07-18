package com.codenow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.BlogComment;

public interface CommentService extends IService<BlogComment> {

    /**
     * 获取文章评论树形结构（已通过审核的评论）
     */
    Page<BlogComment> getCommentTree(Long articleId, Integer pageNum, Integer pageSize);

    /**
     * 统计文章下所有已审核评论（包含回复）。
     */
    long countApproved(Long articleId);

    /**
     * 分页查询评论列表（管理后台，含所有状态）
     */
    Page<BlogComment> pageComments(Integer pageNum, Integer pageSize, Long articleId);

    /**
     * 分页查询当前作者文章下的评论；管理员可查询全部文章评论。
     */
    Page<BlogComment> pageAuthorComments(Integer pageNum, Integer pageSize, Long articleId,
                                         Long currentUserId, boolean admin);

    /**
     * 删除评论及其所有子评论
     */
    void deleteWithChildren(Long id);

    /**
     * 删除作者可管理文章下的评论及其全部回复。
     */
    void deleteAuthorCommentWithChildren(Long id, Long currentUserId, boolean admin);
}
