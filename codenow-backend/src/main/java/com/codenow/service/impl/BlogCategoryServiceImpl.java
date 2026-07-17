package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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

@Service
@RequiredArgsConstructor
public class BlogCategoryServiceImpl extends ServiceImpl<BlogCategoryMapper, BlogCategory> implements BlogCategoryService {
    private final BlogArticleMapper articleMapper;

    @Override
    public List<BlogCategory> listTree() {
        List<BlogCategory> categories = list(new LambdaQueryWrapper<BlogCategory>()
                .orderByAsc(BlogCategory::getSort).orderByAsc(BlogCategory::getId));
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

    @Override
    public List<Long> selfAndDescendantIds(Long categoryId) {
        if (categoryId == null) return Collections.emptyList();
        List<BlogCategory> categories = list();
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

    @Override
    @Transactional
    public void createCategory(BlogCategory category) {
        category.setParentId(normalizeParentId(category.getParentId()));
        validateParent(null, category.getParentId());
        save(category);
    }

    @Override
    @Transactional
    public void updateCategory(Long id, BlogCategory category) {
        if (getById(id) == null) throw new BusinessException(404, "分类不存在");
        category.setParentId(normalizeParentId(category.getParentId()));
        validateParent(id, category.getParentId());
        category.setId(id);
        updateById(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        if (getById(id) == null) throw new BusinessException(404, "分类不存在");
        long childCount = count(new LambdaQueryWrapper<BlogCategory>().eq(BlogCategory::getParentId, id));
        if (childCount > 0) throw new BusinessException(400, "请先删除该分类下的子分类");
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<BlogArticle>().eq(BlogArticle::getCategoryId, id));
        if (articleCount != null && articleCount > 0) {
            throw new BusinessException(400, "请先移动或删除该分类下的文章");
        }
        removeById(id);
    }

    private void validateParent(Long id, Long parentId) {
        if (parentId == 0L) return;
        if (Objects.equals(id, parentId)) throw new BusinessException(400, "分类不能选择自身作为父分类");
        if (getById(parentId) == null) throw new BusinessException(400, "父分类不存在");
        if (id != null && selfAndDescendantIds(id).contains(parentId)) {
            throw new BusinessException(400, "不能将分类移动到自己的子分类下");
        }
    }

    private long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }
}
