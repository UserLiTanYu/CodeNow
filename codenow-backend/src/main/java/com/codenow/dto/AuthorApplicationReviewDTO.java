package com.codenow.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthorApplicationReviewDTO {
    @Size(max = 500, message = "审核意见不能超过 500 个字符")
    private String reviewRemark;
}
