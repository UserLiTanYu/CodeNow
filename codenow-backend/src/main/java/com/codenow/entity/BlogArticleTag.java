package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 文章与标签的关联实体类，实现文章和标签的多对多关系
 */
@Data
@TableName("blog_article_tag")
public class BlogArticleTag {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章 ID */
    private Long articleId;

    /** 标签 ID */
    private Long tagId;

    /** 逻辑删除标志（0=未删除, 1=已删除） */
    @TableLogic
    private Integer isDeleted;
}
