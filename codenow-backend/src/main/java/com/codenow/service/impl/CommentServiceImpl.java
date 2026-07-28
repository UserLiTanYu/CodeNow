package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.BlogComment;
import com.codenow.common.CommentStatus;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.BlogCommentMapper;
import com.codenow.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论树服务。根评论分页、后代按层加载；删除时按子节点到父节点的顺序维护树结构完整性。
 */
@Service
public class CommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements CommentService {

    /**
     * 获取文章评论树。
     * 仅分页查询根评论，按层批量加载后代，查询次数与树深度相关而非评论总数。
     *
     * @param articleId 文章 ID
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @return 包含评论树结构的分页结果
     */
    @Override
    public Page<BlogComment> getCommentTree(Long articleId, Integer pageNum, Integer pageSize) {
        // 仅分页查询根评论，避免一次性把文章的全部评论载入内存。
        Page<BlogComment> rootPage = page(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getArticleId, articleId)
                .eq(BlogComment::getStatus, CommentStatus.APPROVED)
                .eq(BlogComment::getParentId, CommentStatus.ROOT_PARENT_ID)
                .orderByAsc(BlogComment::getCreateTime));

        List<BlogComment> roots = rootPage.getRecords();
        if (roots.isEmpty()) {
            return rootPage;
        }

        // 按层批量加载当前页根评论的后代，查询次数与树深度相关，而非评论总数相关。
        List<BlogComment> descendants = new ArrayList<>();
        Set<Long> visited = roots.stream().map(BlogComment::getId).collect(Collectors.toSet());
        List<Long> parentIds = new ArrayList<>(visited);
        while (!parentIds.isEmpty()) {
            List<BlogComment> children = list(new LambdaQueryWrapper<BlogComment>()
                    .eq(BlogComment::getArticleId, articleId)
                    .eq(BlogComment::getStatus, CommentStatus.APPROVED)
                    .in(BlogComment::getParentId, parentIds)
                    .orderByAsc(BlogComment::getCreateTime));
            List<BlogComment> newChildren = children.stream()
                    .filter(comment -> visited.add(comment.getId()))
                    .toList();
            descendants.addAll(newChildren);
            parentIds = newChildren.stream().map(BlogComment::getId).toList();
        }

        Map<Long, List<BlogComment>> parentMap = descendants.stream()
                .collect(Collectors.groupingBy(BlogComment::getParentId));

        for (BlogComment root : roots) {
            buildChildren(root, parentMap);
        }
        rootPage.setRecords(roots);
        return rootPage;
    }

    /**
     * 分页查询评论列表（管理员用）
     *
     * @param pageNum   页码
     * @param pageSize  每页大小
     * @param articleId 文章 ID（可选，不传则查全部）
     * @return 分页结果
     */
    @Override
    public Page<BlogComment> pageComments(Integer pageNum, Integer pageSize, Long articleId) {
        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<BlogComment>()
                .eq(articleId != null, BlogComment::getArticleId, articleId)
                .orderByDesc(BlogComment::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    /**
     * 分页查询作者评论列表。
     * 管理员可查看所有评论，普通作者只能查看自己文章下的评论。
     *
     * @param pageNum       页码
     * @param pageSize      每页大小
     * @param articleId     文章 ID（可选）
     * @param currentUserId 当前登录用户 ID
     * @param admin         是否为管理员
     * @return 分页结果
     */
    @Override
    public Page<BlogComment> pageAuthorComments(Integer pageNum, Integer pageSize, Long articleId,
                                                Long currentUserId, boolean admin) {
        if (pageNum == null || pageNum < 1 || pageSize == null || pageSize < 1 || pageSize > 100) {
            throw new BusinessException(400, "分页参数不合法");
        }
        return baseMapper.selectAuthorCommentPage(new Page<>(pageNum, pageSize), articleId, currentUserId, admin);
    }

    /**
     * 统计文章已审核通过的评论数量
     *
     * @param articleId 文章 ID
     * @return 已审核评论数量
     */
    @Override
    public long countApproved(Long articleId) {
        return count(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getArticleId, articleId)
                .eq(BlogComment::getStatus, CommentStatus.APPROVED));
    }

    /**
     * 递归删除评论及其所有子评论。
     * 后序遍历保证先删除后代，再删除自身，兼容数据库外键约束。
     *
     * @param id 评论 ID
     */
    @Override
    @Transactional
    public void deleteWithChildren(Long id) {
        // 递归删除所有子评论
        deleteChildrenRecursive(id);
        // 删除自身
        removeById(id);
    }

    /**
     * 作者删除自己的评论及其子评论。
     * 通过条件查询校验评论归属权后递归删除。
     *
     * @param id            评论 ID
     * @param currentUserId 当前登录用户 ID
     * @param admin         是否为管理员
     */
    @Override
    @Transactional
    public void deleteAuthorCommentWithChildren(Long id, Long currentUserId, boolean admin) {
        BlogComment root = baseMapper.selectAuthorCommentForUpdate(id, currentUserId, admin);
        if (root == null) {
            throw new BusinessException(404, "评论不存在或无权操作");
        }
        deleteOwnedChildrenRecursive(root.getId(), root.getArticleId());
        removeById(root.getId());
    }

    private void deleteChildrenRecursive(Long parentId) {
        // 后序遍历保证先删除后代，再删除直接子节点，兼容数据库外键约束。
        List<BlogComment> children = list(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getParentId, parentId));
        for (BlogComment child : children) {
            deleteChildrenRecursive(child.getId());
            removeById(child.getId());
        }
    }

    private void deleteOwnedChildrenRecursive(Long parentId, Long articleId) {
        List<BlogComment> children = list(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getParentId, parentId)
                .eq(BlogComment::getArticleId, articleId));
        for (BlogComment child : children) {
            deleteOwnedChildrenRecursive(child.getId(), articleId);
            removeById(child.getId());
        }
    }

    private void buildChildren(BlogComment parent, Map<Long, List<BlogComment>> parentMap) {
        List<BlogComment> children = parentMap.getOrDefault(parent.getId(), Collections.emptyList());
        parent.setChildren(children);
        for (BlogComment child : children) {
            buildChildren(child, parentMap);
        }
    }
}
