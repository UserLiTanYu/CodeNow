package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("blog_article")
public class BlogArticle {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String contentHtml;

    private String summary;

    private String coverImage;

    private Long categoryId;

    private Long authorId;

    private Integer status;

    private Integer isTop;

    private Integer sort;

    private Integer viewCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}
