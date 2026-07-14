package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.dto.ArticleVO;
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
        // 1. 删除文章标签关联
        articleTagMapper.delete(
                new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getArticleId, id));
        // 2. 逻辑删除文章
        removeById(id);
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
    public Page<ArticleVO> pagePublishedArticles(Integer pageNum, Integer pageSize, Long categoryId, Long tagId) {
        List<Long> articleIds = null;
        if (tagId != null) {
            List<BlogArticleTag> relations = articleTagMapper.selectList(
                    new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getTagId, tagId));
            articleIds = relations.stream().map(BlogArticleTag::getArticleId).collect(Collectors.toList());
            if (articleIds.isEmpty()) {
                return new Page<>(pageNum, pageSize);
            }
        }

        LambdaQueryWrapper<BlogArticle> wrapper = new LambdaQueryWrapper<BlogArticle>()
                .eq(BlogArticle::getStatus, 1)  // 仅已发布
                .eq(categoryId != null, BlogArticle::getCategoryId, categoryId)
                .in(articleIds != null && !articleIds.isEmpty(), BlogArticle::getId, articleIds)
                .orderByDesc(BlogArticle::getIsTop)
                .orderByDesc(BlogArticle::getCreateTime);

        Page<BlogArticle> articlePage = page(new Page<>(pageNum, pageSize), wrapper);

        Page<ArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        voPage.setRecords(buildArticleVOBatch(articlePage.getRecords()));
        return voPage;
    }

    @Override
    @Transactional
    public ArticleVO getPublishedArticleById(Long id) {
        BlogArticle article = getById(id);
        if (article == null || article.getStatus() != 1) {
            return null;
        }
        // 浏览量 +1
        lambdaUpdate().eq(BlogArticle::getId, id)
                .setSql("view_count = view_count + 1")
                .update();
        int newViewCount = article.getViewCount() + 1;
        article.setViewCount(newViewCount);
        // 同步更新 Redis 缓存
        hotArticleService.incrementViewCount(id, newViewCount);
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
        for (Long tagId : tagIds) {
            BlogArticleTag relation = new BlogArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tagId);
            articleTagMapper.insert(relation);
        }
    }

    /**
     * 批量构建 ArticleVO（避免 N+1 查询：分类和标签一次性批量加载）
     */
    private List<ArticleVO> buildArticleVOBatch(List<BlogArticle> articles) {
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
