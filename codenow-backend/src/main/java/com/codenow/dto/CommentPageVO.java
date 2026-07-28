package com.codenow.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.entity.BlogComment;
import lombok.AllArgsConstructor;
import lombok.Data;

/** 评论分页视图对象 */
@Data
@AllArgsConstructor
public class CommentPageVO {
    /** 评论分页数据 */
    private Page<BlogComment> page;
    /** 评论总数 */
    private long totalCount;
}
