package com.codenow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/** 用户状态变更请求参数 */
@Data
public class UserStatusDTO {
    /** 用户状态（ACTIVE=正常, BANNED=封禁） */
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ACTIVE|BANNED", message = "用户状态不正确")
    private String status;

    /** 变更原因 */
    private String reason;
}
