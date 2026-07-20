package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.CommentLike;
import com.codenow.mapper.CommentLikeMapper;
import com.codenow.service.CommentLikeService;
import org.springframework.stereotype.Service;

/**
 * 评论点赞服务实现类。
 * 管理用户对评论的点赞记录，支持点赞和取消点赞操作。
 */
@Service
public class CommentLikeServiceImpl extends ServiceImpl<CommentLikeMapper, CommentLike> implements CommentLikeService {}
