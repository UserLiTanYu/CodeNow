package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.dto.ArticleVO;
import com.codenow.common.ArticleStatus;
import com.codenow.exception.BusinessException;
import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogArticleTag;
import com.codenow.entity.BlogCategory;
import com.codenow.entity.BlogTag;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogArticleTagMapper;
import com.codenow.mapper.BlogCategoryMapper;
import com.codenow.mapper.BlogTagMapper;
import com.codenow.service.BlogArticleService;
import com.codenow.service.HotArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle> implements BlogArticleService {

    private final BlogArticleTagMapper articleTagMapper;
    private final BlogCategoryMapper categoryMapper;
    private final BlogTagMapper tagMapper;
    private final HotArticleService hotArticleService;

    @Override
    @Transactional
    public void saveArticleWithTags(BlogArticle article, List<Long> tagIds) {
        // 1. 保存文章
        save(article);
        // 2. 保存文章-标签关联
        saveArticleTagRelations(article.getId(), tagIds);
    }

    @Override
    @Transactional
    public void updateArticleWithTags(BlogArticle article, List<Long> tagIds) {
        // 1. 更新文章
        updateById(article);
        // 2. 删除旧的标签关联
        articleTagMapper.delete(
                new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getArticleId, article.getId()));
        // 3. 保存新的标签关联
        saveArticleTagRelations(article.getId(), tagIds);
    }

    @Override
    @Transactional
    public void deleteArticleWithTags(Long id) {
        // 仅逻辑删除文章，保留标签关联，后续恢复文章时不会丢失标签。
        removeById(id);
    }

    @Override
    public boolean toggleStatus(Long id) {
        return baseMapper.toggleStatus(id) == 1;
    }

    @Override
    public boolean toggleTop(Long id) {
        return baseMapper.toggleTop(id) == 1;
    }

    @Override
    public ArticleVO getArticleVOById(Long id) {
        BlogArticle article = getById(id);
        if (article == null) {
            return null;
        }
        List<ArticleVO> voList = buildArticleVOBatch(List.of(article));
        return voList.isEmpty() ? null : voList.get(0);
    }

    @Override
    public Page<ArticleVO> pageArticleVO(Integer pageNum, Integer pageSize, Long categoryId, Long tagId) {
        // 如果按标签筛选，先查出该标签下的文章 ID 列表
        List<Long> articleIds = null;
        if (tagId != null) {
            List<BlogArticleTag> relations = articleTagMapper.selectList(
                    new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getTagId, tagId));
            articleIds = relations.stream().map(BlogArticleTag::getArticleId).collect(Collectors.toList());
            if (articleIds.isEmpty()) {
                // 该标签下没有文章，直接返回空页
                return new Page<>(pageNum, pageSize);
            }
        }

        // 构建查询条件
        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<BlogArticle>()
                .eq(categoryId != null, BlogArticle::getCategoryId, categoryId)
                .in(articleIds != null && !articleIds.isEmpty(), BlogArticle::getId, articleIds)
                .orderByDesc(BlogArticle::getIsTop)
                .orderByDesc(BlogArticle::getCreateTime);

        // 分页查询文章
        Page<BlogArticle> articlePage = page(new Page<>(pageNum, pageSize), wrapper);

        // 批量转换为 VO（避免 N+1 查询）
        Page<ArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        voPage.setRecords(buildArticleVOBatch(articlePage.getRecords()));
        return voPage;
    }

    @Override
    public Page<ArticleVO> pagePublishedArticles(Integer pageNum, Integer pageSize, Long categoryId, Long tagId,
                                                 String keyword) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        if (normalizedKeyword != null && normalizedKeyword.length() > 100) {
            throw new BusinessException(400, "搜索关键词不能超过 100 个字符");
        }
        if (normalizedKeyword != null && normalizedKeyword.isEmpty()) {
            normalizedKeyword = null;
        }

        Page<BlogArticle> articlePage = baseMapper.selectPublishedArticlePage(
                new Page<>(pageNum, pageSize), categoryId, tagId, normalizedKeyword);

        Page<ArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        voPage.setRecords(buildArticleVOBatch(articlePage.getRecords()));
        return voPage;
    }

    @Override
    @Transactional
    public ArticleVO getPublishedArticleById(Long id) {
        BlogArticle article = getById(id);
        if (article == null || !Objects.equals(article.getStatus(), ArticleStatus.PUBLISHED)) {
            return null;
        }
        // 浏览量 +1（原子更新后重新读取真实值，避免并发竞态）
        lambdaUpdate().eq(BlogArticle::getId, id)
                .setSql("view_count = view_count + 1")
                .update();
        // 重新读取数据库中的真实浏览量，推送到 Redis
        int actualViewCount = getById(id).getViewCount();
        article.setViewCount(actualViewCount);
        hotArticleService.incrementViewCount(id, actualViewCount);
        List<ArticleVO> voList = buildArticleVOBatch(List.of(article));
        return voList.isEmpty() ? null : voList.get(0);
    }

    /**
     * 保存文章-标签关联记录
     */
    private void saveArticleTagRelations(Long articleId, List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<Long> distinctTagIds = tagIds.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctTagIds.isEmpty()) {
            return;
        }
        List<BlogTag> existingTags = tagMapper.selectBatchIds(distinctTagIds);
        if (existingTags.size() != distinctTagIds.size()) {
            throw new BusinessException(400, "包含不存在的标签 ID");
        }
        for (Long tagId : distinctTagIds) {
            BlogArticleTag relation = new BlogArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tagId);
            articleTagMapper.insert(relation);
        }
    }

    /**
     * 批量构建 ArticleVO（避免 N+1 查询：分类和标签一次性批量加载）
     */
    @Override
    public List<ArticleVO> buildArticleVOBatch(List<BlogArticle> articles) {
        if (articles == null || articles.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> articleIds = articles.stream().map(BlogArticle::getId).collect(Collectors.toList());

        // 1. 批量查询分类
        Set<Long> categoryIds = articles.stream()
                .map(BlogArticle::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = Collections.emptyMap();
        if (!categoryIds.isEmpty()) {
            List<BlogCategory> categories = categoryMapper.selectBatchIds(categoryIds);
            categoryNameMap = categories.stream()
                    .collect(Collectors.toMap(BlogCategory::getId, BlogCategory::getName, (a, b) -> a));
        }

        // 2. 批量查询文章-标签关联
        List<BlogArticleTag> allRelations = articleTagMapper.selectList(
                new LambdaQueryWrapper<BlogArticleTag>().in(BlogArticleTag::getArticleId, articleIds));

        // 3. 批量查询标签
        Set<Long> tagIds = allRelations.stream().map(BlogArticleTag::getTagId).collect(Collectors.toSet());
        Map<Long, BlogTag> tagMap = Collections.emptyMap();
        if (!tagIds.isEmpty()) {
            List<BlogTag> tags = tagMapper.selectBatchIds(tagIds);
            tagMap = tags.stream().collect(Collectors.toMap(BlogTag::getId, t -> t, (a, b) -> a));
        }

        // 4. 按 articleId 分组标签
        Map<Long, List<Long>> articleTagMap = allRelations.stream()
                .collect(Collectors.groupingBy(BlogArticleTag::getArticleId,
                        Collectors.mapping(BlogArticleTag::getTagId, Collectors.toList())));

        // 5. 组装 VO
        Map<Long, String> finalCategoryNameMap = categoryNameMap;
        Map<Long, BlogTag> finalTagMap = tagMap;
        return articles.stream().map(article -> {
            ArticleVO vo = new ArticleVO();
            vo.setArticle(article);
            vo.setCategoryName(finalCategoryNameMap.get(article.getCategoryId()));
            List<Long> articleTagIds = articleTagMap.getOrDefault(article.getId(), Collections.emptyList());
            vo.setTags(articleTagIds.stream()
                    .map(finalTagMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }
}
