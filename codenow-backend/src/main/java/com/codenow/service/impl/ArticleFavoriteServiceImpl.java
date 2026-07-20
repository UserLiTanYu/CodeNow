package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.ArticleFavorite;
import com.codenow.mapper.ArticleFavoriteMapper;
import com.codenow.service.ArticleFavoriteService;
import org.springframework.stereotype.Service;

/**
 * 文章收藏服务实现类。
 * 继承 MyBatis-Plus 通用服务，提供文章收藏的 CRUD 基础能力。
 */
@Service
public class ArticleFavoriteServiceImpl extends ServiceImpl<ArticleFavoriteMapper, ArticleFavorite>
        implements ArticleFavoriteService {
}
