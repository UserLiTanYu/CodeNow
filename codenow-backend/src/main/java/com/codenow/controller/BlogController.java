package com.codenow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.R;
import com.codenow.dto.ArticleVO;
import com.codenow.entity.BlogCategory;
import com.codenow.entity.BlogTag;
import com.codenow.service.BlogArticleService;
import com.codenow.service.BlogCategoryService;
import com.codenow.service.BlogTagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "博客前台（公开）")
@RestController
@RequestMapping("/api/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogArticleService articleService;
    private final BlogCategoryService categoryService;
    private final BlogTagService tagService;

    @Operation(summary = "分页查询已发布文章", description = "仅返回已发布文章，置顶优先，支持按分类和标签筛选")
    @GetMapping("/articles")
    public R<Page<ArticleVO>> listArticles(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类 ID（可选）") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签 ID（可选）") @RequestParam(required = false) Long tagId) {
        return R.ok(articleService.pagePublishedArticles(pageNum, pageSize, categoryId, tagId));
    }

    @Operation(summary = "查询文章详情", description = "查询已发布文章详情，浏览量自动 +1")
    @GetMapping("/articles/{id}")
    public R<ArticleVO> getArticle(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        ArticleVO vo = articleService.getPublishedArticleById(id);
        if (vo == null) {
            return R.error(404, "文章不存在");
        }
        return R.ok(vo);
    }

    @Operation(summary = "查询所有分类")
    @GetMapping("/categories")
    public R<List<BlogCategory>> listCategories() {
        return R.ok(categoryService.list(
                new LambdaQueryWrapper<BlogCategory>().orderByAsc(BlogCategory::getSort)));
    }

    @Operation(summary = "查询所有标签")
    @GetMapping("/tags")
    public R<List<BlogTag>> listTags() {
        return R.ok(tagService.list());
    }
}
