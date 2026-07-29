package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.LocalDate;

/** 站点管理员在博客前台展示的公开简介。 */
@Data
@TableName("site_profile")
public class SiteProfile {
    @TableId(type = IdType.INPUT)
    private Integer id;
    private String siteName;
    private String slogan;
    private String description;
    private String bio;
    private String aboutContent;
    private String contactEmail;
    private String githubUrl;
    private LocalDate foundedAt;
    private LocalDateTime updateTime;
}
