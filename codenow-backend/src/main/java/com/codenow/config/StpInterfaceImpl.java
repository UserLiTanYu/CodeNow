package com.codenow.config;

import cn.dev33.satoken.stp.StpInterface;
import com.codenow.entity.SysUser;
import com.codenow.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {
    private final SysUserService userService;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = userService.getById(Long.valueOf(loginId.toString()));
        if (user == null || user.getRole() == null) {
            return Collections.emptyList();
        }
        return List.of(user.getRole().toUpperCase());
    }
}
