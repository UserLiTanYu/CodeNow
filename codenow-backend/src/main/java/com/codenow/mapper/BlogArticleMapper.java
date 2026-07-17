package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.entity.BlogArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    @Update("UPDATE blog_article SET status = 1 - status WHERE id = #{id} AND is_deleted = 0")
    int toggleStatus(@Param("id") Long id);

    @Update("UPDATE blog_article SET is_top = 1 - is_top WHERE id = #{id} AND is_deleted = 0")
    int toggleTop(@Param("id") Long id);

    @Select("""
            <script>
            SELECT a.*
            FROM blog_article a
            LEFT JOIN blog_category c
              ON c.id = a.category_id AND c.is_deleted = 0
            WHERE a.is_deleted = 0
              AND a.status = 1
            <if test="categoryId != null">
              AND a.category_id = #{categoryId}
            </if>
            <if test="tagId != null">
              AND EXISTS (
                SELECT 1
                FROM blog_article_tag filter_rel
                WHERE filter_rel.article_id = a.id
                  AND filter_rel.tag_id = #{tagId}
                  AND filter_rel.is_deleted = 0
              )
            </if>
            <if test="keyword != null and keyword != ''">
              AND (
                LOCATE(#{keyword}, a.title) &gt; 0
                OR LOCATE(#{keyword}, COALESCE(a.summary, '')) &gt; 0
                OR LOCATE(#{keyword}, COALESCE(c.name, '')) &gt; 0
                OR EXISTS (
                  SELECT 1
                  FROM blog_article_tag search_rel
                  INNER JOIN blog_tag t
                    ON t.id = search_rel.tag_id AND t.is_deleted = 0
                  WHERE search_rel.article_id = a.id
                    AND search_rel.is_deleted = 0
                    AND LOCATE(#{keyword}, t.name) &gt; 0
                )
              )
            </if>
            ORDER BY a.is_top DESC,
            <choose>
              <when test="sort == 'mostViewed'">
                a.view_count DESC, a.create_time DESC
              </when>
              <otherwise>
                a.create_time DESC
              </otherwise>
            </choose>
            </script>
            """)
    Page<BlogArticle> selectPublishedArticlePage(
            Page<BlogArticle> page,
            @Param("categoryId") Long categoryId,
            @Param("tagId") Long tagId,
            @Param("keyword") String keyword,
            @Param("sort") String sort);
}
