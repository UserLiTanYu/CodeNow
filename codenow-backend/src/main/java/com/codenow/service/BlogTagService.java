package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.BlogTag;

import java.util.List;

public interface BlogTagService extends IService<BlogTag> {
    List<BlogTag> listByCreator(Long creatorId);

    /**
     * 仅返回至少关联了一篇已发布文章的标签
     */
    List<BlogTag> listByPublishedArticles();
}
