package com.codenow.dto;

import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/** 文章详情视图对象（含分类名称和标签列表） */
@Data
@Schema(description = "文章详情（含分类名称和标签列表）")
public class ArticleVO {

    /** 文章信息 */
    @Schema(description = "文章信息")
    private BlogArticle article;

    /** 分类名称 */
    @Schema(description = "分类名称", example = "Java")
    private String categoryName;

    /** 标签列表 */
    @Schema(description = "标签列表")
    private List<BlogTag> tags;

    /** 可公开发现的作者摘要；作者不可发现时为空 */
    @Schema(description = "可公开发现的作者摘要；作者不可发现时为空")
    private ArticleAuthorVO author;
}
