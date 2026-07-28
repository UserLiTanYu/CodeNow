package com.codenow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 管理员更新站点公开简介的请求参数。 */
@Data
public class SiteProfileUpdateDTO {
    @NotBlank(message = "个人简介不能为空")
    @Size(min = 10, max = 500, message = "个人简介长度应为 10-500 个字符")
    private String bio;
}
