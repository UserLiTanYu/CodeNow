package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.BlogArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    @Update("UPDATE blog_article SET status = 1 - status WHERE id = #{id} AND is_deleted = 0")
    int toggleStatus(@Param("id") Long id);

    @Update("UPDATE blog_article SET is_top = 1 - is_top WHERE id = #{id} AND is_deleted = 0")
    int toggleTop(@Param("id") Long id);
}
