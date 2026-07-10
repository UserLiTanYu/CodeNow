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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle> implements BlogArticleService {

    private final BlogArticleTagMapper articleTagMapper;
    private final BlogCategoryMapper categoryMapper;
    private final BlogTagMapper tagMapper;

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
        return buildArticleVO(article);
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

        // 转换为 VO
        Page<ArticleVO> voPage = new Page<>(articlePage.getCurrent(), articlePage.getSize(), articlePage.getTotal());
        List<ArticleVO> voList = articlePage.getRecords().stream()
                .map(this::buildArticleVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
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
        List<ArticleVO> voList = articlePage.getRecords().stream()
                .map(this::buildArticleVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
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
        article.setViewCount(article.getViewCount() + 1);
        return buildArticleVO(article);
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
     * 构建 ArticleVO（填充分类名称和标签列表）
     */
    private ArticleVO buildArticleVO(BlogArticle article) {
        ArticleVO vo = new ArticleVO();
        vo.setArticle(article);

        // 查询分类名称
        if (article.getCategoryId() != null) {
            BlogCategory category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }

        // 查询标签列表
        List<BlogArticleTag> relations = articleTagMapper.selectList(
                new LambdaQueryWrapper<BlogArticleTag>().eq(BlogArticleTag::getArticleId, article.getId()));
        if (!relations.isEmpty()) {
            List<Long> tagIds = relations.stream().map(BlogArticleTag::getTagId).collect(Collectors.toList());
            List<BlogTag> tags = tagMapper.selectBatchIds(tagIds);
            vo.setTags(tags);
        } else {
            vo.setTags(Collections.emptyList());
        }

        return vo;
    }
}
