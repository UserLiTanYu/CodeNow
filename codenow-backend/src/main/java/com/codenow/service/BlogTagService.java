package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.BlogTag;

import java.util.List;

/**
 * 博客标签服务接口
 */
public interface BlogTagService extends IService<BlogTag> {

    /**
     * 查询指定创建者的标签列表
     *
     * @param creatorId 创建者ID
     * @return 标签列表
     */
    List<BlogTag> listByCreator(Long creatorId);

    /**
     * 仅返回至少关联了一篇已发布文章的标签
     *
     * @return 标签列表
     */
    List<BlogTag> listByPublishedArticles();
}
