package com.codenow.controller;

import com.codenow.common.R;
import com.codenow.dto.SiteStatsVO;
import com.codenow.service.SiteStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 关于本站页面读取公开聚合统计。 */
@RestController
@RequestMapping("/api/blog/site-stats")
@RequiredArgsConstructor
public class PublicSiteStatsController {
    private final SiteStatsService siteStatsService;

    @GetMapping
    public R<SiteStatsVO> get() {
        return R.ok(siteStatsService.getPublicStats());
    }
}
