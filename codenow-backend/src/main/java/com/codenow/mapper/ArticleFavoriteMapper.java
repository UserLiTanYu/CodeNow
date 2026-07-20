package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.ArticleFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章收藏数据访问接口。
 * <p>
 * 提供对文章收藏表（article_favorite）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * </p>
 */
@Mapper
public interface ArticleFavoriteMapper extends BaseMapper<ArticleFavorite> {
}
