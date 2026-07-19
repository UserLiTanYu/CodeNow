package com.codenow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PublicAuthorVO {
    private Long userId;
    private String displayName;
    private String avatar;
    private String bio;
    private List<String> expertise;
    private String websiteUrl;
    private String portfolioUrl;
    private Long articleCount;
    private Long totalViews;
    private LocalDateTime lastPublishedAt;
}
