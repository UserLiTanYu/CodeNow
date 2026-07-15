package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.BlogComment;
import com.codenow.common.CommentStatus;
import com.codenow.mapper.BlogCommentMapper;
import com.codenow.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements CommentService {

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

    @Override
    public Page<BlogComment> pageComments(Integer pageNum, Integer pageSize, Long articleId) {
        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<BlogComment>()
                .eq(articleId != null, BlogComment::getArticleId, articleId)
                .orderByDesc(BlogComment::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public long countApproved(Long articleId) {
        return count(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getArticleId, articleId)
                .eq(BlogComment::getStatus, CommentStatus.APPROVED));
    }

    @Override
    @Transactional
    public void deleteWithChildren(Long id) {
        // 递归删除所有子评论
        deleteChildrenRecursive(id);
        // 删除自身
        removeById(id);
    }

    private void deleteChildrenRecursive(Long parentId) {
        List<BlogComment> children = list(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getParentId, parentId));
        for (BlogComment child : children) {
            deleteChildrenRecursive(child.getId());
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
