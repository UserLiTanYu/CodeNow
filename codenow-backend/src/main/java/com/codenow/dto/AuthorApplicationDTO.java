package com.codenow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 作者申请请求参数 */
@Data
public class AuthorApplicationDTO {
    /** 申请理由 */
    @NotBlank(message = "申请理由不能为空")
    @Size(min = 50, max = 1000, message = "申请理由长度应为 50-1000 个字符")
    private String reason;

    /** 擅长领域列表 */
    @NotEmpty(message = "请至少填写一个擅长领域")
    @Size(max = 10, message = "擅长领域不能超过 10 项")
    private List<@Valid @NotBlank(message = "擅长领域不能为空") @Size(max = 30, message = "单个擅长领域不能超过 30 个字符") String> expertise;

    /** 个人简介 */
    @NotBlank(message = "个人简介不能为空")
    @Size(min = 20, max = 500, message = "个人简介长度应为 20-500 个字符")
    private String bio;

    /** 作品链接 */
    @Pattern(regexp = "^https?://[^ ]+$", message = "作品链接必须是有效的 HTTP/HTTPS 地址")
    @Size(max = 500, message = "作品链接不能超过 500 个字符")
    private String portfolioUrl;

    /** 个人网站地址 */
    @Pattern(regexp = "^https?://[^ ]+$", message = "个人网站必须是有效的 HTTP/HTTPS 地址")
    @Size(max = 500, message = "个人网站不能超过 500 个字符")
    private String websiteUrl;

    /** 是否同意作者创作规范 */
    @NotNull(message = "请先同意作者创作规范")
    @AssertTrue(message = "请先同意作者创作规范")
    private Boolean agreementAccepted;
}
