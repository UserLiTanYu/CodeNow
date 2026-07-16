package com.codenow.service;

import java.util.List;

public interface HotArticleService {

    /**
     * 更新文章的浏览量分数（文章被访问时调用）
     */
    void incrementViewCount(Long articleId, int newViewCount);

    /**
     * 获取热门文章 ID 列表（Top N）
     */
    List<Long> getHotArticleIds();

    /**
     * 检查缓存是否存在
     */
    boolean hasCache();
}
