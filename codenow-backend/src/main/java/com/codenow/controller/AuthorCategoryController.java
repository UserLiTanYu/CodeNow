package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.CategoryDTO;
import com.codenow.entity.BlogCategory;
import com.codenow.service.BlogCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "作者分类管理")
@RestController
@RequestMapping("/api/author/categories")
@RequiredArgsConstructor
/**
 * 作者分类管理控制器。
 * 提供作者对自身分类的增删改查功能。
 */
public class AuthorCategoryController {

    private final BlogCategoryService categoryService;

    /**
     * 查询当前作者的分类列表（树形结构）。
     */
    @Operation(summary = "查询我的分类列表")
    @GetMapping
    public R<List<BlogCategory>> list() {
        //获取当前登录用户ID，查询该作者创建的分类树形结构
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(categoryService.listTreeByAuthor(userId));
    }

    /**
     * 新增分类。
     */
    @OperationLog("作者新增分类")
    @Operation(summary = "新增分类")
    @PostMapping
    public R<Void> save(@Valid @RequestBody CategoryDTO dto) {
        //获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        //创建分类实体类
        BlogCategory category = new BlogCategory();
        //将DTO参数复制到实体对象中
        BeanUtils.copyProperties(dto, category);
        //设置分类所属作者ID
        category.setAuthorId(userId);
        //调用业务层创建分类（含父分类校验）
        categoryService.createCategory(category);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 修改分类。
     */
    @OperationLog("作者修改分类")
    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "分类 ID") @PathVariable Long id,
            @Valid @RequestBody CategoryDTO dto) {
        //获取当前登录用户ID
        long userId = StpUtil.getLoginIdAsLong();
        //创建分类实体类
        BlogCategory category = new BlogCategory();
        //将DTO参数复制到实体对象中
        BeanUtils.copyProperties(dto, category);
        //调用业务层更新分类（含权限校验，仅允许修改自己创建的分类）
        categoryService.updateAuthorCategory(id, userId, category);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 删除分类。
     */
    @OperationLog("作者删除分类")
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "分类 ID") @PathVariable Long id) {
        //获取当前登录用户ID，调用业务层删除分类（含权限校验，仅允许删除自己创建的分类）
        long userId = StpUtil.getLoginIdAsLong();
        categoryService.deleteAuthorCategory(id, userId);
        //返回正确的响应结果
        return R.ok();
    }
}
