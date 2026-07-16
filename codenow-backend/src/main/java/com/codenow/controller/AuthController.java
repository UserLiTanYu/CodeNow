package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.codenow.annotation.OperationLog;
import com.codenow.annotation.RateLimit;
import com.codenow.common.IpUtils;
import com.codenow.common.R;
import com.codenow.common.UserRole;
import com.codenow.common.UserStatus;
import com.codenow.dto.EmailCodeDTO;
import com.codenow.dto.CaptchaVO;
import com.codenow.dto.LoginDTO;
import com.codenow.dto.RegisterDTO;
import com.codenow.dto.ResetPasswordDTO;
import com.codenow.entity.SysUser;
import com.codenow.entity.LoginLog;
import com.codenow.exception.BusinessException;
import com.codenow.service.EmailCodeService;
import com.codenow.service.SysUserService;
import com.codenow.service.LoginLogService;
import com.codenow.service.LoginSecurityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final SysUserService userService;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailCodeService emailCodeService;
    private final LoginSecurityService loginSecurityService;
    private final LoginLogService loginLogService;

    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        return R.ok(loginSecurityService.createCaptcha());
    }

    @RateLimit(maxCount = 5, timeWindow = 60, message = "登录尝试过于频繁，请 1 分钟后再试")
    @OperationLog("用户登录")
    @Operation(summary = "登录", description = "用户名或邮箱加密码登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        String account = dto.resolvedAccount();
        if (account == null || account.isBlank()) {
            return R.error(400, "请输入用户名或邮箱");
        }
        String normalizedAccount = account.trim();
        try {
            loginSecurityService.verifyCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        } catch (BusinessException e) {
            saveLoginLog(null, normalizedAccount, request, false, "图形验证码错误");
            throw e;
        }
        if (loginSecurityService.isLocked(normalizedAccount)) {
            saveLoginLog(null, normalizedAccount, request, false, "连续登录失败，账号暂时锁定");
            return R.error(423, "登录失败次数过多，请 15 分钟后再试");
        }
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, normalizedAccount)
                .or()
                .eq(SysUser::getEmail, normalizedAccount.toLowerCase()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            loginSecurityService.recordFailure(normalizedAccount);
            saveLoginLog(user, normalizedAccount, request, false, "账号或密码错误");
            return R.error(401, "账号或密码错误");
        }
        if (UserStatus.BANNED.equalsIgnoreCase(user.getStatus())) {
            saveLoginLog(user, normalizedAccount, request, false, "账号已被禁用");
            return R.error(403, "账号已被禁用");
        }

        loginSecurityService.clearFailures(normalizedAccount);
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(IpUtils.getRealIp(request));
        userService.updateById(user);
        saveLoginLog(user, normalizedAccount, request, true, null);
        return R.ok(loginResult(user));
    }

    @RateLimit(maxCount = 10, timeWindow = 3600, message = "验证码发送次数过多，请稍后再试")
    @PostMapping("/register/code")
    public R<Void> sendRegisterCode(@Valid @RequestBody EmailCodeDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return R.error(409, "该邮箱已注册");
        }
        emailCodeService.sendRegisterCode(email);
        return R.ok();
    }

    @RateLimit(maxCount = 10, timeWindow = 600, message = "注册尝试次数过多，请 10 分钟后再试")
    @PostMapping("/register")
    @Transactional
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        String username = dto.getUsername().trim();
        String email = normalizeEmail(dto.getEmail());
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            return R.error(409, "用户名已存在");
        }
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return R.error(409, "该邮箱已注册");
        }
        emailCodeService.verifyRegisterCode(email, dto.getVerificationCode());

        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(username);
        user.setEmail(email);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(1);
        user.setAgreementVersion(dto.getAgreementVersion());
        user.setAgreementAcceptedAt(LocalDateTime.now());
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDeleted(0);
        userService.save(user);
        return R.ok();
    }

    @RateLimit(maxCount = 10, timeWindow = 3600, message = "验证码发送次数过多，请稍后再试")
    @PostMapping("/password/code")
    public R<Void> sendResetCode(@Valid @RequestBody EmailCodeDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            emailCodeService.sendResetCode(email);
        }
        return R.ok();
    }

    @RateLimit(maxCount = 10, timeWindow = 600, message = "密码重置次数过多，请 10 分钟后再试")
    @PostMapping("/password/reset")
    @Transactional
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            return R.error(400, "验证码错误或已过期");
        }
        emailCodeService.verifyResetCode(email, dto.getVerificationCode());
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.updateById(user);
        StpUtil.kickout(user.getId());
        return R.ok();
    }

    @OperationLog("用户登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        StpUtil.logout();
        return R.ok();
    }

    @GetMapping("/me")
    public R<SysUser> me() {
        SysUser user = currentActiveUser();
        user.setPassword(null);
        return R.ok(user);
    }

    private SysUser currentActiveUser() {
        SysUser user = userService.getById(StpUtil.getLoginIdAsLong());
        if (user == null || UserStatus.BANNED.equalsIgnoreCase(user.getStatus())) {
            StpUtil.logout();
            throw new BusinessException(403, "账号不可用");
        }
        return user;
    }

    private Map<String, Object> loginResult(SysUser user) {
        Map<String, Object> result = new HashMap<>();
        result.put("token", StpUtil.getTokenValue());
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("nickname", user.getNickname());
        result.put("avatar", user.getAvatar());
        result.put("role", user.getRole().toUpperCase());
        return result;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private void saveLoginLog(SysUser user, String account, HttpServletRequest request,
                              boolean success, String failureReason) {
        LoginLog log = new LoginLog();
        log.setUserId(user == null ? null : user.getId());
        log.setAccount(account.length() > 100 ? account.substring(0, 100) : account);
        log.setIp(IpUtils.getRealIp(request));
        String userAgent = request.getHeader("User-Agent");
        log.setUserAgent(userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 255)));
        log.setSuccess(success ? 1 : 0);
        log.setFailureReason(failureReason);
        log.setCreateTime(LocalDateTime.now());
        loginLogService.save(log);
    }
}
