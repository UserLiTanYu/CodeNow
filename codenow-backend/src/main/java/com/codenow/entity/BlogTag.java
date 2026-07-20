package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 博客标签实体类，用于文章的标签分类管理
 */
@Data
@TableName("blog_tag")
public class BlogTag {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称 */
    private String name;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 创建者用户 ID */
    private Long createdBy;

    /** 逻辑删除标志（0=未删除, 1=已删除） */
    @TableLogic
    private Integer isDeleted;
}
