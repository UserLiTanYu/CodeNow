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

/**
 * 热门文章缓存。Redis ZSet 只保存 Top N 排名，MySQL 始终是事实来源；Redis 故障时降级查询数据库。
 * ZSet 写入、裁剪和续期并非同一原子操作，允许排行榜在并发刷新时短暂不一致。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleServiceImpl implements HotArticleService {

    private static final String HOT_ARTICLES_KEY = "codenow:hot_articles:v2";
    private static final long TTL_SECONDS = 300;
    private static final int HOT_ARTICLE_LIMIT = 3;

    private final StringRedisTemplate stringRedisTemplate;
    private final BlogArticleMapper articleMapper;

    /**
     * 更新热门文章缓存中的浏览量。
     * 使用数据库回读后的浏览量作为 score，避免缓存自行累加造成双写偏差。
     * 更新后裁剪超出限制的记录并续期。
     *
     * @param articleId    文章 ID
     * @param newViewCount 数据库中的最新浏览量
     */
    @Override
    public void incrementViewCount(Long articleId, int newViewCount) {
        try {
            // 使用数据库回读后的浏览量作为 score，避免缓存自行累加造成双写偏差。
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

    /**
     * 获取热门文章 ID 列表。
     * 优先从 Redis 缓存读取，缓存不足或异常时降级查询数据库并回填缓存。
     *
     * @return 热门文章 ID 列表（最多 N 条）
     */
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

    /**
     * 检查 Redis 中是否存在热门文章缓存
     *
     * @return 缓存是否存在
     */
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
