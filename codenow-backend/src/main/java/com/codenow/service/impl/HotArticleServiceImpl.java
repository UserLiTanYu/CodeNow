package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codenow.common.ArticleStatus;
import com.codenow.entity.BlogArticle;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.service.HotArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleServiceImpl implements HotArticleService {

    private static final String HOT_ARTICLES_KEY = "codenow:hot_articles:v2";
    private static final long TTL_SECONDS = 300;
    private static final int HOT_ARTICLE_LIMIT = 3;

    private final StringRedisTemplate stringRedisTemplate;
    private final BlogArticleMapper articleMapper;

    @Override
    public void incrementViewCount(Long articleId, int newViewCount) {
        try {
            stringRedisTemplate.opsForZSet().add(
                    HOT_ARTICLES_KEY,
                    String.valueOf(articleId),
                    newViewCount);
            Long cacheSize = stringRedisTemplate.opsForZSet().size(HOT_ARTICLES_KEY);
            if (cacheSize != null && cacheSize > HOT_ARTICLE_LIMIT) {
                stringRedisTemplate.opsForZSet().removeRange(
                        HOT_ARTICLES_KEY,
                        0,
                        cacheSize - HOT_ARTICLE_LIMIT - 1);
            }
            stringRedisTemplate.expire(HOT_ARTICLES_KEY, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("更新 Redis 热门文章缓存失败: {}", e.getMessage());
        }
    }

    @Override
    public List<Long> getHotArticleIds() {
        try {
            Set<String> ids = stringRedisTemplate.opsForZSet()
                    .reverseRange(HOT_ARTICLES_KEY, 0, HOT_ARTICLE_LIMIT - 1);
            if (ids != null && ids.size() >= HOT_ARTICLE_LIMIT) {
                return ids.stream().map(Long::valueOf).toList();
            }
        } catch (Exception e) {
            log.warn("读取 Redis 热门文章失败，将从数据库查询: {}", e.getMessage());
        }
        return loadFromDatabaseAndRefreshCache();
    }

    private List<Long> loadFromDatabaseAndRefreshCache() {
        List<BlogArticle> articles = articleMapper.selectList(
                new LambdaQueryWrapper<BlogArticle>()
                        .eq(BlogArticle::getStatus, ArticleStatus.PUBLISHED)
                        .orderByDesc(BlogArticle::getViewCount)
                        .orderByDesc(BlogArticle::getCreateTime)
                        .orderByDesc(BlogArticle::getId)
                        .last("LIMIT " + HOT_ARTICLE_LIMIT));
        if (articles.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            stringRedisTemplate.delete(HOT_ARTICLES_KEY);
            articles.forEach(article -> stringRedisTemplate.opsForZSet().add(
                    HOT_ARTICLES_KEY,
                    String.valueOf(article.getId()),
                    article.getViewCount() == null ? 0 : article.getViewCount()));
            stringRedisTemplate.expire(HOT_ARTICLES_KEY, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("回填 Redis 热门文章缓存失败，继续返回数据库结果: {}", e.getMessage());
        }
        return articles.stream().map(BlogArticle::getId).toList();
    }

    @Override
    public boolean hasCache() {
        try {
            Long size = stringRedisTemplate.opsForZSet().zCard(HOT_ARTICLES_KEY);
            return size != null && size > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
