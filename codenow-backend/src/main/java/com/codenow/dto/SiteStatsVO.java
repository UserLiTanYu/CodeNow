package com.codenow.dto;

import lombok.Data;

/** 关于本站页面展示的公开聚合数据。 */
@Data
public class SiteStatsVO {
    private Long articleCount;
    private Long authorCount;
    private Long categoryCount;
    private Long totalViews;
}
