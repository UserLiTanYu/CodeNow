package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.ArticleVO;
import com.codenow.dto.PublicAuthorRow;
import com.codenow.dto.PublicAuthorVO;
import com.codenow.entity.BlogArticle;
import com.codenow.exception.BusinessException;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.PublicAuthorMapper;
import com.codenow.service.BlogArticleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicAuthorServiceImplTest {

    @InjectMocks
    private PublicAuthorServiceImpl service;

    @Mock
    private PublicAuthorMapper mapper;

    @Mock
    private BlogArticleMapper articleMapper;

    @Mock
    private BlogArticleService articleService;

    @Test
    void pagePublicAuthorsNormalizesKeywordAndSplitsExpertise() {
        PublicAuthorRow row = row(7L);
        Page<PublicAuthorRow> source = new Page<>(1, 12, 1);
        source.setRecords(List.of(row));
        when(mapper.selectPublicAuthorPage(any(Page.class), eq("Java"), eq("popular"))).thenReturn(source);

        Page<PublicAuthorVO> result = service.pagePublicAuthors(1, 12, "  Java  ", "popular");

        assertEquals(1, result.getTotal());
        assertEquals(List.of("Java", "Spring Boot"), result.getRecords().getFirst().getExpertise());
        assertEquals(7L, result.getRecords().getFirst().getUserId());
    }

    @Test
    void pagePublicAuthorsRejectsUnsupportedSort() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.pagePublicAuthors(1, 12, null, "random"));

        assertEquals(400, exception.getCode());
        verifyNoInteractions(mapper);
    }

    @Test
    void pagePublicAuthorsRejectsInvalidPagingAndOverlongKeyword() {
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.pagePublicAuthors(0, 12, null, "popular")).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.pagePublicAuthors(1, 101, null, "popular")).getCode());
        assertEquals(400, assertThrows(BusinessException.class,
                () -> service.pagePublicAuthors(1, 12, "x".repeat(101), "popular")).getCode());
        verifyNoInteractions(mapper);
    }

    @Test
    void getPublicAuthorReturns404WhenAuthorIsNotDiscoverable() {
        when(mapper.selectPublicAuthorByUserId(99L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getPublicAuthor(99L));

        assertEquals(404, exception.getCode());
        assertEquals("作者不存在", exception.getMessage());
    }

    @Test
    void getPublicAuthorReturnsOnlyPublicProjection() {
        when(mapper.selectPublicAuthorByUserId(7L)).thenReturn(row(7L));

        PublicAuthorVO result = service.getPublicAuthor(7L);

        assertEquals("测试作者", result.getDisplayName());
        assertEquals(3L, result.getArticleCount());
        assertEquals(120L, result.getTotalViews());
        assertEquals(List.of("Java", "Spring Boot"), result.getExpertise());
    }

    @Test
    void getPublicAuthorOmitsUnsafeHistoricExternalLinks() {
        PublicAuthorRow row = row(7L);
        row.setWebsiteUrl("javascript:alert(1)");
        row.setPortfolioUrl("data:text/html,unsafe");
        when(mapper.selectPublicAuthorByUserId(7L)).thenReturn(row);

        PublicAuthorVO result = service.getPublicAuthor(7L);

        assertNull(result.getWebsiteUrl());
        assertNull(result.getPortfolioUrl());
    }

    @Test
    void pagePublicAuthorArticlesRequiresDiscoverableAuthorAndBuildsVOs() {
        when(mapper.selectPublicAuthorByUserId(7L)).thenReturn(row(7L));
        BlogArticle article = new BlogArticle();
        article.setId(21L);
        article.setAuthorId(7L);
        article.setStatus(1);
        Page<BlogArticle> source = new Page<>(1, 10, 1);
        source.setRecords(List.of(article));
        when(articleMapper.selectPublishedAuthorArticlePage(any(Page.class), eq(7L), eq("latest")))
                .thenReturn(source);
        ArticleVO articleVO = new ArticleVO();
        articleVO.setArticle(article);
        when(articleService.buildArticleVOBatch(List.of(article))).thenReturn(List.of(articleVO));

        Page<ArticleVO> result = service.pagePublicAuthorArticles(7L, 1, 10, "latest");

        assertEquals(1, result.getTotal());
        assertEquals(21L, result.getRecords().getFirst().getArticle().getId());
    }

    @Test
    void pagePublicAuthorArticlesRejectsHiddenAuthorBeforeArticleQuery() {
        when(mapper.selectPublicAuthorByUserId(8L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.pagePublicAuthorArticles(8L, 1, 10, "latest"));

        assertEquals(404, exception.getCode());
        verifyNoInteractions(articleMapper, articleService);
    }

    @Test
    void pagePublicAuthorArticlesRejectsUnsupportedSort() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.pagePublicAuthorArticles(7L, 1, 10, "learning"));

        assertEquals(400, exception.getCode());
        verifyNoInteractions(articleMapper, articleService);
    }

    private PublicAuthorRow row(Long userId) {
        PublicAuthorRow row = new PublicAuthorRow();
        row.setUserId(userId);
        row.setDisplayName("测试作者");
        row.setAvatar("/images/default-avatar.svg");
        row.setBio("专注 Java 后端与工程实践");
        row.setExpertise("Java,Spring Boot");
        row.setWebsiteUrl("https://example.com");
        row.setPortfolioUrl("https://example.com/works");
        row.setArticleCount(3L);
        row.setTotalViews(120L);
        row.setLastPublishedAt(LocalDateTime.of(2026, 7, 19, 1, 0));
        return row;
    }
}
