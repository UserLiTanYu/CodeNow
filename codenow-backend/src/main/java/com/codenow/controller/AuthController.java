package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codenow.annotation.OperationLog;
import com.codenow.common.R;
import com.codenow.dto.LoginDTO;
import com.codenow.entity.SysUser;
import com.codenow.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @OperationLog("用户登录")
    @Operation(summary = "登录", description = "用户名密码登录，返回 Token")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        SysUser user = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername())
        );
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            return R.error(401, "用户名或密码错误");
        }
        StpUtil.login(user.getId());
        // 将用户名存入 Session，供 AOP 操作日志使用
        StpUtil.getSession().set("username", user.getUsername());
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        return R.ok(result);
    }

    @OperationLog("用户登出")
    @Operation(summary = "登出", description = "退出登录，Token 失效")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @Operation(summary = "获取当前用户信息", description = "根据 Token 获取当前登录用户信息")
    @GetMapping("/me")
    public R<SysUser> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        user.setPassword(null);
        return R.ok(user);
    }
}
