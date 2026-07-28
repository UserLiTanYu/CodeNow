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
/**
 * 文章管理控制器。
 * 提供文章的增删改查、状态切换和置顶管理等功能。
 */
public class ArticleController {

    private final BlogArticleService articleService;

    /**
     * 分页查询文章列表。
     * 按创建时间倒序分页查询，支持按分类和标签筛选。
     */
    @Operation(summary = "分页查询文章列表", description = "按创建时间倒序分页查询，支持按分类和标签筛选")
    @GetMapping
    public R<Page<ArticleVO>> list(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "分类 ID（可选，按分类筛选）") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "标签 ID（可选，按标签筛选）") @RequestParam(required = false) Long tagId) {
        //调用业务层分页查询方法，传入分页参数和筛选条件，返回包含文章列表的分页结果
        return R.ok(articleService.pageArticleVO(pageNum, pageSize, categoryId, tagId));
    }

    /**
     * 查询文章详情。
     * 根据 ID 查询文章完整信息，包含分类名称和标签列表。
     */
    @Operation(summary = "查询文章详情", description = "根据 ID 查询文章完整信息（含分类名称和标签列表）")
    @GetMapping("/{id}")
    public R<ArticleVO> getById(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        //根据文章ID查询文章详情视图对象（包含分类名称和标签列表）
        ArticleVO vo = articleService.getArticleVOById(id);
        //文章不存在时返回404错误
        if (vo == null) {
            return R.error(404, "文章不存在");
        }
        //返回文章详情
        return R.ok(vo);
    }

    /**
     * 新增文章。
     * 创建一篇新文章，可同时关联标签。
     */
    @OperationLog("新增文章")
    @Operation(summary = "新增文章", description = "创建一篇新文章，可同时关联标签")
    @PostMapping
    public R<Void> save(@Valid @RequestBody ArticleDTO dto) {
        //创建博客文章实体类
        BlogArticle article = new BlogArticle();
        //将参数传入实体类
        BeanUtils.copyProperties(dto, article);
        //调用业务方法，传入文章实体和标签id列表
        articleService.saveArticleWithTags(article, dto.getTagIds());
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 修改文章。
     * 根据 ID 修改文章内容和标签关联。
     */
    @OperationLog("修改文章")
    @Operation(summary = "修改文章", description = "根据 ID 修改文章内容和标签关联")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody ArticleDTO dto) {
        //根据文章ID查询原文章记录，用于后续更新
        BlogArticle article = articleService.getById(id);
        //文章不存在时返回404错误
        if (article == null) {
            return R.error(404, "文章不存在");
        }
        //将DTO中的新参数复制到已查询到的实体对象中
        BeanUtils.copyProperties(dto, article);
        //确保实体ID与路径参数一致，防止误更新其他记录
        article.setId(id);
        //调用业务层更新方法，在同一事务中更新文章内容和标签关联
        articleService.updateArticleWithTags(article, dto.getTagIds());
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 删除文章。
     * 根据 ID 逻辑删除文章并保留标签关联，以支持后续恢复。
     */
    @OperationLog("删除文章")
    @Operation(summary = "删除文章", description = "根据 ID 逻辑删除文章并保留标签关联，以支持后续恢复")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        //调用业务层逻辑删除方法，仅标记删除状态，保留标签关联以便后续恢复
        articleService.deleteArticleWithTags(id);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 切换文章状态。
     * 切换文章的草稿/发布状态（0=草稿, 1=已发布）。
     */
    @OperationLog("切换文章状态")
    @Operation(summary = "切换文章状态", description = "切换文章的草稿/发布状态（0=草稿, 1=已发布）")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        //调用业务层状态切换方法，使用原子SQL避免并发竞态问题
        if (!articleService.toggleStatus(id)) {
            //切换失败说明文章不存在，返回404错误
            return R.error(404, "文章不存在");
        }
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 切换文章置顶状态。
     * 切换文章的置顶状态（0=不置顶, 1=置顶）。
     */
    @OperationLog("切换文章置顶")
    @Operation(summary = "切换文章置顶", description = "切换文章的置顶状态（0=不置顶, 1=置顶）")
    @PutMapping("/{id}/top")
    public R<Void> toggleTop(
            @Parameter(description = "文章 ID", example = "1") @PathVariable Long id) {
        //调用业务层置顶切换方法，使用原子SQL避免并发竞态问题
        if (!articleService.toggleTop(id)) {
            //切换失败说明文章不存在，返回404错误
            return R.error(404, "文章不存在");
        }
        //返回正确的响应结果
        return R.ok();
    }
}
