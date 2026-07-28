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

/**
 * 认证入口。验证码、失败锁定、密码哈希和会话失效共同构成登录与账号恢复的安全边界。
 */
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

    /**
     * 获取图形验证码。
     */
    @GetMapping("/captcha")
    public R<CaptchaVO> captcha() {
        //调用安全服务生成图形验证码，返回验证码ID和图片
        return R.ok(loginSecurityService.createCaptcha());
    }

    /**
     * 用户登录。
     * 支持用户名或邮箱加密码登录，包含验证码校验、失败次数锁定等安全机制。
     */
    @RateLimit(maxCount = 5, timeWindow = 60, message = "登录尝试过于频繁，请 1 分钟后再试")
    @OperationLog("用户登录")
    @Operation(summary = "登录", description = "用户名或邮箱加密码登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        //获取并校验用户输入的账号（用户名或邮箱）
        String account = dto.resolvedAccount();
        if (account == null || account.isBlank()) {
            return R.error(400, "请输入用户名或邮箱");
        }
        //标准化账号格式，去除首尾空格
        String normalizedAccount = account.trim();
        //先消费图形验证码，再查询账号和校验密码，降低自动化撞库成本
        try {
            loginSecurityService.verifyCaptcha(dto.getCaptchaId(), dto.getCaptchaCode());
        } catch (BusinessException e) {
            //验证码错误时记录登录失败日志
            saveLoginLog(null, normalizedAccount, request, false, "图形验证码错误");
            throw e;
        }
        //检查账号是否因连续登录失败被锁定
        if (loginSecurityService.isLocked(normalizedAccount)) {
            saveLoginLog(null, normalizedAccount, request, false, "连续登录失败，账号暂时锁定");
            return R.error(423, "登录失败次数过多，请 15 分钟后再试");
        }
        //根据用户名或邮箱查询用户信息
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, normalizedAccount)
                .or()
                .eq(SysUser::getEmail, normalizedAccount.toLowerCase()));
        //验证用户是否存在且密码正确
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            //记录登录失败次数，用于后续锁定判断
            loginSecurityService.recordFailure(normalizedAccount);
            saveLoginLog(user, normalizedAccount, request, false, "账号或密码错误");
            return R.error(401, "账号或密码错误");
        }
        //检查账号是否被禁用
        if (UserStatus.BANNED.equalsIgnoreCase(user.getStatus())) {
            saveLoginLog(user, normalizedAccount, request, false, "账号已被禁用");
            return R.error(403, "账号已被禁用");
        }

        //登录成功，清除之前的失败记录
        loginSecurityService.clearFailures(normalizedAccount);
        //创建用户会话，记录登录状态
        StpUtil.login(user.getId());
        StpUtil.getSession().set("username", user.getUsername());
        //更新用户最后登录时间和IP地址
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(IpUtils.getRealIp(request));
        userService.updateById(user);
        //记录登录成功日志
        saveLoginLog(user, normalizedAccount, request, true, null);
        //返回登录结果，包含token和用户基本信息
        return R.ok(loginResult(user));
    }

    /**
     * 发送注册验证码。
     * 向指定邮箱发送注册验证码，限制发送频率。
     */
    @RateLimit(maxCount = 10, timeWindow = 3600, message = "验证码发送次数过多，请稍后再试")
    @PostMapping("/register/code")
    public R<Void> sendRegisterCode(@Valid @RequestBody EmailCodeDTO dto) {
        //标准化邮箱格式（去除空格、转小写）
        String email = normalizeEmail(dto.getEmail());
        //检查邮箱是否已被注册
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return R.error(409, "该邮箱已注册");
        }
        //调用邮件服务发送注册验证码
        emailCodeService.sendRegisterCode(email);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 用户注册。
     * 校验验证码后创建新用户账号。
     */
    @RateLimit(maxCount = 10, timeWindow = 600, message = "注册尝试次数过多，请 10 分钟后再试")
    @PostMapping("/register")
    @Transactional
    public R<Void> register(@Valid @RequestBody RegisterDTO dto) {
        //标准化用户名和邮箱格式
        String username = dto.getUsername().trim();
        String email = normalizeEmail(dto.getEmail());
        //检查用户名是否已存在
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0) {
            return R.error(409, "用户名已存在");
        }
        //检查邮箱是否已被注册
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            return R.error(409, "该邮箱已注册");
        }
        //校验邮箱验证码是否正确
        emailCodeService.verifyRegisterCode(email, dto.getVerificationCode());

        //创建新用户实体，设置初始信息
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
        //保存用户到数据库
        userService.save(user);
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 发送密码重置验证码。
     * 向已注册邮箱发送密码重置验证码。
     */
    @RateLimit(maxCount = 10, timeWindow = 3600, message = "验证码发送次数过多，请稍后再试")
    @PostMapping("/password/code")
    public R<Void> sendResetCode(@Valid @RequestBody EmailCodeDTO dto) {
        //标准化邮箱格式
        String email = normalizeEmail(dto.getEmail());
        //仅当邮箱已注册时才发送重置验证码，避免泄露注册信息
        if (userService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email)) > 0) {
            emailCodeService.sendResetCode(email);
        }
        //无论邮箱是否注册都返回成功，防止枚举攻击
        return R.ok();
    }

    /**
     * 重置密码。
     * 校验验证码后重置用户密码，并踢出已有会话。
     */
    @RateLimit(maxCount = 10, timeWindow = 600, message = "密码重置次数过多，请 10 分钟后再试")
    @PostMapping("/password/reset")
    @Transactional
    public R<Void> resetPassword(@Valid @RequestBody ResetPasswordDTO dto) {
        //标准化邮箱格式
        String email = normalizeEmail(dto.getEmail());
        //根据邮箱查询用户，统一返回"验证码错误"防止枚举攻击
        SysUser user = userService.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getEmail, email));
        if (user == null) {
            return R.error(400, "验证码错误或已过期");
        }
        //校验邮箱验证码是否正确
        emailCodeService.verifyResetCode(email, dto.getVerificationCode());
        //加密新密码并更新到数据库
        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        userService.updateById(user);
        //踢出该用户的所有已有会话，强制重新登录
        StpUtil.kickout(user.getId());
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 用户登出。
     * 清除当前会话。
     */
    @OperationLog("用户登出")
    @PostMapping("/logout")
    public R<Void> logout() {
        //清除当前用户的会话信息，使其token失效
        StpUtil.logout();
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 获取当前登录用户信息。
     */
    @GetMapping("/me")
    public R<SysUser> me() {
        //获取当前登录且状态正常的用户信息
        SysUser user = currentActiveUser();
        //清除密码字段，避免敏感信息泄露
        user.setPassword(null);
        //返回用户信息
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
