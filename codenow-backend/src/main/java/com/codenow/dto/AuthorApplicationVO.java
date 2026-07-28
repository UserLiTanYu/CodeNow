package com.codenow.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 作者申请详情视图对象 */
@Data
public class AuthorApplicationVO {
    /** 申请ID */
    private Long id;
    /** 申请人用户ID */
    private Long userId;
    /** 申请人用户名 */
    private String username;
    /** 申请人昵称 */
    private String nickname;
    /** 申请人邮箱 */
    private String email;
    /** 申请理由 */
    private String reason;
    /** 擅长领域列表 */
    private List<String> expertise;
    /** 个人简介 */
    private String bio;
    /** 作品链接 */
    private String portfolioUrl;
    /** 个人网站地址 */
    private String websiteUrl;
    /** 申请状态 */
    private String status;
    /** 审核人用户ID */
    private Long reviewerId;
    /** 审核人姓名 */
    private String reviewerName;
    /** 审核意见 */
    private String reviewRemark;
    /** 提交时间 */
    private LocalDateTime submittedAt;
    /** 审核时间 */
    private LocalDateTime reviewedAt;
    /** 更新时间 */
    private LocalDateTime updateTime;
}
