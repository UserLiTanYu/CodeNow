package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客评论实体类，支持多级嵌套回复的树形评论结构
 */
@Data
@TableName("blog_comment")
public class BlogComment {

    /** 主键 ID */
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

    /** 评论用户 ID（匿名评论时为空） */
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

    /** 点赞数（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private Long likeCount;

    /** 当前用户是否已点赞（非数据库字段） */
    @TableField(exist = false)
    private Boolean liked;

    /** 是否属于当前登录用户（非数据库字段） */
    @TableField(exist = false)
    private Boolean ownedByCurrentUser;

    /** 文章标题（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String articleTitle;

    /** 用户头像地址（非数据库字段） */
    @TableField(exist = false)
    private String avatar;

    /** 用户角色（非数据库字段） */
    @TableField(exist = false)
    private String userRole;
}
