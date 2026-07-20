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

@Tag(name = "管理员分类管理")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
/**
 * 管理员分类管理控制器。
 * 提供管理员对分类的增删改查功能。
 */
public class CategoryController {

    private final BlogCategoryService categoryService;

    /**
     * 查询当前管理员的分类列表（树形结构）。
     */
    @Operation(summary = "查询分类列表")
    @GetMapping
    public R<List<BlogCategory>> list() {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(categoryService.listTreeByAuthor(userId));
    }

    /**
     * 新增分类。
     */
    @OperationLog("管理员新增分类")
    @Operation(summary = "新增分类")
    @PostMapping
    public R<Void> save(@Valid @RequestBody CategoryDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        BlogCategory category = new BlogCategory();
        BeanUtils.copyProperties(dto, category);
        category.setAuthorId(userId);
        categoryService.createCategory(category);
        return R.ok();
    }

    /**
     * 修改分类。
     */
    @OperationLog("管理员修改分类")
    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "分类 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody CategoryDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        BlogCategory category = new BlogCategory();
        BeanUtils.copyProperties(dto, category);
        categoryService.updateAuthorCategory(id, userId, category);
        return R.ok();
    }

    /**
     * 删除分类。
     */
    @OperationLog("管理员删除分类")
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "分类 ID", example = "1") @PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        categoryService.deleteAuthorCategory(id, userId);
        return R.ok();
    }
}
