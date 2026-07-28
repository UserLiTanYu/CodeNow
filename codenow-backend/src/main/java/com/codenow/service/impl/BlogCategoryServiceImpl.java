package com.codenow.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.common.UserRole;
import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogCategory;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogCategoryMapper;
import com.codenow.service.BlogCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 分类树服务。负责平铺记录与树结构互转、后代遍历，以及新增、移动、删除时的环路和引用校验。
 */
@Service
@RequiredArgsConstructor
public class BlogCategoryServiceImpl extends ServiceImpl<BlogCategoryMapper, BlogCategory> implements BlogCategoryService {
    private final BlogArticleMapper articleMapper;

    /**
     * 查询全部分类树结构
     *
     * @return 分类树列表（顶级分类及其子分类）
     */
    @Override
    public List<BlogCategory> listTree() {
        List<BlogCategory> categories = list(new LambdaQueryWrapper<BlogCategory>()
                .orderByAsc(BlogCategory::getSort).orderByAsc(BlogCategory::getId));
        return buildTree(categories);
    }

    /**
     * 根据作者 ID 查询分类树结构
     *
     * @param authorId 作者用户 ID
     * @return 该作者的分类树列表
     */
    @Override
    public List<BlogCategory> listTreeByAuthor(Long authorId) {
        List<BlogCategory> categories = list(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getAuthorId, authorId)
                .orderByAsc(BlogCategory::getSort).orderByAsc(BlogCategory::getId));
        return buildTree(categories);
    }

    /**
     * 获取指定分类及其所有后代分类的 ID 列表
     *
     * @param categoryId 分类 ID
     * @return 包含自身和所有后代的分类 ID 列表
     */
    @Override
    public List<Long> selfAndDescendantIds(Long categoryId) {
        if (categoryId == null) return Collections.emptyList();
        List<BlogCategory> categories = list();
        return collectDescendantIds(categoryId, categories);
    }

    /**
     * 获取指定作者的分类及其所有后代分类的 ID 列表
     *
     * @param categoryId 分类 ID
     * @param authorId   作者用户 ID
     * @return 包含自身和所有后代的分类 ID 列表
     */
    @Override
    public List<Long> selfAndDescendantIdsByAuthor(Long categoryId, Long authorId) {
        if (categoryId == null) return Collections.emptyList();
        List<BlogCategory> categories = list(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getAuthorId, authorId));
        return collectDescendantIds(categoryId, categories);
    }

    /**
     * 创建分类。
     * 校验父分类合法性（防止环路）和同名唯一性后保存。
     *
     * @param category 分类实体
     */
    @Override
    @Transactional
    public void createCategory(BlogCategory category) {
        category.setParentId(normalizeParentId(category.getParentId()));
        validateParent(null, category.getParentId(), category.getAuthorId());
        validateNameUnique(null, category.getName(), category.getAuthorId());
        save(category);
    }

    /**
     * 更新分类（管理员操作）。
     * 校验父分类合法性和同名唯一性后更新。
     *
     * @param id       分类 ID
     * @param category 更新后的分类实体
     */
    @Override
    @Transactional
    public void updateCategory(Long id, BlogCategory category) {
        BlogCategory existing = getById(id);
        if (existing == null) throw new BusinessException(404, "分类不存在");
        category.setParentId(normalizeParentId(category.getParentId()));
        validateParent(id, category.getParentId(), existing.getAuthorId());
        validateNameUnique(id, category.getName(), existing.getAuthorId());
        category.setId(id);
        category.setAuthorId(existing.getAuthorId());
        updateById(category);
    }

    /**
     * 作者更新自己的分类。
     * 校验分类归属权、父分类合法性和同名唯一性后更新。
     *
     * @param id       分类 ID
     * @param authorId 作者用户 ID
     * @param category 更新后的分类实体
     */
    @Override
    @Transactional
    public void updateAuthorCategory(Long id, Long authorId, BlogCategory category) {
        BlogCategory existing = getById(id);
        if (existing == null) throw new BusinessException(404, "分类不存在");
        if (!Objects.equals(existing.getAuthorId(), authorId)) {
            throw new BusinessException(403, "只能修改自己创建的分类");
        }
        category.setParentId(normalizeParentId(category.getParentId()));
        validateParent(id, category.getParentId(), authorId);
        validateNameUnique(id, category.getName(), authorId);
        category.setId(id);
        category.setAuthorId(authorId);
        updateById(category);
    }

    /**
     * 删除分类（管理员操作）。
     * 有子分类或关联文章时不允许删除。
     *
     * @param id 分类 ID
     */
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (getById(id) == null) throw new BusinessException(404, "分类不存在");
        throwIfHasChildren(id);
        throwIfHasArticles(id);
        removeById(id);
    }

    /**
     * 作者删除自己的分类。
     * 校验分类归属权，有子分类或关联文章时不允许删除。
     *
     * @param id       分类 ID
     * @param authorId 作者用户 ID
     */
    @Override
    @Transactional
    public void deleteAuthorCategory(Long id, Long authorId) {
        BlogCategory existing = getById(id);
        if (existing == null) throw new BusinessException(404, "分类不存在");
        if (!Objects.equals(existing.getAuthorId(), authorId)) {
            throw new BusinessException(403, "只能删除自己创建的分类");
        }
        throwIfHasChildren(id);
        throwIfHasArticles(id);
        removeById(id);
    }

    private List<BlogCategory> buildTree(List<BlogCategory> categories) {
        Map<Long, BlogCategory> byId = new LinkedHashMap<>();
        categories.forEach(category -> {
            category.setChildren(new ArrayList<>());
            byId.put(category.getId(), category);
        });
        List<BlogCategory> roots = new ArrayList<>();
        for (BlogCategory category : categories) {
            Long parentId = normalizeParentId(category.getParentId());
            BlogCategory parent = byId.get(parentId);
            // Only attach to parent if same author (prevents cross-author mixing)
            if (parent != null && Objects.equals(parent.getAuthorId(), category.getAuthorId())) {
                parent.getChildren().add(category);
            } else {
                roots.add(category);
            }
        }
        return roots;
    }

    private List<Long> collectDescendantIds(Long categoryId, List<BlogCategory> categories) {
        if (categories.stream().noneMatch(item -> Objects.equals(item.getId(), categoryId))) {
            return List.of(categoryId);
        }
        Map<Long, List<Long>> children = new HashMap<>();
        for (BlogCategory category : categories) {
            children.computeIfAbsent(normalizeParentId(category.getParentId()), ignored -> new ArrayList<>())
                    .add(category.getId());
        }
        List<Long> result = new ArrayList<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(categoryId);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            if (result.contains(current)) continue;
            result.add(current);
            queue.addAll(children.getOrDefault(current, Collections.emptyList()));
        }
        return result;
    }

    private void validateParent(Long id, Long parentId, Long authorId) {
        if (parentId == 0L) return;
        if (Objects.equals(id, parentId)) throw new BusinessException(400, "分类不能选择自身作为父分类");
        BlogCategory parent = getById(parentId);
        if (parent == null) throw new BusinessException(400, "父分类不存在");
        if (authorId != null && !Objects.equals(parent.getAuthorId(), authorId)) {
            throw new BusinessException(400, "父分类必须属于同一作者");
        }
        // 父节点不能来自当前节点的后代，否则移动后会形成分类环。
        if (id != null && selfAndDescendantIdsByAuthor(id, authorId).contains(parentId)) {
            throw new BusinessException(400, "不能将分类移动到自己的子分类下");
        }
    }

    private void validateNameUnique(Long id, String name, Long authorId) {
        if (name == null || name.isBlank()) return;
        Long count = count(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getName, name.trim())
                .eq(BlogCategory::getAuthorId, authorId)
                .ne(id != null, BlogCategory::getId, id));
        if (count != null && count > 0) {
            throw new BusinessException(409, "同名分类已存在");
        }
    }

    private void throwIfHasChildren(Long id) {
        long childCount = count(new LambdaQueryWrapper<BlogCategory>().eq(BlogCategory::getParentId, id));
        if (childCount > 0) throw new BusinessException(400, "请先删除该分类下的子分类");
    }

    private void throwIfHasArticles(Long id) {
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<BlogArticle>().eq(BlogArticle::getCategoryId, id));
        if (articleCount != null && articleCount > 0) {
            throw new BusinessException(400, "请先移动或删除该分类下的文章");
        }
    }

    /**
     * 查询博客首页侧边栏分类树。
     * 只显示主要作者的、包含已发布文章的分类及其祖先分类。
     *
     * @return 分类树列表
     */
    @Override
    public List<BlogCategory> listTreeByPublishedArticles() {
        // Blog home sidebar: show primary author's categories that have published articles
        // Each author's own categories are shown on their individual profile page
        List<BlogCategory> allCategories = list(new LambdaQueryWrapper<BlogCategory>()
                .orderByAsc(BlogCategory::getSort).orderByAsc(BlogCategory::getId));
        Long primaryAuthorId = allCategories.stream()
                .map(BlogCategory::getAuthorId)
                .filter(Objects::nonNull)
                .min(Long::compareTo)
                .orElse(null);
        if (primaryAuthorId == null) return Collections.emptyList();

        List<Long> publishedCategoryIds = articleMapper.selectPublishedCategoryIds();
        if (publishedCategoryIds.isEmpty()) return Collections.emptyList();

        Map<Long, BlogCategory> byId = new HashMap<>();
        allCategories.forEach(c -> byId.put(c.getId(), c));

        // Collect primary author's categories that have published articles + ancestors
        Set<Long> requiredIds = new HashSet<>();
        for (Long cid : publishedCategoryIds) {
            BlogCategory cat = byId.get(cid);
            if (cat == null || !Objects.equals(cat.getAuthorId(), primaryAuthorId)) continue;
            Long current = cid;
            while (current != null && requiredIds.add(current)) {
                BlogCategory c = byId.get(current);
                current = (c == null || c.getParentId() == null || c.getParentId() == 0L) ? null : c.getParentId();
            }
        }
        List<BlogCategory> filtered = allCategories.stream()
                .filter(c -> Objects.equals(c.getAuthorId(), primaryAuthorId) && requiredIds.contains(c.getId()))
                .collect(Collectors.toList());
        return buildTree(filtered);
    }

    private long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }
}
