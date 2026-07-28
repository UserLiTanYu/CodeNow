package com.codenow.dto;

import lombok.Data;

/** 文章作者信息视图对象 */
@Data
public class ArticleAuthorVO {
    /** 用户ID */
    private Long userId;
    /** 显示名称 */
    private String displayName;
    /** 头像地址 */
    private String avatar;
}
