package com.codenow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 文章包视图对象，用于文章导入导出 */
@Data
@AllArgsConstructor
public class ArticlePackageVO {
    /** 文章标题 */
    private String title;
    /** 文章内容 */
    private String content;
    /** 图片数量 */
    private int imageCount;
}
