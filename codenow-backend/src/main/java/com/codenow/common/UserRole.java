package com.codenow.common;

/**
 * 用户角色常量类，定义系统中用户的权限角色。
 * <p>
 * 角色决定了用户在系统中的操作权限，不同角色拥有不同的功能访问范围。
 * </p>
 */
public final class UserRole {

    /** 管理员角色，拥有系统最高权限，可管理所有内容和用户 */
    public static final String ADMIN = "ADMIN";

    /** 作者角色，拥有发布和管理自己文章的权限 */
    public static final String AUTHOR = "AUTHOR";

    /** 普通用户角色，拥有浏览文章、发表评论等基本权限 */
    public static final String USER = "USER";

    /**
     * 私有构造方法，防止实例化常量类。
     */
    private UserRole() {
    }
}
