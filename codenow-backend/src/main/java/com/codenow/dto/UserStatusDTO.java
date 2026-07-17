package com.codenow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class UserStatusDTO {
    @NotBlank(message = "状态不能为空")
    @Pattern(regexp = "ACTIVE|BANNED", message = "用户状态不正确")
    private String status;

    private String reason;
}
