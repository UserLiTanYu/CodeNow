package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.BlogComment;
import com.codenow.mapper.BlogCommentMapper;
import com.codenow.service.CommentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl extends ServiceImpl<BlogCommentMapper, BlogComment> implements CommentService {

    @Override
    public List<BlogComment> getCommentTree(Long articleId) {
        // 1. 查出该文章所有已通过审核的评论
        List<BlogComment> allComments = list(new LambdaQueryWrapper<BlogComment>()
                .eq(BlogComment::getArticleId, articleId)
                .eq(BlogComment::getStatus, 1)
                .orderByAsc(BlogComment::getCreateTime));

        if (allComments.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 按 parentId 分组
        Map<Long, List<BlogComment>> parentMap = allComments.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));

        // 3. 从顶级评论（parentId=0）开始，递归填充 children
        List<BlogComment> rootComments = parentMap.getOrDefault(0L, Collections.emptyList());
        for (BlogComment root : rootComments) {
            buildChildren(root, parentMap);
        }

        return rootComments;
    }

    @Override
    public Page<BlogComment> pageComments(Integer pageNum, Integer pageSize, Long articleId) {
        LambdaQueryWrapper<BlogComment> wrapper = new LambdaQueryWrapper<BlogComment>()
                .eq(articleId != null, BlogComment::getArticleId, articleId)
                .orderByDesc(BlogComment::getCreateTime);
        return page(new Page<>(pageNum, pageSize), wrapper);
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
