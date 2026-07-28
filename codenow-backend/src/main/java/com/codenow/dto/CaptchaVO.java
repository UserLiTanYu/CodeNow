package com.codenow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/** 图形验证码视图对象 */
@Data
@AllArgsConstructor
public class CaptchaVO {
    /** 验证码标识 */
    private String captchaId;
    /** 验证码图片（Base64编码） */
    private String image;
}
