package com.codenow.service;

import java.util.List;

/**
 * 热门文章服务接口
 */
public interface HotArticleService {

    /**
     * 更新文章的浏览量分数（文章被访问时调用）
     *
     * @param articleId    文章ID
     * @param newViewCount 更新后的浏览量
     */
    void incrementViewCount(Long articleId, int newViewCount);

    /**
     * 获取热门文章 ID 列表（Top N）
     *
     * @return 热门文章ID列表
     */
    List<Long> getHotArticleIds();

    /**
     * 检查缓存是否存在
     *
     * @return 缓存存在时返回 true
     */
    boolean hasCache();
}
