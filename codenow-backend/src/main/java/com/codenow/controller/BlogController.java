package com.codenow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.RateLimit;
import com.codenow.common.R;
import com.codenow.common.ArticleStatus;
import com.codenow.dto.ArticleVO;
import com.codenow.entity.BlogCategory;
import com.codenow.entity.BlogTag;
import com.codenow.service.BlogArticleService;
import com.codenow.service.BlogCategoryService;
import com.codenow.service.BlogTagService;
import com.codenow.service.HotArticleService;
import com.codenow.entity.BlogArticle;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Collections;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "博客前台（公开）")
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
/**
 * 博客前台控制器（公开接口）。
 * 提供已发布文章的查询、热门文章、分类和标签列表等公开访问功能。
 */
public class BlogController {

    private final BlogArticleService articleService;
    private final BlogCategoryService categoryService;
    private final BlogTagService tagService;
    private final HotArticleService hotArticleService;

    /**
     * 分页查询已发布文章。
     * 仅返回已发布文章，置顶优先，支持按分类、标签和关键词筛选，并可按发布时间或阅读量排序。
     */
    @RateLimit(maxCount = 30, timeWindow = 10, message = "请求过于频繁，请稍后再试")
    @Operation(summary = "分页查询已发布文章", description = "仅返回已发布文章，置顶优先，支持按分类、标签和关键词筛选，并可按发布时间或阅读量排序")
    @GetMapping("/articles")
    public R<Page<ArticleVO>> listArticles(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类 ID（可选）") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签 ID（可选）") @RequestParam(required = false) Long tagId,
            @Parameter(description = "搜索关键词，匹配标题、摘要、分类和标签（可选）")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "排序方式：learning（学习顺序）、latest（最新发布）或 mostViewed（最多阅读）")
            @RequestParam(defaultValue = "learning") String sort) {
        //调用业务层分页查询已发布文章，支持分类、标签、关键词筛选和多种排序方式
        return R.ok(articleService.pagePublishedArticles(pageNum, pageSize, categoryId, tagId, keyword, sort));
    }

    /**
     * 查询热门文章。
     * 固定返回浏览量 Top 3 的已发布文章；Redis 为空时从数据库重建缓存。
     */
    @RateLimit(maxCount = 20, timeWindow = 10, message = "请求过于频繁，请稍后再试")
    @Operation(summary = "查询热门文章", description = "固定返回浏览量 Top 3 的已发布文章；Redis 为空时从数据库重建缓存")
    @GetMapping("/articles/hot")
    public R<List<ArticleVO>> hotArticles() {
        //从Redis获取热门文章ID列表（按浏览量排序）
        List<Long> ids = hotArticleService.getHotArticleIds();
        //如果没有热门文章，直接返回空列表
        if (ids.isEmpty()) {
            return R.ok(Collections.emptyList());
        }
        //批量查询文章，保持Redis返回的排序
        List<BlogArticle> articles = articleService.listByIds(ids);
        //按ids顺序排序并过滤已发布文章（listByIds不保证顺序）
        Map<Long, BlogArticle> articleMap = articles.stream()
                .filter(a -> Objects.equals(a.getStatus(), ArticleStatus.PUBLISHED))
                .collect(Collectors.toMap(BlogArticle::getId, a -> a, (a, b) -> a));
        List<BlogArticle> orderedArticles = ids.stream()
                .map(articleMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        //一次性批量构建VO（避免N+1查询），返回热门文章列表
        return R.ok(articleService.buildArticleVOBatch(orderedArticles));
    }

    /**
     * 查询文章详情。
     * 查询已发布文章详情，浏览量自动加一。
     */
    @Operation(summary = "查询文章详情", description = "查询已发布文章详情，浏览量自动 +1")
    @GetMapping("/articles/{id}")
    public R<ArticleVO> getArticle(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        //查询已发布文章详情，浏览量自动加一，返回文章完整信息
        ArticleVO vo = articleService.getPublishedArticleById(id);
        //文章不存在或未发布时返回404错误
        if (vo == null) {
            return R.error(404, "文章不存在");
        }
        //返回文章详情
        return R.ok(vo);
    }

    /**
     * 查询所有分类。
     * 仅返回至少有一篇已发布文章的分类。
     */
    @Operation(summary = "查询所有分类", description = "仅返回至少有一篇已发布文章的分类")
    @GetMapping("/categories")
    public R<List<BlogCategory>> listCategories() {
        //查询分类树形结构，仅包含至少有一篇已发布文章的分类
        return R.ok(categoryService.listTreeByPublishedArticles());
    }

    /**
     * 查询所有标签。
     * 仅返回至少关联了一篇已发布文章的标签。
     */
    @Operation(summary = "查询所有标签", description = "仅返回至少关联了一篇已发布文章的标签")
    @GetMapping("/tags")
    public R<List<BlogTag>> listTags() {
        //查询标签列表，仅包含至少关联了一篇已发布文章的标签
        return R.ok(tagService.listByPublishedArticles());
    }
}
