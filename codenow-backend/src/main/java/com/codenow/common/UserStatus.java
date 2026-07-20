package com.codenow.common;

/**
 * 用户状态常量类，定义用户账号的生命周期状态。
 * <p>
 * 用于标识用户当前的账号状态，如正常活跃或已被封禁。
 * </p>
 */
public final class UserStatus {

    /** 正常状态，用户账号正常可用 */
    public static final String ACTIVE = "ACTIVE";

    /** 封禁状态，用户账号已被管理员禁用，无法登录和操作 */
    public static final String BANNED = "BANNED";

    /**
     * 私有构造方法，防止实例化常量类。
     */
    private UserStatus() {
    }
}
