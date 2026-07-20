package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 博客文章实体类，存储文章的标题、内容、分类等信息
 */
@Data
@TableName("blog_article")
public class BlogArticle {

    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文章标题 */
    private String title;

    /** 文章内容（Markdown 格式） */
    private String content;

    /** 文章内容（HTML 格式） */
    private String contentHtml;

    /** 文章摘要 */
    private String summary;

    /** 封面图片地址 */
    private String coverImage;

    /** 分类 ID */
    private Long categoryId;

    /** 作者 ID */
    private Long authorId;

    /** 文章状态（0=草稿, 1=已发布, 2=已下架） */
    private Integer status;

    /** 是否置顶（0=否, 1=是） */
    private Integer isTop;

    /** 排序序号 */
    private Integer sort;

    /** 浏览次数 */
    private Integer viewCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;

    /** 逻辑删除标志（0=未删除, 1=已删除） */
    @TableLogic
    private Integer isDeleted;
}
