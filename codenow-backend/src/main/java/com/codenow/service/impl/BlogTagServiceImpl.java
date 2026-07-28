package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.BlogArticleTag;
import com.codenow.entity.BlogTag;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogArticleTagMapper;
import com.codenow.mapper.BlogTagMapper;
import com.codenow.service.BlogTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 博客标签服务实现类。
 * 管理标签查询以及作者作用域内的标签增删改操作。
 */
@Service
@RequiredArgsConstructor
public class BlogTagServiceImpl extends ServiceImpl<BlogTagMapper, BlogTag> implements BlogTagService {

    private final BlogArticleMapper articleMapper;
    private final BlogArticleTagMapper articleTagMapper;

    @Override
    public List<BlogTag> listByCreator(Long creatorId) {
        return list(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getCreatedBy, creatorId)
                .orderByDesc(BlogTag::getCreateTime));
    }

    @Override
    @Transactional
    public BlogTag createAuthorTag(String name, Long authorId) {
        String normalizedName = normalizeName(name);
        validateNameUnique(null, normalizedName, authorId);
        BlogTag tag = new BlogTag();
        tag.setName(normalizedName);
        tag.setCreatedBy(authorId);
        tag.setCreateTime(LocalDateTime.now());
        try {
            if (!save(tag)) {
                throw new BusinessException(500, "标签保存失败");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "标签名称已存在");
        }
        return tag;
    }

    @Override
    @Transactional
    public void updateAuthorTag(Long id, String name, Long authorId) {
        BlogTag existing = requireOwnedTag(id, authorId, "修改");
        String normalizedName = normalizeName(name);
        validateNameUnique(id, normalizedName, authorId);
        existing.setName(normalizedName);
        try {
            if (!updateById(existing)) {
                throw new BusinessException(409, "标签状态已发生变化，请刷新后重试");
            }
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "标签名称已存在");
        }
    }

    @Override
    @Transactional
    public void deleteAuthorTag(Long id, Long authorId) {
        requireOwnedTag(id, authorId, "删除");
        Long relationCount = articleTagMapper.selectCount(new LambdaQueryWrapper<BlogArticleTag>()
                .eq(BlogArticleTag::getTagId, id));
        if (relationCount != null && relationCount > 0) {
            throw new BusinessException(400, "请先移除使用该标签的文章");
        }
        try {
            if (!removeById(id)) {
                throw new BusinessException(409, "标签状态已发生变化，请刷新后重试");
            }
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(400, "请先移除使用该标签的文章");
        }
    }

    @Override
    public List<BlogTag> listByPublishedArticles() {
        List<Long> publishedTagIds = articleMapper.selectPublishedTagIds();
        if (publishedTagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<BlogTag>()
                .in(BlogTag::getId, publishedTagIds)
                .orderByDesc(BlogTag::getCreateTime));
    }

    private BlogTag requireOwnedTag(Long id, Long authorId, String action) {
        BlogTag tag = getById(id);
        if (tag == null) throw new BusinessException(404, "标签不存在");
        if (!Objects.equals(tag.getCreatedBy(), authorId)) {
            throw new BusinessException(403, "只能" + action + "自己创建的标签");
        }
        return tag;
    }

    private void validateNameUnique(Long id, String name, Long authorId) {
        long count = count(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getName, name)
                .eq(BlogTag::getCreatedBy, authorId)
                .ne(id != null, BlogTag::getId, id));
        if (count > 0) throw new BusinessException(409, "标签名称已存在");
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) throw new BusinessException(400, "标签名称不能为空");
        String normalized = name.trim();
        if (normalized.length() > 50) throw new BusinessException(400, "标签名称不能超过 50 字");
        return normalized;
    }
}
