package com.codenow.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 收藏文章视图对象 */
@Data
public class FavoriteArticleVO {
    /** 文章ID */
    private Long articleId;
    /** 文章标题 */
    private String title;
    /** 文章摘要 */
    private String summary;
    /** 文章创建时间 */
    private LocalDateTime articleCreateTime;
    /** 收藏时间 */
    private LocalDateTime favoriteTime;
}
