package com.codenow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthorRoleRevokeDTO {
    @NotBlank(message = "撤销原因不能为空")
    @Size(max = 500, message = "撤销原因不能超过 500 个字符")
    private String reason;
}
