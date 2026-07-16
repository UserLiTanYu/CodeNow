package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.ArticleFavorite;
import com.codenow.mapper.ArticleFavoriteMapper;
import com.codenow.service.ArticleFavoriteService;
import org.springframework.stereotype.Service;

@Service
public class ArticleFavoriteServiceImpl extends ServiceImpl<ArticleFavoriteMapper, ArticleFavorite>
        implements ArticleFavoriteService {
}
