package com.codenow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.PageQuery;
import com.codenow.common.R;
import com.codenow.dto.ArticleDTO;
import com.codenow.entity.BlogArticle;
import com.codenow.service.BlogArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "文章管理")
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final BlogArticleService articleService;

    @Operation(summary = "分页查询文章列表", description = "按创建时间倒序分页查询")
    @GetMapping
    public R<Page<BlogArticle>> list(PageQuery pageQuery) {
        Page<BlogArticle> page = new Page<>(pageQuery.getPageNum(), pageQuery.getPageSize());
        Page<BlogArticle> result = articleService.page(page,
                new LambdaQueryWrapper<BlogArticle>().orderByDesc(BlogArticle::getCreateTime));
        return R.ok(result);
    }

    @Operation(summary = "查询文章详情", description = "根据 ID 查询文章完整信息")
    @GetMapping("/{id}")
    public R<BlogArticle> getById(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        BlogArticle article = articleService.getById(id);
        if (article == null) {
            return R.error(404, "文章不存在");
        }
        return R.ok(article);
    }

    @Operation(summary = "新增文章", description = "创建一篇新文章")
    @PostMapping
    public R<Void> save(@Valid @RequestBody ArticleDTO dto) {
        BlogArticle article = new BlogArticle();
        BeanUtils.copyProperties(dto, article);
        articleService.save(article);
        return R.ok();
    }

    @Operation(summary = "修改文章", description = "根据 ID 修改文章内容")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ArticleDTO dto) {
        BlogArticle article = articleService.getById(id);
        if (article == null) {
            return R.error(404, "文章不存在");
        }
        BeanUtils.copyProperties(dto, article);
        article.setId(id);
        articleService.updateById(article);
        return R.ok();
    }

    @Operation(summary = "删除文章", description = "根据 ID 逻辑删除文章")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        articleService.removeById(id);
        return R.ok();
    }
}
