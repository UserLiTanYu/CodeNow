package com.codenow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.BlogTag;
import com.codenow.mapper.BlogArticleMapper;
import com.codenow.mapper.BlogTagMapper;
import com.codenow.service.BlogTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogTagServiceImpl extends ServiceImpl<BlogTagMapper, BlogTag> implements BlogTagService {

    private final BlogArticleMapper articleMapper;

    @Override
    public List<BlogTag> listByCreator(Long creatorId) {
        return list(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getCreatedBy, creatorId)
                .orderByDesc(BlogTag::getCreateTime));
    }

    @Override
    public List<BlogTag> listByPublishedArticles() {
        List<Long> publishedTagIds = articleMapper.selectPublishedTagIds();
        if (publishedTagIds.isEmpty()) {
            return Collections.emptyList();
        }
        return list(new LambdaQueryWrapper<BlogTag>()
                .in(BlogTag::getId, publishedTagIds)
                .orderByDesc(BlogTag::getCreateTime));
    }
}
