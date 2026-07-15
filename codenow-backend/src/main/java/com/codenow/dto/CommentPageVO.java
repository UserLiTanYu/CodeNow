package com.codenow.dto;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.entity.BlogComment;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommentPageVO {
    private Page<BlogComment> page;
    private long totalCount;
}
