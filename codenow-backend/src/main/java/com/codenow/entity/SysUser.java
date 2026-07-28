package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统用户实体类，存储用户账号、角色及状态等核心信息
 */
@Data
@TableName("sys_user")
public class SysUser {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名（登录账号） */
    private String username;

    /** 密码（加密存储） */
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 头像地址 */
    private String avatar;

    /** 邮箱地址 */
    private String email;

    /** 用户角色（如 admin、author、user） */
    private String role;

    /** 账号状态（active=正常, banned=封禁） */
    private String status;

    /** 邮箱是否已验证（0=未验证, 1=已验证） */
    private Integer emailVerified;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录 IP 地址 */
    private String lastLoginIp;

    /** 封禁原因 */
    private String banReason;

    /** 封禁时间 */
    private LocalDateTime bannedAt;

    /** 用户协议版本号 */
    private String agreementVersion;

    /** 用户协议同意时间 */
    private LocalDateTime agreementAcceptedAt;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标志（0=未删除, 1=已删除） */
    @TableLogic
    private Integer isDeleted;
}
