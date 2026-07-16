package com.codenow.service.impl;

import com.codenow.entity.BlogArticle;
import com.codenow.mapper.BlogArticleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotArticleServiceImplTest {

    private static final String CACHE_KEY = "codenow:hot_articles:v2";

    @InjectMocks
    private HotArticleServiceImpl hotArticleService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private BlogArticleMapper articleMapper;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    void incrementViewCount_shouldKeepOnlyTheHighestThreeScores() {
        when(zSetOperations.size(CACHE_KEY)).thenReturn(4L);

        hotArticleService.incrementViewCount(4L, 40);

        verify(zSetOperations).add(CACHE_KEY, "4", 40);
        verify(zSetOperations).removeRange(CACHE_KEY, 0, 0);
        verify(stringRedisTemplate).expire(CACHE_KEY, 300, TimeUnit.SECONDS);
    }

    @Test
    void getHotArticleIds_shouldReturnOnlyThreeCachedArticles() {
        Set<String> cachedIds = new LinkedHashSet<>(List.of("3", "2", "1"));
        when(zSetOperations.reverseRange(CACHE_KEY, 0, 2)).thenReturn(cachedIds);

        assertEquals(List.of(3L, 2L, 1L), hotArticleService.getHotArticleIds());

        verifyNoInteractions(articleMapper);
    }

    @Test
    void getHotArticleIds_shouldRebuildFromDatabaseWhenCacheIsEmpty() {
        when(zSetOperations.reverseRange(CACHE_KEY, 0, 2)).thenReturn(Set.of());
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(1L, 30), article(2L, 20), article(3L, 10)));

        assertEquals(List.of(1L, 2L, 3L), hotArticleService.getHotArticleIds());

        verify(stringRedisTemplate).delete(CACHE_KEY);
        verify(zSetOperations).add(CACHE_KEY, "1", 30);
        verify(zSetOperations).add(CACHE_KEY, "2", 20);
        verify(zSetOperations).add(CACHE_KEY, "3", 10);
        verify(stringRedisTemplate).expire(CACHE_KEY, 300, TimeUnit.SECONDS);
    }

    @Test
    void getHotArticleIds_shouldRebuildWhenCacheContainsFewerThanThreeArticles() {
        when(zSetOperations.reverseRange(CACHE_KEY, 0, 2)).thenReturn(Set.of("1"));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(1L, 30), article(2L, 20), article(3L, 10)));

        assertEquals(List.of(1L, 2L, 3L), hotArticleService.getHotArticleIds());

        verify(articleMapper).selectList(any());
    }

    @Test
    void getHotArticleIds_shouldUseDatabaseWhenRedisIsUnavailable() {
        when(zSetOperations.reverseRange(CACHE_KEY, 0, 2)).thenThrow(new IllegalStateException("redis unavailable"));
        when(articleMapper.selectList(any())).thenReturn(List.of(
                article(1L, 30), article(2L, 20), article(3L, 10)));

        assertEquals(List.of(1L, 2L, 3L), hotArticleService.getHotArticleIds());
    }

    private BlogArticle article(Long id, int viewCount) {
        BlogArticle article = new BlogArticle();
        article.setId(id);
        article.setViewCount(viewCount);
        return article;
    }
}
