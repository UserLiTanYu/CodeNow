package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者档案实体类，存储作者的个人资料信息
 */
@Data
@TableName("author_profile")
public class AuthorProfile {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 关联的用户 ID */
    private Long userId;
    /** 个人简介 */
    private String bio;
    /** 专业领域 */
    private String expertise;
    /** 个人网站链接 */
    private String websiteUrl;
    /** 作品集链接 */
    private String portfolioUrl;
    /** 创建时间 */
    private LocalDateTime createTime;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
