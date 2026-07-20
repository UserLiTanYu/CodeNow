package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 登录日志实体类，记录用户每次登录的详细信息和结果
 */
@Data
@TableName("sys_login_log")
public class LoginLog {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 登录账号 */
    private String account;
    /** 登录 IP 地址 */
    private String ip;
    /** 浏览器 User-Agent 信息 */
    private String userAgent;
    /** 登录结果（0=失败, 1=成功） */
    private Integer success;
    /** 登录失败原因 */
    private String failureReason;
    /** 登录时间 */
    private LocalDateTime createTime;
}
