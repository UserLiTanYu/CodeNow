package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("blog_comment")
public class BlogComment {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 文章 ID
     */
    private Long articleId;

    /**
     * 父评论 ID（顶级评论为 0）
     */
    private Long parentId;

    private Long userId;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 邮箱（不公开显示）
     */
    private String email;

    /**
     * 评论者 IP
     */
    private String ip;

    /**
     * 状态（0=待审核, 1=已通过）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 子评论列表（非数据库字段，用于树形结构）
     */
    @TableField(exist = false)
    private List<BlogComment> children;
}
