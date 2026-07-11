package com.codenow.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotArticleService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final String HOT_ARTICLES_KEY = "codenow:hot_articles";
    private static final long TTL_SECONDS = 300; // 5 分钟

    /**
     * 更新文章的浏览量分数（文章被访问时调用）
     */
    public void incrementViewCount(Long articleId, int newViewCount) {
        try {
            stringRedisTemplate.opsForZSet().add(
                    HOT_ARTICLES_KEY,
                    String.valueOf(articleId),
                    newViewCount
            );
            // 刷新 TTL
            stringRedisTemplate.expire(HOT_ARTICLES_KEY, TTL_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("更新 Redis 热门文章缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 获取热门文章 ID 列表（Top N）
     */
    public List<Long> getHotArticleIds(int topN) {
        try {
            // ZREVRANGE: 按分数从高到低取
            Set<String> ids = stringRedisTemplate.opsForZSet()
                    .reverseRange(HOT_ARTICLES_KEY, 0, topN - 1);
            if (ids == null || ids.isEmpty()) {
                return Collections.emptyList();
            }
            return ids.stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("从 Redis 获取热门文章失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 检查缓存是否存在
     */
    public boolean hasCache() {
        try {
            Long size = stringRedisTemplate.opsForZSet().zCard(HOT_ARTICLES_KEY);
            return size != null && size > 0;
        } catch (Exception e) {
            return false;
        }
    }
}
