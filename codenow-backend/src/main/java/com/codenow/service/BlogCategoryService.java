package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.BlogCategory;

import java.util.List;

public interface BlogCategoryService extends IService<BlogCategory> {
    List<BlogCategory> listTree();

    List<Long> selfAndDescendantIds(Long categoryId);

    void createCategory(BlogCategory category);

    void updateCategory(Long id, BlogCategory category);

    void deleteCategory(Long id);
}
