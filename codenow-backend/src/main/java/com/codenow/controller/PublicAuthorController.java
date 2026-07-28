package com.codenow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.RateLimit;
import com.codenow.common.R;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.PublicAuthorVO;
import com.codenow.entity.BlogCategory;
import com.codenow.entity.BlogTag;
import com.codenow.service.BlogCategoryService;
import com.codenow.service.BlogTagService;
import com.codenow.service.PublicAuthorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "公开作者")
@RestController
@RequestMapping("/api/blog/authors")
@RequiredArgsConstructor
/**
 * 公开作者控制器。
 * 提供作者发现、作者主页和作者文章列表等公开访问接口。
 */
public class PublicAuthorController {
    private final PublicAuthorService service;
    private final BlogCategoryService categoryService;
    private final BlogTagService tagService;

    /**
     * 发现作者。
     * 仅返回账号正常、作者身份有效且具备公开资料的作者。
     */
    @RateLimit(maxCount = 30, timeWindow = 10, message = "请求过于频繁，请稍后再试")
    @Operation(summary = "发现作者", description = "仅返回账号正常、作者身份有效且具备公开资料的作者")
    @GetMapping
    public R<Page<PublicAuthorVO>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "12") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @Parameter(description = "排序方式：popular（热门）或 latest（最近活跃）")
            @RequestParam(defaultValue = "popular") String sort) {
        //分页查询公开作者列表，支持关键词搜索和排序方式筛选
        return R.ok(service.pagePublicAuthors(pageNum, pageSize, keyword, sort));
    }

    /**
     * 查看作者公开主页。
     */
    @RateLimit(maxCount = 30, timeWindow = 10, message = "请求过于频繁，请稍后再试")
    @Operation(summary = "查看作者公开主页")
    @GetMapping("/{userId}")
    public R<PublicAuthorVO> detail(@PathVariable Long userId) {
        //根据用户ID查询作者公开主页信息
        return R.ok(service.getPublicAuthor(userId));
    }

    /**
     * 查看作者已发布文章。
     * 支持按排序方式、分类、标签和关键词筛选。
     */
    @RateLimit(maxCount = 30, timeWindow = 10, message = "请求过于频繁，请稍后再试")
    @Operation(summary = "查看作者已发布文章")
    @GetMapping("/{userId}/articles")
    public R<Page<ArticleVO>> articles(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "排序方式：learning（学习顺序）、latest（最新）或 mostViewed（最多阅读）")
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String keyword) {
        //分页查询指定作者的已发布文章，支持排序方式、分类、标签和关键词筛选
        return R.ok(service.pagePublicAuthorArticles(userId, pageNum, pageSize, sort, categoryId, tagId, keyword));
    }

    /**
     * 查询作者的分类列表。
     * 返回指定作者创建的分类（树形结构）。
     */
    @RateLimit(maxCount = 30, timeWindow = 10, message = "请求过于频繁，请稍后再试")
    @Operation(summary = "查询作者的分类列表", description = "返回指定作者创建的分类（树形结构）")
    @GetMapping("/{userId}/categories")
    public R<List<BlogCategory>> authorCategories(@PathVariable Long userId) {
        service.getPublicAuthor(userId);
        //查询指定作者创建的分类树形结构
        return R.ok(categoryService.listTreeByAuthor(userId));
    }

    /**
     * 查询作者的标签列表。
     * 返回指定作者创建的标签。
     */
    @RateLimit(maxCount = 30, timeWindow = 10, message = "请求过于频繁，请稍后再试")
    @Operation(summary = "查询作者的标签列表", description = "返回指定作者创建的标签")
    @GetMapping("/{userId}/tags")
    public R<List<BlogTag>> authorTags(@PathVariable Long userId) {
        service.getPublicAuthor(userId);
        //查询指定作者创建的标签列表
        return R.ok(tagService.listByCreator(userId));
    }
}
