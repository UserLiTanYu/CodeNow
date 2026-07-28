package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.BlogArticleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章-标签关联数据访问接口。
 * <p>
 * 提供对文章与标签关联表（blog_article_tag）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 实现文章与标签的多对多关系映射。
 * </p>
 */
@Mapper
public interface BlogArticleTagMapper extends BaseMapper<BlogArticleTag> {
}
