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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        BlogCategory root = category(1L, "开发工具", 0L);
        BlogCategory child = category(2L, "Git 教程", 1L);
        when(categoryMapper.selectList(any())).thenReturn(List.of(root, child));

        List<BlogCategory> tree = categoryService.listTree();

        assertEquals(1, tree.size());
        assertEquals("开发工具", tree.getFirst().getName());
        assertEquals("Git 教程", tree.getFirst().getChildren().getFirst().getName());
    }

    @Test
    void selfAndDescendantIdsIncludesNestedCategories() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                category(1L, "Java 基础", 0L),
                category(2L, "集合框架", 1L),
                category(3L, "List", 2L)));

        assertEquals(List.of(1L, 2L, 3L), categoryService.selfAndDescendantIds(1L));
    }

    private BlogCategory category(Long id, String name, Long parentId) {
        BlogCategory category = new BlogCategory();
        category.setId(id);
        category.setName(name);
        category.setParentId(parentId);
        return category;
    }
}
