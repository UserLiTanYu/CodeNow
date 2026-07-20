package com.codenow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 作者角色撤销请求参数 */
@Data
public class AuthorRoleRevokeDTO {
    /** 撤销原因 */
    @NotBlank(message = "撤销原因不能为空")
    @Size(max = 500, message = "撤销原因不能超过 500 个字符")
    private String reason;
}
