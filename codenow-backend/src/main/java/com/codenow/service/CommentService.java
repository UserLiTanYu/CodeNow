package com.codenow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.BlogComment;

/**
 * 评论服务接口
 */
public interface CommentService extends IService<BlogComment> {

    /**
     * 获取文章评论树形结构（已通过审核的评论）
     *
     * @param articleId 文章ID
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @return 分页结果
     */
    Page<BlogComment> getCommentTree(Long articleId, Integer pageNum, Integer pageSize);

    /**
     * 统计文章下所有已审核评论（包含回复）
     *
     * @param articleId 文章ID
     * @return 已审核评论总数
     */
    long countApproved(Long articleId);

    /**
     * 分页查询评论列表（管理后台，含所有状态）
     *
     * @param pageNum   页码
     * @param pageSize  每页数量
     * @param articleId 文章ID（可选）
     * @return 分页结果
     */
    Page<BlogComment> pageComments(Integer pageNum, Integer pageSize, Long articleId);

    /**
     * 分页查询当前作者文章下的评论；管理员可查询全部文章评论
     *
     * @param pageNum       页码
     * @param pageSize      每页数量
     * @param articleId     文章ID（可选）
     * @param currentUserId 当前用户ID
     * @param admin         是否为管理员
     * @return 分页结果
     */
    Page<BlogComment> pageAuthorComments(Integer pageNum, Integer pageSize, Long articleId,
                                         Long currentUserId, boolean admin);

    /**
     * 删除评论及其所有子评论
     *
     * @param id 评论ID
     */
    void deleteWithChildren(Long id);

    /**
     * 删除作者可管理文章下的评论及其全部回复
     *
     * @param id            评论ID
     * @param currentUserId 当前用户ID
     * @param admin         是否为管理员
     */
    void deleteAuthorCommentWithChildren(Long id, Long currentUserId, boolean admin);
}
