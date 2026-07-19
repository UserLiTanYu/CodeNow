package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.BlogCategory;

import java.util.List;

public interface BlogCategoryService extends IService<BlogCategory> {
    List<BlogCategory> listTree();

    List<BlogCategory> listTreeByAuthor(Long authorId);

    List<Long> selfAndDescendantIds(Long categoryId);

    List<Long> selfAndDescendantIdsByAuthor(Long categoryId, Long authorId);

    void createCategory(BlogCategory category);

    void updateCategory(Long id, BlogCategory category);

    void updateAuthorCategory(Long id, Long authorId, BlogCategory category);

    void deleteCategory(Long id);

    void deleteAuthorCategory(Long id, Long authorId);

    /**
     * 仅返回至少有一篇已发布文章的分类（含祖先节点），树形结构
     */
    List<BlogCategory> listTreeByPublishedArticles();
}
