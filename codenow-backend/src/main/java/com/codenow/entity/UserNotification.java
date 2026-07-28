package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户通知实体类，记录系统发送给用户的通知消息
 */
@Data
@TableName("user_notification")
public class UserNotification {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 接收通知的用户 ID */
    private Long userId;
    /** 通知类型（如 comment、like、system） */
    private String type;
    /** 通知标题 */
    private String title;
    /** 通知内容 */
    private String content;
    /** 关联的文章 ID */
    private Long articleId;
    /** 关联的评论 ID */
    private Long commentId;
    /** 是否已读（0=未读, 1=已读） */
    private Integer isRead;
    /** 创建时间 */
    private LocalDateTime createTime;
}
