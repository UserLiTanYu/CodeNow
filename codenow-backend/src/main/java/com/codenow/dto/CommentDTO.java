package com.codenow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 评论请求参数 */
@Data
@Schema(description = "评论请求参数")
public class CommentDTO {

    /** 文章ID */
    @NotNull(message = "文章 ID 不能为空")
    @Schema(description = "文章 ID", example = "1")
    private Long articleId;

    /** 父评论ID（顶级评论为0或null） */
    @Schema(description = "父评论 ID（顶级评论为 0 或 null）", example = "0")
    private Long parentId;

    /** 评论内容 */
    @NotBlank(message = "评论内容不能为空")
    @Schema(description = "评论内容", example = "这篇文章写得很好！")
    private String content;

    /** 昵称 */
    @Schema(description = "昵称", example = "访客小明")
    private String nickname;

    /** 邮箱（选填，不公开显示） */
    @Schema(description = "邮箱（选填，不公开显示）", example = "test@example.com")
    private String email;
}
