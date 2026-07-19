package com.codenow.service.impl;

import com.codenow.entity.BlogCategory;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogCategoryMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogCategoryServiceImplTest {
    @InjectMocks
    private BlogCategoryServiceImpl categoryService;

    @Mock
    private BlogCategoryMapper categoryMapper;

    @Mock
    private BlogArticleMapper articleMapper;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(categoryService, "baseMapper", categoryMapper);
    }

    @Test
    void listTreeBuildsOrderedParentChildStructure() {
        BlogCategory root = category(1L, "开发工具", 0L, 10L);
        BlogCategory child = category(2L, "Git 教程", 1L, 10L);
        when(categoryMapper.selectList(any())).thenReturn(List.of(root, child));

        List<BlogCategory> tree = categoryService.listTree();

        assertEquals(1, tree.size());
        assertEquals("开发工具", tree.getFirst().getName());
        assertEquals("Git 教程", tree.getFirst().getChildren().getFirst().getName());
    }

    @Test
    void selfAndDescendantIdsIncludesNestedCategories() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "Java 基础", 0L, 10L),
                category(2L, "集合框架", 1L, 10L),
                category(3L, "List", 2L, 10L)));

        assertEquals(List.of(1L, 2L, 3L), categoryService.selfAndDescendantIds(1L));
    }

    @Test
    void listTreeByPublishedArticlesReturnsOnlyCategoriesWithArticlesAndAncestors() {
        // 已发布文章关联了分类 3（List），其祖先链为 1 -> 2 -> 3
        when(articleMapper.selectPublishedCategoryIds()).thenReturn(List.of(3L));
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "Java 基础", 0L, 10L),
                category(2L, "集合框架", 1L, 10L),
                category(3L, "List", 2L, 10L),
                category(4L, "无关分类", 0L, 10L)));

        List<BlogCategory> tree = categoryService.listTreeByPublishedArticles();

        // 仅包含 1 -> 2 -> 3 的树，不包含 4
        assertEquals(1, tree.size());
        assertEquals("Java 基础", tree.getFirst().getName());
        assertEquals(1, tree.getFirst().getChildren().size());
        assertEquals("集合框架", tree.getFirst().getChildren().getFirst().getName());
        assertEquals(1, tree.getFirst().getChildren().getFirst().getChildren().size());
        assertEquals("List", tree.getFirst().getChildren().getFirst().getChildren().getFirst().getName());
    }

    @Test
    void listTreeByPublishedArticlesReturnsEmptyWhenNoPublishedArticles() {
        when(articleMapper.selectPublishedCategoryIds()).thenReturn(Collections.emptyList());

        List<BlogCategory> tree = categoryService.listTreeByPublishedArticles();

        assertTrue(tree.isEmpty());
    }

    @Test
    void listTreeByAuthorBuildsFilteredTree() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "我的分类", 0L, 10L),
                category(2L, "子分类", 1L, 10L)));

        List<BlogCategory> tree = categoryService.listTreeByAuthor(10L);

        assertEquals(1, tree.size());
        assertEquals("我的分类", tree.getFirst().getName());
        assertEquals(1, tree.getFirst().getChildren().size());
    }

    private BlogCategory category(Long id, String name, Long parentId, Long authorId) {
        BlogCategory category = new BlogCategory();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        category.setAuthorId(authorId);
        return category;
    }
}
