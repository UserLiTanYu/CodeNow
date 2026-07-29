package com.codenow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/** 管理员更新站点公开简介的请求参数。 */
@Data
public class SiteProfileUpdateDTO {
    @NotBlank(message = "站点名称不能为空")
    @Size(min = 2, max = 50, message = "站点名称长度应为 2-50 个字符")
    private String siteName;

    @NotBlank(message = "站点标语不能为空")
    @Size(min = 2, max = 100, message = "站点标语长度应为 2-100 个字符")
    private String slogan;

    @NotBlank(message = "关于页简介不能为空")
    @Size(min = 10, max = 500, message = "关于页简介长度应为 10-500 个字符")
    private String description;

    @NotBlank(message = "个人简介不能为空")
    @Size(min = 10, max = 500, message = "个人简介长度应为 10-500 个字符")
    private String bio;

    @NotBlank(message = "关于页正文不能为空")
    @Size(min = 20, max = 5000, message = "关于页正文长度应为 20-5000 个字符")
    private String aboutContent;

    @Email(message = "联系邮箱格式不正确")
    @Size(max = 100, message = "联系邮箱不能超过 100 个字符")
    private String contactEmail;

    @Pattern(
            regexp = "^$|^https://github\\.com/[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})(?:/[A-Za-z0-9_.-]+)?/?$",
            message = "GitHub 地址格式不正确"
    )
    @Size(max = 255, message = "GitHub 地址不能超过 255 个字符")
    private String githubUrl;

    @PastOrPresent(message = "建站日期不能晚于今天")
    private LocalDate foundedAt;
}
