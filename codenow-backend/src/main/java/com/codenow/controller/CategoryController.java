package com.codenow.controller;

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

@Tag(name = "分类管理")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final BlogCategoryService categoryService;

    @Operation(summary = "查询分类列表", description = "按排序号升序查询全部分类")
    @GetMapping
    public R<List<BlogCategory>> list() {
        return R.ok(categoryService.listTree());
    }

    @OperationLog("新增分类")
    @Operation(summary = "新增分类", description = "创建一个新分类")
    @PostMapping
    public R<Void> save(@Valid @RequestBody CategoryDTO dto) {
        BlogCategory category = new BlogCategory();
        BeanUtils.copyProperties(dto, category);
        categoryService.createCategory(category);
        return R.ok();
    }

    @OperationLog("修改分类")
    @Operation(summary = "修改分类", description = "根据 ID 修改分类信息")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "分类 ID", example = "1") @PathVariable Long id,
            @Valid @RequestBody CategoryDTO dto) {
        BlogCategory category = new BlogCategory();
        BeanUtils.copyProperties(dto, category);
        categoryService.updateCategory(id, category);
        return R.ok();
    }

    @OperationLog("删除分类")
    @Operation(summary = "删除分类", description = "根据 ID 逻辑删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(
            @Parameter(description = "分类 ID", example = "1") @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return R.ok();
    }
}
