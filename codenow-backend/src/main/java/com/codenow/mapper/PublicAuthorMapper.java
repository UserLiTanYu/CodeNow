package com.codenow.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.PublicAuthorRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PublicAuthorMapper {

    @Select("""
            <script>
            SELECT u.id AS user_id,
                   COALESCE(NULLIF(TRIM(u.nickname), ''), CONCAT('作者 ', u.id)) AS display_name,
                   u.avatar,
                   p.bio,
                   p.expertise,
                   p.website_url,
                   p.portfolio_url,
                   COALESCE(stats.article_count, 0) AS article_count,
                   COALESCE(stats.total_views, 0) AS total_views,
                   stats.last_published_at
            FROM author_profile p
            INNER JOIN sys_user u ON u.id = p.user_id
            LEFT JOIN (
                SELECT author_id,
                       COUNT(*) AS article_count,
                       COALESCE(SUM(view_count), 0) AS total_views,
                       MAX(create_time) AS last_published_at
                FROM blog_article
                WHERE status = 1 AND is_deleted = 0
                GROUP BY author_id
            ) stats ON stats.author_id = u.id
            WHERE u.role = 'AUTHOR'
              AND u.status = 'ACTIVE'
              AND u.is_deleted = 0
            <if test="keyword != null and keyword != ''">
              AND (
                LOCATE(#{keyword}, COALESCE(u.nickname, '')) &gt; 0
                OR LOCATE(#{keyword}, p.bio) &gt; 0
                OR LOCATE(#{keyword}, p.expertise) &gt; 0
              )
            </if>
            ORDER BY
            <choose>
              <when test="sort == 'latest'">
                COALESCE(stats.last_published_at, p.create_time) DESC, u.id DESC
              </when>
              <when test="sort == 'articles'">
                COALESCE(stats.article_count, 0) DESC, u.id DESC
              </when>
              <otherwise>
                COALESCE(stats.article_count, 0) DESC,
                COALESCE(stats.total_views, 0) DESC,
                COALESCE(stats.last_published_at, p.create_time) DESC,
                u.id DESC
              </otherwise>
            </choose>
            </script>
            """)
    Page<PublicAuthorRow> selectPublicAuthorPage(Page<PublicAuthorRow> page,
                                                  @Param("keyword") String keyword,
                                                  @Param("sort") String sort);

    @Select("""
            SELECT u.id AS user_id,
                   COALESCE(NULLIF(TRIM(u.nickname), ''), CONCAT('作者 ', u.id)) AS display_name,
                   u.avatar,
                   p.bio,
                   p.expertise,
                   p.website_url,
                   p.portfolio_url,
                   COALESCE(stats.article_count, 0) AS article_count,
                   COALESCE(stats.total_views, 0) AS total_views,
                   stats.last_published_at
            FROM author_profile p
            INNER JOIN sys_user u ON u.id = p.user_id
            LEFT JOIN (
                SELECT author_id,
                       COUNT(*) AS article_count,
                       COALESCE(SUM(view_count), 0) AS total_views,
                       MAX(create_time) AS last_published_at
                FROM blog_article
                WHERE status = 1 AND is_deleted = 0
                GROUP BY author_id
            ) stats ON stats.author_id = u.id
            WHERE u.id = #{userId}
              AND u.role = 'AUTHOR'
              AND u.status = 'ACTIVE'
              AND u.is_deleted = 0
            """)
    PublicAuthorRow selectPublicAuthorByUserId(@Param("userId") Long userId);

    @Select("""
            <script>
            SELECT u.id AS user_id,
                   COALESCE(NULLIF(TRIM(u.nickname), ''), CONCAT('作者 ', u.id)) AS display_name,
                   u.avatar
            FROM author_profile p
            INNER JOIN sys_user u ON u.id = p.user_id
            WHERE u.role = 'AUTHOR'
              AND u.status = 'ACTIVE'
              AND u.is_deleted = 0
              AND u.id IN
              <foreach collection="userIds" item="userId" open="(" separator="," close=")">
                #{userId}
              </foreach>
            </script>
            """)
    java.util.List<PublicAuthorRow> selectPublicAuthorSummariesByUserIds(
            @Param("userIds") java.util.Collection<Long> userIds);
}
