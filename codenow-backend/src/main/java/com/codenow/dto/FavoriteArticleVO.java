package com.codenow.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteArticleVO {
    private Long articleId;
    private String title;
    private String summary;
    private LocalDateTime articleCreateTime;
    private LocalDateTime favoriteTime;
}
