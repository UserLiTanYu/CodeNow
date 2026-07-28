package com.codenow.controller;

import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.SiteProfileUpdateDTO;
import com.codenow.entity.SiteProfile;
import com.codenow.service.SiteProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员维护博客首页公开简介。 */
@Tag(name = "站点简介管理")
@RestController
@RequestMapping("/api/admin/site-profile")
@RequiredArgsConstructor
public class SiteProfileController {
    private final SiteProfileService siteProfileService;

    @Operation(summary = "查询站点简介")
    @GetMapping
    public R<SiteProfile> get() {
        return R.ok(siteProfileService.getSiteProfile());
    }

    @Operation(summary = "更新站点简介")
    @OperationLog("管理员更新个人简介")
    @PutMapping
    public R<SiteProfile> update(@Valid @RequestBody SiteProfileUpdateDTO dto) {
        return R.ok(siteProfileService.updateBio(dto.getBio()));
    }
}
