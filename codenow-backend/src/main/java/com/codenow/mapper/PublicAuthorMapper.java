package com.codenow.mapper;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.dto.PublicAuthorRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 公开作者投影查询。SQL 必须同时约束账号有效、AUTHOR 角色、资料完整和至少存在一篇已发布文章。
 */
@Mapper
public interface PublicAuthorMapper {

    /**
     * 分页查询公开的作者列表（前台作者页用）。
     * <p>
     * 查询逻辑：
     * <ul>
     *   <li>关联 author_profile 和 sys_user，确保账号角色为 AUTHOR、状态为 ACTIVE、未删除</li>
     *   <li>LEFT JOIN 子查询统计每位作者的已发布文章数、总浏览量和最近发布时间</li>
     *   <li>昵称为空时自动使用 "作者 {id}" 作为显示名称</li>
     *   <li>支持关键词模糊搜索，搜索范围包括昵称、简介、专业领域</li>
     *   <li>排序支持 latest（按最近发布时间倒序）、articles（按文章数倒序）和默认（综合排序：文章数 > 浏览量 > 发布时间）</li>
     * </ul>
     * </p>
     *
     * @param page    分页参数
     * @param keyword 搜索关键词，为 null 或空时不过滤
     * @param sort    排序方式，"latest" / "articles" / 其他值
     * @return 分页结果，包含作者的公开投影信息（PublicAuthorRow）
     */
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

    /**
     * 根据用户ID查询单个作者的公开信息。
     * <p>
     * 查询逻辑与 {@link #selectPublicAuthorPage} 类似，但为单条查询：
     * <ul>
     *   <li>校验账号角色为 AUTHOR、状态为 ACTIVE、未删除</li>
     *   <li>LEFT JOIN 子查询统计已发布文章数、总浏览量和最近发布时间</li>
     *   <li>昵称为空时自动使用 "作者 {id}" 作为显示名称</li>
     * </ul>
     * </p>
     *
     * @param userId 作者的用户ID
     * @return 作者公开投影信息，不存在或不满足条件时返回 null
     */
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

    /**
     * 批量查询作者的简要公开信息（仅包含用户ID、显示名称和头像）。
     * <p>
     * 查询逻辑：
     * <ul>
     *   <li>校验账号角色为 AUTHOR、状态为 ACTIVE、未删除</li>
     *   <li>通过 IN 子句批量查询指定用户ID列表</li>
     *   <li>仅返回轻量级字段（user_id、display_name、avatar），适用于文章列表中批量装配作者信息</li>
     * </ul>
     * </p>
     *
     * @param userIds 作者用户ID集合
     * @return 作者简要信息列表
     */
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
