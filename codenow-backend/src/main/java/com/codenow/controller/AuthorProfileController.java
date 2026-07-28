package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.AuthorProfileUpdateDTO;
import com.codenow.entity.AuthorProfile;
import com.codenow.exception.BusinessException;
import com.codenow.service.AuthorProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 作者维护自己公开资料的接口。 */
@Tag(name = "作者资料管理")
@RestController
@RequestMapping("/api/author/profile")
@RequiredArgsConstructor
public class AuthorProfileController {

    private final AuthorProfileService profileService;

    @Operation(summary = "查询我的作者资料")
    @GetMapping
    public R<AuthorProfile> get() {
        AuthorProfile profile = profileService.getByUserId(StpUtil.getLoginIdAsLong());
        if (profile == null) throw new BusinessException(404, "作者资料不存在");
        return R.ok(profile);
    }

    @Operation(summary = "更新我的作者资料")
    @OperationLog("作者更新个人资料")
    @PutMapping
    public R<AuthorProfile> update(@Valid @RequestBody AuthorProfileUpdateDTO dto) {
        return R.ok(profileService.updateAuthorProfile(StpUtil.getLoginIdAsLong(), dto));
    }
}
