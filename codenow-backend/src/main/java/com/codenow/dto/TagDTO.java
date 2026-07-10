package com.codenow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "标签请求参数")
public class TagDTO {

    @NotBlank(message = "标签名称不能为空")
    @Schema(description = "标签名称", example = "Spring Boot")
    private String name;
}
