package com.codenow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 分类请求参数 */
@Data
@Schema(description = "分类请求参数")
public class CategoryDTO {

    /** 分类名称 */
    @NotBlank(message = "分类名称不能为空")
    @Schema(description = "分类名称", example = "Java")
    private String name;

    /** 分类描述 */
    @Schema(description = "分类描述", example = "Java 技术相关文章")
    private String description;

    /** 父分类ID，0表示一级分类 */
    @Schema(description = "父分类 ID，0 表示一级分类", example = "0")
    private Long parentId;

    /** 排序号（数字越小越靠前） */
    @Schema(description = "排序号（数字越小越靠前）", example = "1")
    private Integer sort;
}
