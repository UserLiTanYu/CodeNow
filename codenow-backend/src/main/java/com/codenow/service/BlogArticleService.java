package com.codenow.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.dto.ArticleVO;
import com.codenow.entity.BlogArticle;

import java.util.List;

public interface BlogArticleService extends IService<BlogArticle> {

    /**
     * 保存文章及其标签关联
     */
    void saveArticleWithTags(BlogArticle article, List<Long> tagIds);

    /**
     * 更新文章及其标签关联
     */
    void updateArticleWithTags(BlogArticle article, List<Long> tagIds);

    /**
     * 删除文章及其标签关联
     */
    void deleteArticleWithTags(Long id);

    /**
     * 根据 ID 查询文章详情（含分类名称和标签列表）
     */
    ArticleVO getArticleVOById(Long id);

    /**
     * 分页查询文章列表（含分类名称和标签列表，支持按分类/标签筛选）
     */
    Page<ArticleVO> pageArticleVO(Integer pageNum, Integer pageSize, Long categoryId, Long tagId);

    /**
     * 分页查询已发布文章（用户端，仅 status=1）
     */
    Page<ArticleVO> pagePublishedArticles(Integer pageNum, Integer pageSize, Long categoryId, Long tagId);

    /**
     * 查询已发布文章详情（用户端），同时浏览量 +1
     */
    ArticleVO getPublishedArticleById(Long id);
}
