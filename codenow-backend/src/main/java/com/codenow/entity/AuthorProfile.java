package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("author_profile")
public class AuthorProfile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String bio;
    private String expertise;
    private String websiteUrl;
    private String portfolioUrl;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
