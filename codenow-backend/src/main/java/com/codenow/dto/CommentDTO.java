package com.codenow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentDTO {

    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    /**
     * 父评论 ID（顶级评论为 0 或 null）
     */
    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    private String content;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    private String email;
}
