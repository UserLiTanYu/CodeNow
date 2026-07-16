package com.codenow.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.R;
import com.codenow.common.UserRole;
import com.codenow.common.UserStatus;
import com.codenow.dto.UserStatusDTO;
import com.codenow.entity.SysUser;
import com.codenow.service.SysUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final SysUserService userService;

    @GetMapping
    public R<Page<SysUser>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .and(keyword != null && !keyword.isBlank(), query -> query
                        .like(SysUser::getUsername, keyword.trim())
                        .or().like(SysUser::getNickname, keyword.trim())
                        .or().like(SysUser::getEmail, keyword.trim()))
                .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = userService.page(new Page<>(pageNum, Math.min(pageSize, 100)), wrapper);
        page.getRecords().forEach(user -> user.setPassword(null));
        return R.ok(page);
    }

    @PutMapping("/{id}/status")
    public R<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        if (id.equals(StpUtil.getLoginIdAsLong())) {
            return R.error(400, "不能修改当前登录账号的状态");
        }
        SysUser user = userService.getById(id);
        if (user == null) {
            return R.error(404, "用户不存在");
        }
        if (UserRole.ADMIN.equalsIgnoreCase(user.getRole())) {
            return R.error(400, "不能通过此接口禁用管理员");
        }
        user.setStatus(dto.getStatus());
        userService.updateById(user);
        if (UserStatus.BANNED.equals(dto.getStatus())) {
            StpUtil.kickout(id);
        }
        return R.ok();
    }
}
