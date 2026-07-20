package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论点赞数据访问接口。
 * <p>
 * 提供对评论点赞表（comment_like）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 记录用户对评论的点赞信息，用于防止重复点赞。
 * </p>
 */
@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {}
