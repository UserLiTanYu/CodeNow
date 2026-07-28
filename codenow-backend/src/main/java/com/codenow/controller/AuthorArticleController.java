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
        //获取当前登录用户ID和管理员状态，分页查询作者文章列表
        //管理员可查看所有作者文章，普通作者只能查看自己的文章
        return R.ok(articleService.pageAuthorArticleVO(pageNum, pageSize, categoryId, tagId,
                currentUserId(), isAdmin()));
    }

    /**
     * 查询作者文章详情。
     */
    @Operation(summary = "查询作者文章详情")
    @GetMapping("/{id}")
    public R<ArticleVO> getById(@PathVariable Long id) {
        //获取当前登录用户ID和管理员状态，查询指定文章详情（含权限校验）
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
        //创建博客文章实体类
        BlogArticle article = new BlogArticle();
        //将DTO参数复制到实体对象中
        BeanUtils.copyProperties(dto, article);
        //获取当前登录用户ID，调用业务层保存文章和标签关联
        articleService.saveAuthorArticleWithTags(article, dto.getTagIds(), currentUserId());
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 修改作者文章。
     */
    @OperationLog("作者修改文章")
    @Operation(summary = "修改作者文章")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @Valid @RequestBody ArticleDTO dto) {
        //创建博客文章实体类
        BlogArticle article = new BlogArticle();
        //将DTO参数复制到实体对象中
        BeanUtils.copyProperties(dto, article);
        //设置文章ID，确保更新的是指定文章
        article.setId(id);
        //获取当前登录用户ID和管理员状态，调用业务层更新文章（含权限校验）
        articleService.updateAuthorArticleWithTags(article, dto.getTagIds(), currentUserId(), isAdmin());
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 删除作者文章。
     */
    @OperationLog("作者删除文章")
    @Operation(summary = "删除作者文章")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        //获取当前登录用户ID和管理员状态，调用业务层逻辑删除文章（含权限校验）
        articleService.deleteAuthorArticleWithTags(id, currentUserId(), isAdmin());
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 切换作者文章发布状态。
     */
    @OperationLog("作者切换文章状态")
    @Operation(summary = "切换作者文章发布状态")
    @PutMapping("/{id}/status")
    public R<Void> toggleStatus(@PathVariable Long id) {
        //获取当前登录用户ID和管理员状态，调用业务层切换文章发布状态（含权限校验）
        articleService.toggleAuthorStatus(id, currentUserId(), isAdmin());
        //返回正确的响应结果
        return R.ok();
    }

    private long currentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private boolean isAdmin() {
        return StpUtil.hasRole("ADMIN");
    }
}
