package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("author_application")
public class AuthorApplication {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String reason;
    private String expertise;
    private String bio;
    private String portfolioUrl;
    private String websiteUrl;
    private String status;
    private Long reviewerId;
    private String reviewRemark;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
    private LocalDateTime updateTime;
}
