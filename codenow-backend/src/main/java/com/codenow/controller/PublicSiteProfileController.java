package com.codenow.controller;

import com.codenow.common.R;
import com.codenow.dto.SiteProfileVO;
import com.codenow.service.SiteProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 博客前台读取站点公开资料。 */
@RestController
@RequestMapping("/api/blog/site-profile")
@RequiredArgsConstructor
public class PublicSiteProfileController {
    private final SiteProfileService siteProfileService;

    @GetMapping
    public R<SiteProfileVO> get() {
        return R.ok(SiteProfileVO.from(siteProfileService.getSiteProfile()));
    }
}
