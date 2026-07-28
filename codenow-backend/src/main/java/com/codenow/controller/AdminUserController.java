package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.R;
import com.codenow.common.UserRole;
import com.codenow.common.UserStatus;
import com.codenow.dto.UserStatusDTO;
import com.codenow.entity.SysUser;
import com.codenow.entity.LoginLog;
import com.codenow.service.SysUserService;
import com.codenow.service.LoginLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
/**
 * 管理员用户管理控制器。
 * 提供用户列表查询、用户状态管理及登录日志查看等管理功能。
 */
public class AdminUserController {
    private final SysUserService userService;
    private final LoginLogService loginLogService;

    /**
     * 分页查询用户列表。
     * 支持按用户名、昵称或邮箱关键词搜索，按创建时间倒序排列。返回数据已脱敏。
     */
    @GetMapping
    public R<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        //构建查询条件：支持按用户名、昵称或邮箱模糊搜索，按创建时间倒序排列
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(SysUser::getUsername, keyword.trim())
                        .or().like(SysUser::getNickname, keyword.trim())
                        .or().like(SysUser::getEmail, keyword.trim()))
                .orderByDesc(SysUser::getCreateTime);
        //分页查询用户列表，限制每页最多100条
        Page<SysUser> page = userService.page(new Page<>(pageNum, Math.min(pageSize, 100)), wrapper);
        //清除密码字段，避免敏感信息泄露
        page.getRecords().forEach(user -> user.setPassword(null));
        //返回脱敏后的用户分页数据
        return R.ok(page);
    }

    /**
     * 修改用户状态。
     * 支持启用或禁用用户，禁用时需填写原因，禁用后自动踢出会话。
     */
    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        //禁止管理员修改自己的状态，防止误操作导致账号锁定
        if (id.equals(StpUtil.getLoginIdAsLong())) {
            return R.error(400, "不能修改当前登录账号的状态");
        }
        //根据用户ID查询用户信息
        SysUser user = userService.getById(id);
        //用户不存在时返回404错误
        if (user == null) {
            return R.error(404, "用户不存在");
        }
        //禁止通过此接口禁用管理员账号
        if (UserRole.ADMIN.equalsIgnoreCase(user.getRole())) {
            return R.error(400, "不能通过此接口禁用管理员");
        }
        //禁用用户时必须填写原因
        if (UserStatus.BANNED.equals(dto.getStatus()) && (dto.getReason() == null || dto.getReason().trim().isBlank())) {
            return R.error(400, "禁用用户时必须填写原因");
        }
        //设置用户状态、禁用原因和禁用时间
        user.setStatus(dto.getStatus());
        user.setBanReason(UserStatus.BANNED.equals(dto.getStatus()) ? dto.getReason().trim() : null);
        user.setBannedAt(UserStatus.BANNED.equals(dto.getStatus()) ? java.time.LocalDateTime.now() : null);
        //更新用户信息到数据库
        userService.updateById(user);
        //禁用用户时自动踢出其所有会话
        if (UserStatus.BANNED.equals(dto.getStatus())) {
            StpUtil.kickout(id);
        }
        //返回正确的响应结果
        return R.ok();
    }

    /**
     * 分页查询登录日志。
     * 支持按用户 ID、账号名称和登录结果筛选。
     */
    @GetMapping("/login-logs")
    public R<Page<LoginLog>> loginLogs(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String account,
            @RequestParam(required = false) Integer success) {
        //构建查询条件：支持按用户ID、账号名称和登录结果筛选，按创建时间倒序排列
        return R.ok(loginLogService.page(new Page<>(pageNum, Math.min(pageSize, 100)),
                new LambdaQueryWrapper<LoginLog>()
                        .eq(userId != null, LoginLog::getUserId, userId)
                        .like(account != null && !account.isBlank(), LoginLog::getAccount, account == null ? null : account.trim())
                        .eq(success != null, LoginLog::getSuccess, success)
                        .orderByDesc(LoginLog::getCreateTime)));
    }
}
