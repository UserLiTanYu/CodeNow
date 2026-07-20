package com.codenow.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 修改邮箱请求参数 */
@Data
public class ChangeEmailDTO {
    /** 新邮箱地址 */
    @NotBlank(message = "新邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /** 验证码 */
    @NotBlank(message = "验证码不能为空")
    private String verificationCode;
}
