package com.codenow.controller;

import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.SiteProfileUpdateDTO;
import com.codenow.dto.SiteProfileVO;
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

/** 管理员维护前台公开展示的站点资料。 */
@Tag(name = "站点资料管理")
@RestController
@RequestMapping("/api/admin/site-profile")
@RequiredArgsConstructor
public class SiteProfileController {
    private final SiteProfileService siteProfileService;

    @Operation(summary = "查询站点资料")
    @GetMapping
    public R<SiteProfileVO> get() {
        return R.ok(SiteProfileVO.from(siteProfileService.getSiteProfile()));
    }

    @Operation(summary = "更新站点资料")
    @OperationLog("管理员更新站点资料")
    @PutMapping
    public R<SiteProfileVO> update(@Valid @RequestBody SiteProfileUpdateDTO dto) {
        return R.ok(SiteProfileVO.from(siteProfileService.updateProfile(dto)));
    }
}
