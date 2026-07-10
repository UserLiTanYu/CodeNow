package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.BlogArticle;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.service.BlogArticleService;
import org.springframework.stereotype.Service;

@Service
public class BlogArticleServiceImpl extends ServiceImpl<BlogArticleMapper, BlogArticle> implements BlogArticleService {
}
