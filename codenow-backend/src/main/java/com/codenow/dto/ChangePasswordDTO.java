package com.codenow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 修改密码请求参数 */
@Data
public class ChangePasswordDTO {
    /** 当前密码 */
    @NotBlank(message = "当前密码不能为空")
    private String currentPassword;

    /** 新密码 */
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 72, message = "新密码长度应为 8-72 位")
    private String newPassword;
}
