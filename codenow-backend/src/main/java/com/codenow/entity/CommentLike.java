package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 评论点赞实体类，记录用户对评论的点赞关系
 */
@Data
@TableName("comment_like")
public class CommentLike {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 评论 ID */
    private Long commentId;
    /** 点赞用户 ID */
    private Long userId;
    /** 点赞时间 */
    private LocalDateTime createTime;
}
