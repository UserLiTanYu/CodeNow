package com.codenow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 作者维护公开资料时提交的请求参数。 */
@Data
public class AuthorProfileUpdateDTO {

    @NotBlank(message = "个人简介不能为空")
    @Size(min = 20, max = 500, message = "个人简介长度应为 20-500 个字符")
    private String bio;

    @NotEmpty(message = "请至少填写一个擅长领域")
    @Size(max = 10, message = "擅长领域不能超过 10 项")
    private List<@Valid @NotBlank(message = "擅长领域不能为空")
            @Size(max = 30, message = "单个擅长领域不能超过 30 个字符") String> expertise;

    @Pattern(regexp = "^(?:|https?://\\S+)$", message = "个人网站必须是有效的 HTTP/HTTPS 地址")
    @Size(max = 500, message = "个人网站不能超过 500 个字符")
    private String websiteUrl;

    @Pattern(regexp = "^(?:|https?://\\S+)$", message = "作品链接必须是有效的 HTTP/HTTPS 地址")
    @Size(max = 500, message = "作品链接不能超过 500 个字符")
    private String portfolioUrl;
}
