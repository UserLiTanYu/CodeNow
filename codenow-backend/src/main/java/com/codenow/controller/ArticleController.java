package com.codenow.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.ArticleDTO;
import com.codenow.dto.ArticleVO;
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

    @Operation(summary = "分页查询文章列表", description = "按创建时间倒序分页查询，支持按分类和标签筛选")
    @GetMapping
    public R<Page<ArticleVO>> list(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类 ID（可选，按分类筛选）") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签 ID（可选，按标签筛选）") @RequestParam(required = false) Long tagId) {
        return R.ok(articleService.pageArticleVO(pageNum, pageSize, categoryId, tagId));
    }

    @Operation(summary = "查询文章详情", description = "根据 ID 查询文章完整信息（含分类名称和标签列表）")
    @GetMapping("/{id}")
    public R<ArticleVO> getById(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        ArticleVO vo = articleService.getArticleVOById(id);
        if (vo == null) {
            return R.error(404, "文章不存在");
        }
        return R.ok(vo);
    }

    @OperationLog("新增文章")
    @Operation(summary = "新增文章", description = "创建一篇新文章，可同时关联标签")
    @PostMapping
    public R<Void> save(@Valid @RequestBody ArticleDTO dto) {
        BlogArticle article = new BlogArticle();
        BeanUtils.copyProperties(dto, article);
        articleService.saveArticleWithTags(article, dto.getTagIds());
        return R.ok();
    }

    @OperationLog("修改文章")
    @Operation(summary = "修改文章", description = "根据 ID 修改文章内容和标签关联")
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
        articleService.updateArticleWithTags(article, dto.getTagIds());
        return R.ok();
    }

    @OperationLog("删除文章")
    @Operation(summary = "删除文章", description = "根据 ID 逻辑删除文章及其标签关联")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        articleService.deleteArticleWithTags(id);
        return R.ok();
    }

    @OperationLog("切换文章状态")
    @Operation(summary = "切换文章状态", description = "切换文章的草稿/发布状态（0=草稿, 1=已发布）")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        BlogArticle article = articleService.getById(id);
        if (article == null) {
            return R.error(404, "文章不存在");
        }
        article.setStatus(article.getStatus() == 0 ? 1 : 0);
        articleService.updateById(article);
        return R.ok();
    }

    @OperationLog("切换文章置顶")
    @Operation(summary = "切换文章置顶", description = "切换文章的置顶状态（0=不置顶, 1=置顶）")
    @PutMapping("/{id}/top")
    public R<Void> toggleTop(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        BlogArticle article = articleService.getById(id);
        if (article == null) {
            return R.error(404, "文章不存在");
        }
        article.setIsTop(article.getIsTop() == 0 ? 1 : 0);
        articleService.updateById(article);
        return R.ok();
    }
}
