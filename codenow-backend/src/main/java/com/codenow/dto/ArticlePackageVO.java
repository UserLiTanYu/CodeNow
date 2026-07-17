package com.codenow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ArticlePackageVO {
    private String title;
    private String content;
    private int imageCount;
}
