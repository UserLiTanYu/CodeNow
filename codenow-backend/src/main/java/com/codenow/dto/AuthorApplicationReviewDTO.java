package com.codenow.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/** 作者申请审核请求参数 */
@Data
public class AuthorApplicationReviewDTO {
    /** 审核意见 */
    @Size(max = 500, message = "审核意见不能超过 500 个字符")
    private String reviewRemark;
}
