package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.BlogArticleTag;
import com.codenow.mapper.BlogArticleTagMapper;
import com.codenow.service.BlogArticleTagService;
import org.springframework.stereotype.Service;

@Service
public class BlogArticleTagServiceImpl extends ServiceImpl<BlogArticleTagMapper, BlogArticleTag> implements BlogArticleTagService {
}
