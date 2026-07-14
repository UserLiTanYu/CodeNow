package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.BlogComment;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {
}
