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
public class AuthorCategoryController {

    private final BlogCategoryService categoryService;

    @Operation(summary = "查询我的分类列表")
    @GetMapping
    public R<List<BlogCategory>> list() {
        long userId = StpUtil.getLoginIdAsLong();
        return R.ok(categoryService.listTreeByAuthor(userId));
    }

    @OperationLog("作者新增分类")
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

    @OperationLog("作者修改分类")
    @Operation(summary = "修改分类")
    @PutMapping("/{id}")
    public R<Void> update(
            @Parameter(description = "分类 ID") @PathVariable Long id,
            @Valid @RequestBody CategoryDTO dto) {
        long userId = StpUtil.getLoginIdAsLong();
        BlogCategory category = new BlogCategory();
        BeanUtils.copyProperties(dto, category);
        categoryService.updateAuthorCategory(id, userId, category);
        return R.ok();
    }

    @OperationLog("作者删除分类")
    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@Parameter(description = "分类 ID") @PathVariable Long id) {
        long userId = StpUtil.getLoginIdAsLong();
        categoryService.deleteAuthorCategory(id, userId);
        return R.ok();
    }
}
