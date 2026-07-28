package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客分类实体类，支持树形层级结构，按作者隔离
 */
@Data
@TableName("blog_category")
public class BlogCategory {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** 分类描述 */
    private String description;

    /** 所属作者 ID */
    private Long authorId;

    /** 父分类 ID（顶级分类为 0 或 null） */
    private Long parentId;

    /** 排序序号 */
    private Integer sort;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标志（0=未删除, 1=已删除） */
    @TableLogic
    private Integer isDeleted;

    /** 子分类列表（非数据库字段，用于树形结构展示） */
    @TableField(exist = false)
    private List<BlogCategory> children;
}
