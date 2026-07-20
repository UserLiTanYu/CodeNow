package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.BlogCategory;

import java.util.List;

/**
 * 博客分类服务接口
 */
public interface BlogCategoryService extends IService<BlogCategory> {

    /**
     * 查询所有分类的树形结构
     *
     * @return 分类树形列表
     */
    List<BlogCategory> listTree();

    /**
     * 查询指定作者的分类树形结构
     *
     * @param authorId 作者ID
     * @return 分类树形列表
     */
    List<BlogCategory> listTreeByAuthor(Long authorId);

    /**
     * 获取当前分类及其所有后代分类的ID列表
     *
     * @param categoryId 分类ID
     * @return 分类ID列表（含自身及后代）
     */
    List<Long> selfAndDescendantIds(Long categoryId);

    /**
     * 获取指定作者的当前分类及其所有后代分类的ID列表
     *
     * @param categoryId 分类ID
     * @param authorId   作者ID
     * @return 分类ID列表（含自身及后代）
     */
    List<Long> selfAndDescendantIdsByAuthor(Long categoryId, Long authorId);

    /**
     * 创建分类
     *
     * @param category 分类实体
     */
    void createCategory(BlogCategory category);

    /**
     * 更新分类
     *
     * @param id       分类ID
     * @param category 分类实体
     */
    void updateCategory(Long id, BlogCategory category);

    /**
     * 更新作者分类（作者只能操作自己的分类）
     *
     * @param id       分类ID
     * @param authorId 作者ID
     * @param category 分类实体
     */
    void updateAuthorCategory(Long id, Long authorId, BlogCategory category);

    /**
     * 删除分类
     *
     * @param id 分类ID
     */
    void deleteCategory(Long id);

    /**
     * 删除作者分类（作者只能删除自己的分类）
     *
     * @param id       分类ID
     * @param authorId 作者ID
     */
    void deleteAuthorCategory(Long id, Long authorId);

    /**
     * 仅返回至少有一篇已发布文章的分类（含祖先节点），树形结构
     *
     * @return 分类树形列表
     */
    List<BlogCategory> listTreeByPublishedArticles();
}
