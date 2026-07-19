package com.codenow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** Mapper-only projection. It deliberately contains no private SysUser fields. */
@Data
public class PublicAuthorRow {
    private Long userId;
    private String displayName;
    private String avatar;
    private String bio;
    private String expertise;
    private String websiteUrl;
    private String portfolioUrl;
    private Long articleCount;
    private Long totalViews;
    private LocalDateTime lastPublishedAt;
}
