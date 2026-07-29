package com.codenow.service;

import com.codenow.dto.SiteStatsVO;
import com.codenow.mapper.SiteStatsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

/** 提供带短时本地缓存的公开站点统计，缓存故障不会影响页面访问。 */
@Service
@RequiredArgsConstructor
public class SiteStatsService {
    static final long CACHE_TTL_MILLIS = Duration.ofMinutes(5).toMillis();

    private final SiteStatsMapper siteStatsMapper;
    private volatile SiteStatsVO cachedStats;
    private volatile long cacheExpiresAt;

    public SiteStatsVO getPublicStats() {
        long now = System.currentTimeMillis();
        SiteStatsVO current = cachedStats;
        if (current != null && now < cacheExpiresAt) return current;
        synchronized (this) {
            now = System.currentTimeMillis();
            if (cachedStats != null && now < cacheExpiresAt) return cachedStats;
            SiteStatsVO loaded = siteStatsMapper.selectPublicStats();
            cachedStats = loaded == null ? emptyStats() : loaded;
            cacheExpiresAt = now + CACHE_TTL_MILLIS;
            return cachedStats;
        }
    }

    private SiteStatsVO emptyStats() {
        SiteStatsVO result = new SiteStatsVO();
        result.setArticleCount(0L);
        result.setAuthorCount(0L);
        result.setCategoryCount(0L);
        result.setTotalViews(0L);
        return result;
    }
}
