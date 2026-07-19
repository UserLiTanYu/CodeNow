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

@Service
@RequiredArgsConstructor
public class BlogCategoryServiceImpl extends ServiceImpl<BlogCategoryMapper, BlogCategory> implements BlogCategoryService {
    private final BlogArticleMapper articleMapper;

    @Override
    public List<BlogCategory> listTree() {
        List<BlogCategory> categories = list(new LambdaQueryWrapper<BlogCategory>()
                .orderByAsc(BlogCategory::getSort).orderByAsc(BlogCategory::getId));
        return buildTree(categories);
    }

    @Override
    public List<BlogCategory> listTreeByAuthor(Long authorId) {
        List<BlogCategory> categories = list(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getAuthorId, authorId)
                .orderByAsc(BlogCategory::getSort).orderByAsc(BlogCategory::getId));
        return buildTree(categories);
    }

    @Override
    public List<Long> selfAndDescendantIds(Long categoryId) {
        if (categoryId == null) return Collections.emptyList();
        List<BlogCategory> categories = list();
        return collectDescendantIds(categoryId, categories);
    }

    @Override
    public List<Long> selfAndDescendantIdsByAuthor(Long categoryId, Long authorId) {
        if (categoryId == null) return Collections.emptyList();
        List<BlogCategory> categories = list(new LambdaQueryWrapper<BlogCategory>()
                .eq(BlogCategory::getAuthorId, authorId));
        return collectDescendantIds(categoryId, categories);
    }

    @Override
    @Transactional
    public void createCategory(BlogCategory category) {
        category.setParentId(normalizeParentId(category.getParentId()));
        validateParent(null, category.getParentId(), category.getAuthorId());
        validateNameUnique(null, category.getName(), category.getAuthorId());
        save(category);
    }

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

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (getById(id) == null) throw new BusinessException(404, "分类不存在");
        throwIfHasChildren(id);
        throwIfHasArticles(id);
        removeById(id);
    }

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
            BlogCategory parent = byId.get(normalizeParentId(category.getParentId()));
            if (parent == null) roots.add(category);
            else parent.getChildren().add(category);
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

    @Override
    public List<BlogCategory> listTreeByPublishedArticles() {
        // 获取所有已发布文章关联的分类 ID（仅叶子节点级别的直接关联）
        List<Long> publishedCategoryIds = articleMapper.selectPublishedCategoryIds();
        if (publishedCategoryIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 获取全部分类（用于构建完整树结构和查找祖先）
        List<BlogCategory> allCategories = list(new LambdaQueryWrapper<BlogCategory>()
                .orderByAsc(BlogCategory::getSort).orderByAsc(BlogCategory::getId));
        // 收集所有需要展示的分类 ID：已发布文章的分类 + 所有祖先
        Set<Long> requiredIds = new HashSet<>();
        Map<Long, BlogCategory> byId = new HashMap<>();
        allCategories.forEach(c -> byId.put(c.getId(), c));
        for (Long cid : publishedCategoryIds) {
            Long current = cid;
            while (current != null && requiredIds.add(current)) {
                BlogCategory cat = byId.get(current);
                current = (cat == null || cat.getParentId() == null || cat.getParentId() == 0L) ? null : cat.getParentId();
            }
        }
        // 仅保留需要展示的分类
        List<BlogCategory> filtered = allCategories.stream()
                .filter(c -> requiredIds.contains(c.getId()))
                .collect(Collectors.toList());
        return buildTree(filtered);
    }

    private long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }
}
