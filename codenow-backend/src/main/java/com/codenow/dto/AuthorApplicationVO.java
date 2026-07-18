package com.codenow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AuthorApplicationVO {
    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String reason;
    private List<String> expertise;
    private String bio;
    private String portfolioUrl;
    private String websiteUrl;
    private String status;
    private Long reviewerId;
    private String reviewerName;
    private String reviewRemark;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime updateTime;
}
