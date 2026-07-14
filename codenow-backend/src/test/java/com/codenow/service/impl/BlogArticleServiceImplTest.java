package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.ArticleVO;
import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogArticleTag;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogArticleTagMapper;
import com.codenow.mapper.BlogCategoryMapper;
import com.codenow.mapper.BlogTagMapper;
import com.codenow.service.HotArticleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BlogArticleServiceImplTest {

    @InjectMocks
    private BlogArticleServiceImpl articleService;

    @Mock
    private BlogArticleMapper articleMapper;

    @Mock
    private BlogArticleTagMapper articleTagMapper;

    @Mock
    private BlogCategoryMapper categoryMapper;

    @Mock
    private BlogTagMapper tagMapper;

    @Mock
    private HotArticleService hotArticleService;

    @BeforeEach
    void setUp() {
        // MyBatis-Plus ServiceImpl 需要 baseMapper 通过 @Autowired 注入
        ReflectionTestUtils.setField(articleService, "baseMapper", articleMapper);
    }

    @Test
    void saveArticleWithTags_shouldSaveArticleAndRelations() {
        BlogArticle article = new BlogArticle();
        article.setTitle("Test Article");
        article.setContent("Test Content");
        List<Long> tagIds = Arrays.asList(1L, 2L);

        when(articleMapper.insert(any(BlogArticle.class))).thenAnswer(invocation -> {
            BlogArticle a = invocation.getArgument(0);
            a.setId(1L);
            return 1;
        });

        articleService.saveArticleWithTags(article, tagIds);

        verify(articleMapper).insert(any(BlogArticle.class));
        verify(articleTagMapper, times(2)).insert(any(BlogArticleTag.class));
    }

    @Test
    void saveArticleWithTags_withNullTagIds_shouldOnlySaveArticle() {
        BlogArticle article = new BlogArticle();
        article.setTitle("Test Article");

        when(articleMapper.insert(any(BlogArticle.class))).thenAnswer(invocation -> {
            BlogArticle a = invocation.getArgument(0);
            a.setId(1L);
            return 1;
        });

        articleService.saveArticleWithTags(article, null);

        verify(articleMapper).insert(any(BlogArticle.class));
        verify(articleTagMapper, never()).insert(any(BlogArticleTag.class));
    }

    @Test
    void deleteArticleWithTags_shouldDeleteRelationsAndArticle() {
        Long articleId = 1L;

        articleService.deleteArticleWithTags(articleId);

        verify(articleTagMapper).delete(any());
        verify(articleMapper).deleteById(articleId);
    }

    @Test
    void getArticleVOById_whenArticleNotFound_shouldReturnNull() {
        when(articleMapper.selectById(999L)).thenReturn(null);

        ArticleVO result = articleService.getArticleVOById(999L);

        assertNull(result);
    }

    @Test
    void getArticleVOById_whenArticleExists_shouldReturnVO() {
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setTitle("Test");
        article.setCategoryId(1L);

        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleTagMapper.selectList(any())).thenReturn(Collections.emptyList());

        ArticleVO result = articleService.getArticleVOById(1L);

        assertNotNull(result);
        assertEquals("Test", result.getArticle().getTitle());
        assertNotNull(result.getTags());
        assertTrue(result.getTags().isEmpty());
    }

    @Test
    void pagePublishedArticles_shouldReturnPaginatedResults() {
        Page<BlogArticle> mockPage = new Page<>(1, 10);
        mockPage.setTotal(0);
        mockPage.setRecords(Collections.emptyList());

        when(articleMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(mockPage);

        Page<ArticleVO> result = articleService.pagePublishedArticles(1, 10, null, null);

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    void getPublishedArticleById_whenDraft_shouldReturnNull() {
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setStatus(0); // draft

        when(articleMapper.selectById(1L)).thenReturn(article);

        ArticleVO result = articleService.getPublishedArticleById(1L);

        assertNull(result);
    }

    @Test
    void getPublishedArticleById_whenPublished_shouldIncrementViewCount() {
        BlogArticle article = new BlogArticle();
        article.setId(1L);
        article.setStatus(1);
        article.setViewCount(10);
        article.setCategoryId(1L);

        when(articleMapper.selectById(1L)).thenReturn(article);
        when(articleMapper.update(any(), any())).thenReturn(1);
        when(articleTagMapper.selectList(any())).thenReturn(Collections.emptyList());

        ArticleVO result = articleService.getPublishedArticleById(1L);

        assertNotNull(result);
        verify(hotArticleService).incrementViewCount(1L, 11);
    }
}
