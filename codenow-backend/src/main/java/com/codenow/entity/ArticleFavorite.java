package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章收藏实体类，记录用户对文章的收藏关系
 */
@Data
@TableName("article_favorite")
public class ArticleFavorite {
    /** 主键 ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户 ID */
    private Long userId;
    /** 文章 ID */
    private Long articleId;
    /** 收藏时间 */
    private LocalDateTime createTime;
}
