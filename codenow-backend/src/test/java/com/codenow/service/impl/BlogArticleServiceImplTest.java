package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.ArticleVO;
import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogArticleTag;
import com.codenow.entity.BlogTag;
import com.codenow.exception.BusinessException;
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
        when(tagMapper.selectBatchIds(any())).thenReturn(List.of(tag(1L), tag(2L)));

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
    void deleteArticleWithTags_shouldPreserveRelationsAndDeleteArticle() {
        Long articleId = 1L;

        articleService.deleteArticleWithTags(articleId);

        verify(articleTagMapper, never()).delete(any());
        verify(articleMapper).deleteById(articleId);
    }

    @Test
    void saveArticleWithTags_withUnknownTag_shouldRollbackWithBusinessError() {
        BlogArticle article = new BlogArticle();
        when(articleMapper.insert(any(BlogArticle.class))).thenAnswer(invocation -> {
            BlogArticle saved = invocation.getArgument(0);
            saved.setId(1L);
            return 1;
        });
        when(tagMapper.selectBatchIds(any())).thenReturn(List.of(tag(1L)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> articleService.saveArticleWithTags(article, List.of(1L, 999L)));

        assertEquals(400, exception.getCode());
        verify(articleTagMapper, never()).insert(any(BlogArticleTag.class));
    }

    @Test
    void toggleStatus_shouldUseAtomicMapperUpdate() {
        when(articleMapper.toggleStatus(1L)).thenReturn(1);

        assertTrue(articleService.toggleStatus(1L));
        verify(articleMapper).toggleStatus(1L);
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

        when(articleMapper.selectPublishedArticlePage(any(Page.class), isNull(), isNull(), isNull(), eq("latest")))
                .thenReturn(mockPage);

        Page<ArticleVO> result = articleService.pagePublishedArticles(1, 10, null, null, null, "latest");

        assertNotNull(result);
        assertEquals(0, result.getTotal());
    }

    @Test
    void pagePublishedArticles_shouldTrimSearchKeyword() {
        Page<BlogArticle> mockPage = new Page<>(1, 10);
        mockPage.setTotal(0);
        mockPage.setRecords(Collections.emptyList());
        when(articleMapper.selectPublishedArticlePage(any(Page.class), isNull(), isNull(), eq("Spring Boot"), eq("latest")))
                .thenReturn(mockPage);

        articleService.pagePublishedArticles(1, 10, null, null, "  Spring Boot  ", null);

        verify(articleMapper).selectPublishedArticlePage(any(Page.class), isNull(), isNull(), eq("Spring Boot"), eq("latest"));
    }

    @Test
    void pagePublishedArticles_shouldRejectOverlongKeyword() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> articleService.pagePublishedArticles(1, 10, null, null, "x".repeat(101), "latest"));

        assertEquals(400, exception.getCode());
        verify(articleMapper, never()).selectPublishedArticlePage(any(), any(), any(), any(), any());
    }

    @Test
    void pagePublishedArticles_shouldPassMostViewedSort() {
        Page<BlogArticle> mockPage = new Page<>(1, 10);
        mockPage.setRecords(Collections.emptyList());
        when(articleMapper.selectPublishedArticlePage(any(Page.class), isNull(), isNull(), isNull(), eq("mostViewed")))
                .thenReturn(mockPage);

        articleService.pagePublishedArticles(1, 10, null, null, null, "mostViewed");

        verify(articleMapper).selectPublishedArticlePage(any(Page.class), isNull(), isNull(), isNull(), eq("mostViewed"));
    }

    @Test
    void pagePublishedArticles_shouldRejectUnknownSort() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> articleService.pagePublishedArticles(1, 10, null, null, null, "random"));

        assertEquals(400, exception.getCode());
        verify(articleMapper, never()).selectPublishedArticlePage(any(), any(), any(), any(), any());
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

        BlogArticle updatedArticle = new BlogArticle();
        updatedArticle.setId(1L);
        updatedArticle.setStatus(1);
        updatedArticle.setViewCount(11);
        updatedArticle.setCategoryId(1L);

        // 第一次 selectById 返回原始值，第二次（重新读取）返回更新后的值
        when(articleMapper.selectById(1L)).thenReturn(article).thenReturn(updatedArticle);
        when(articleMapper.update(any(), any())).thenReturn(1);
        when(articleTagMapper.selectList(any())).thenReturn(Collections.emptyList());

        ArticleVO result = articleService.getPublishedArticleById(1L);

        assertNotNull(result);
        verify(hotArticleService).incrementViewCount(1L, 11);
    }

    private BlogTag tag(Long id) {
        BlogTag tag = new BlogTag();
        tag.setId(id);
        tag.setName("tag-" + id);
        return tag;
    }
}
