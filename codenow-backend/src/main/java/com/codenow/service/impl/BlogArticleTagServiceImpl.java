package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.BlogArticleTag;
import com.codenow.mapper.BlogArticleTagMapper;
import com.codenow.service.BlogArticleTagService;
import org.springframework.stereotype.Service;

/**
 * 文章-标签关联服务实现类。
 * 管理文章与标签之间的多对多关系映射。
 */
@Service
public class BlogArticleTagServiceImpl extends ServiceImpl<BlogArticleTagMapper, BlogArticleTag> implements BlogArticleTagService {
}
