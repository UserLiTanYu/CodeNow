package com.codenow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 公开作者信息行对象（仅用于Mapper投影，不包含私有用户字段） */
@Data
public class PublicAuthorRow {
    /** 用户ID */
    private Long userId;
    /** 显示名称 */
    private String displayName;
    /** 头像地址 */
    private String avatar;
    /** 个人简介 */
    private String bio;
    /** 擅长领域 */
    private String expertise;
    /** 个人网站地址 */
    private String websiteUrl;
    /** 作品链接 */
    private String portfolioUrl;
    /** 文章数量 */
    private Long articleCount;
    /** 总浏览量 */
    private Long totalViews;
    /** 最近发布时间 */
    private LocalDateTime lastPublishedAt;
}
