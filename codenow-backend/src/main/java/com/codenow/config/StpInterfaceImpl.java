package com.codenow.config;

import cn.dev33.satoken.stp.StpInterface;
import com.codenow.entity.SysUser;
import com.codenow.service.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Sa-Token 角色数据源。每次授权从数据库读取当前角色，不信任 Token 中可能已经过期的角色快照。
 */
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {
    /**
     * 用户服务，用于根据登录 ID 查询用户角色信息
     */
    private final SysUserService userService;

    /**
     * 获取指定用户的权限列表。当前实现未使用细粒度权限，返回空列表。
     *
     * @param loginId   用户登录 ID
     * @param loginType 登录类型
     * @return 权限列表，当前固定返回空列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        return Collections.emptyList();
    }

    /**
     * 获取指定用户的角色列表。从数据库实时读取用户角色，不依赖 Token 中的缓存。
     *
     * @param loginId   用户登录 ID
     * @param loginType 登录类型
     * @return 角色列表，如 ["ADMIN"] 或 ["AUTHOR"]；用户不存在时返回空列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SysUser user = userService.getById(Long.valueOf(loginId.toString()));
        if (user == null || user.getRole() == null) {
            return Collections.emptyList();
        }
        return List.of(user.getRole().toUpperCase());
    }
}
