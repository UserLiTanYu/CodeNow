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

/**
 * 博客标签服务实现类。
 * 管理文章标签的查询操作，支持按创建者筛选和按已发布文章关联筛选。
 */
@Service
@RequiredArgsConstructor
public class BlogTagServiceImpl extends ServiceImpl<BlogTagMapper, BlogTag> implements BlogTagService {

    private final BlogArticleMapper articleMapper;

    /**
     * 根据创建者 ID 查询标签列表
     *
     * @param creatorId 创建者用户 ID
     * @return 该创建者创建的标签列表，按创建时间倒序排列
     */
    @Override
    public List<BlogTag> listByCreator(Long creatorId) {
        return list(new LambdaQueryWrapper<BlogTag>()
                .eq(BlogTag::getCreatedBy, creatorId)
                .orderByDesc(BlogTag::getCreateTime));
    }

    /**
     * 查询已发布文章关联的标签列表
     *
     * @return 已发布文章使用的标签列表，按创建时间倒序排列
     */
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
