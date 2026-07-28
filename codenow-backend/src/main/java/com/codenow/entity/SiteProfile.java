package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 站点管理员在博客前台展示的公开简介。 */
@Data
@TableName("site_profile")
public class SiteProfile {
    @TableId(type = IdType.INPUT)
    private Integer id;
    private String bio;
    private LocalDateTime updateTime;
}
