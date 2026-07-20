package com.codenow.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 登录请求参数 */
@Data
@Schema(description = "登录请求参数")
public class LoginDTO {

    /** 用户名（兼容旧版客户端） */
    @Schema(description = "用户名（兼容旧版客户端）", example = "admin")
    private String username;

    /** 用户名或邮箱 */
    @Schema(description = "用户名或邮箱", example = "admin")
    private String account;

    /** 密码 */
    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码", example = "123456")
    private String password;

    /** 验证码标识 */
    @NotBlank(message = "验证码标识不能为空")
    private String captchaId;

    /** 图形验证码 */
    @NotBlank(message = "图形验证码不能为空")
    private String captchaCode;

    public String resolvedAccount() {
        return account != null && !account.isBlank() ? account : username;
    }
}
