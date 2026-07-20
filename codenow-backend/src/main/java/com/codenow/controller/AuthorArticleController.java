package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.ArticleDTO;
import com.codenow.dto.ArticleVO;
import com.codenow.entity.BlogArticle;
import com.codenow.service.BlogArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

@Tag(name = "作者文章管理")
@RestController
@RequestMapping("/api/author/articles")
@RequiredArgsConstructor
/**
 * 作者文章管理控制器。
 * 提供作者对自身文章的增删改查和状态切换等功能，管理员可操作所有作者文章。
 */
public class AuthorArticleController {

    private final BlogArticleService articleService;

    /**
     * 分页查询作者文章列表。
     * 支持按分类和标签筛选，管理员可查看所有作者文章。
     */
    @Operation(summary = "分页查询作者文章")
    @GetMapping
    public R<Page<ArticleVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                   @RequestParam(required = false) Long categoryId,
                                   @RequestParam(required = false) Long tagId) {
        return R.ok(articleService.pageAuthorArticleVO(pageNum, pageSize, categoryId, tagId,
                currentUserId(), isAdmin()));
    }

    /**
     * 查询作者文章详情。
     */
    @Operation(summary = "查询作者文章详情")
    @GetMapping("/{id}")
    public R<ArticleVO> getById(@PathVariable Long id) {
        return R.ok(articleService.getAuthorArticleVOById(id, currentUserId(), isAdmin()));
    }

    /**
     * 新增作者文章。
     * 创建文章并关联标签。
     */
    @OperationLog("作者新增文章")
    @Operation(summary = "新增作者文章")
    @PostMapping
    public R<Void> save(@Valid @RequestBody ArticleDTO dto) {
        BlogArticle article = new BlogArticle();
        BeanUtils.copyProperties(dto, article);
        articleService.saveAuthorArticleWithTags(article, dto.getTagIds(), currentUserId());
        return R.ok();
    }

    /**
     * 修改作者文章。
     */
    @OperationLog("作者修改文章")
    @Operation(summary = "修改作者文章")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleDTO dto) {
        BlogArticle article = new BlogArticle();
        BeanUtils.copyProperties(dto, article);
        article.setId(id);
        articleService.updateAuthorArticleWithTags(article, dto.getTagIds(), currentUserId(), isAdmin());
        return R.ok();
    }

    /**
     * 删除作者文章。
     */
    @OperationLog("作者删除文章")
    @Operation(summary = "删除作者文章")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        articleService.deleteAuthorArticleWithTags(id, currentUserId(), isAdmin());
        return R.ok();
    }

    /**
     * 切换作者文章发布状态。
     */
    @OperationLog("作者切换文章状态")
    @Operation(summary = "切换作者文章发布状态")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id) {
        articleService.toggleAuthorStatus(id, currentUserId(), isAdmin());
        return R.ok();
    }

    private long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private boolean isAdmin() {
        return StpUtil.hasRole("ADMIN");
    }
}
