package com.codenow.dto;

import lombok.Data;

@Data
public class ArticleAuthorVO {
    private Long userId;
    private String displayName;
    private String avatar;
}
