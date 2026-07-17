package com.codenow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "文章请求参数")
public class ArticleDTO {

    @NotBlank(message = "文章标题不能为空")
    @Schema(description = "文章标题", example = "Spring Boot 入门教程")
    private String title;

    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "文章内容（Markdown 格式）", example = "# Hello World")
    private String content;

    @Schema(description = "文章摘要", example = "这是一篇 Spring Boot 入门文章")
    private String summary;

    @Schema(description = "封面图 URL", example = "https://example.com/cover.jpg")
    private String coverImage;

    @Schema(description = "所属分类 ID", example = "1")
    private Long categoryId;

    @Schema(description = "文章状态（0=草稿, 1=已发布）", example = "0")
    private Integer status;

    @Schema(description = "学习顺序（数字越小越靠前）", example = "1")
    private Integer sort;

    @Schema(description = "标签 ID 列表", example = "[1, 2]")
    private List<Long> tagIds;
}
