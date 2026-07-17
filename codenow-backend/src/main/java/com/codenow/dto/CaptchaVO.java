package com.codenow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CaptchaVO {
    private String captchaId;
    private String image;
}
