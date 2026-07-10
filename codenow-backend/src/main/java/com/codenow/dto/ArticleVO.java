package com.codenow.dto;

import com.codenow.entity.BlogArticle;
import com.codenow.entity.BlogTag;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "文章详情（含分类名称和标签列表）")
public class ArticleVO {

    @Schema(description = "文章信息")
    private BlogArticle article;

    @Schema(description = "分类名称", example = "Java")
    private String categoryName;

    @Schema(description = "标签列表")
    private List<BlogTag> tags;
}
