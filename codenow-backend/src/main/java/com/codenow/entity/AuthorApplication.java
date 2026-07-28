package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者申请实体类，记录用户申请成为作者的信息及审核状态
 */
@Data
@TableName("author_application")
public class AuthorApplication {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 申请人用户 ID */
    private Long userId;
    /** 申请理由 */
    private String reason;
    /** 专业领域 */
    private String expertise;
    /** 个人简介 */
    private String bio;
    /** 作品集链接 */
    private String portfolioUrl;
    /** 个人网站链接 */
    private String websiteUrl;
    /** 审核状态（pending/approved/rejected） */
    private String status;
    /** 审核人 ID */
    private Long reviewerId;
    /** 审核备注 */
    private String reviewRemark;
    /** 提交时间 */
    private LocalDateTime submittedAt;
    /** 审核时间 */
    private LocalDateTime reviewedAt;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
