package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.entity.BlogArticle;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 文章数据访问层。复杂列表 SQL 在数据库侧完成作者范围、发布状态和排序过滤，服务层负责批量装配 VO。
 */
@Mapper
public interface BlogArticleMapper extends BaseMapper<BlogArticle> {

    /**
     * 切换文章的发布状态（已发布/草稿）。
     * <p>
     * 通过位运算将 status 在 0 和 1 之间切换，仅操作未删除的文章。
     * </p>
     *
     * @param id 文章ID
     * @return 受影响的行数，0 表示文章不存在或已删除
     */
    @Update("UPDATE blog_article SET status = 1 - status WHERE id = #{id} AND is_deleted = 0")
    int toggleStatus(@Param("id") Long id);

    /**
     * 切换文章的置顶状态。
     * <p>
     * 通过位运算将 is_top 在 0 和 1 之间切换，仅操作未删除的文章。
     * </p>
     *
     * @param id 文章ID
     * @return 受影响的行数，0 表示文章不存在或已删除
     */
    @Update("UPDATE blog_article SET is_top = 1 - is_top WHERE id = #{id} AND is_deleted = 0")
    int toggleTop(@Param("id") Long id);

    /**
     * 更新作者自己的文章信息。
     * <p>
     * 动态 SQL：非管理员用户只能更新自己 author_id 匹配的文章，管理员可更新任意文章。
     * 支持更新标题、内容、摘要、封面图、分类、状态和排序字段。
     * </p>
     *
     * @param article 文章实体，包含要更新的字段值
     * @param userId  当前操作用户ID
     * @param admin   是否为管理员，true 时跳过作者范围限制
     * @return 受影响的行数，0 表示文章不存在或无权限
     */
    @Update("""
            <script>
            UPDATE blog_article
            SET title = #{article.title}, content = #{article.content}, summary = #{article.summary},
                cover_image = #{article.coverImage}, category_id = #{article.categoryId},
                status = #{article.status}, sort = #{article.sort}
            WHERE id = #{article.id} AND is_deleted = 0
            <if test="!admin">AND author_id = #{userId}</if>
            </script>
            """)
    int updateAuthorArticle(@Param("article") BlogArticle article,
                            @Param("userId") Long userId,
                            @Param("admin") boolean admin);

    /**
     * 逻辑删除作者自己的文章。
     * <p>
     * 将 is_deleted 设为 1 实现软删除。非管理员只能删除自己 author_id 匹配的文章。
     * </p>
     *
     * @param id     文章ID
     * @param userId 当前操作用户ID
     * @param admin  是否为管理员，true 时跳过作者范围限制
     * @return 受影响的行数，0 表示文章不存在或无权限
     */
    @Update("""
            <script>
            UPDATE blog_article SET is_deleted = 1
            WHERE id = #{id} AND is_deleted = 0
            <if test="!admin">AND author_id = #{userId}</if>
            </script>
            """)
    int deleteAuthorArticle(@Param("id") Long id, @Param("userId") Long userId,
                            @Param("admin") boolean admin);

    /**
     * 切换作者自己文章的发布状态。
     * <p>
     * 与 {@link #toggleStatus(Long)} 类似，但增加了作者范围限制：
     * 非管理员只能切换自己 author_id 匹配的文章状态。
     * </p>
     *
     * @param id     文章ID
     * @param userId 当前操作用户ID
     * @param admin  是否为管理员，true 时跳过作者范围限制
     * @return 受影响的行数，0 表示文章不存在或无权限
     */
    @Update("""
            <script>
            UPDATE blog_article SET status = 1 - status
            WHERE id = #{id} AND is_deleted = 0
            <if test="!admin">AND author_id = #{userId}</if>
            </script>
            """)
    int toggleAuthorStatus(@Param("id") Long id, @Param("userId") Long userId,
                           @Param("admin") boolean admin);

    /**
     * 分页查询作者的文章列表（后台管理用）。
     * <p>
     * 动态 SQL 查询逻辑：
     * <ul>
     *   <li>非管理员只能查看自己 author_id 匹配的文章</li>
     *   <li>支持按分类ID列表过滤（多选）</li>
     *   <li>支持按标签ID过滤（通过关联表 EXISTS 子查询）</li>
     *   <li>结果按创建时间倒序排列</li>
     * </ul>
     * </p>
     *
     * @param page        分页参数
     * @param categoryIds 分类ID列表，为 null 或空时不过滤
     * @param tagId       标签ID，为 null 时不过滤
     * @param userId      当前操作用户ID
     * @param admin       是否为管理员，true 时跳过作者范围限制
     * @return 分页结果，包含文章列表
     */
    @Select("""
            <script>
            SELECT a.*
            FROM blog_article a
            WHERE a.is_deleted = 0
            <if test="!admin">AND a.author_id = #{userId}</if>
            <if test="categoryIds != null and !categoryIds.isEmpty()">
              AND a.category_id IN
              <foreach collection="categoryIds" item="categoryId" open="(" separator="," close=")">
                #{categoryId}
              </foreach>
            </if>
            <if test="tagId != null">
              AND EXISTS (
                SELECT 1 FROM blog_article_tag rel
                WHERE rel.article_id = a.id
                  AND rel.tag_id = #{tagId}
                  AND rel.is_deleted = 0
              )
            </if>
            ORDER BY a.create_time DESC, a.id DESC
            </script>
            """)
    Page<BlogArticle> selectAuthorArticlePage(
            Page<BlogArticle> page,
            @Param("categoryIds") java.util.List<Long> categoryIds,
            @Param("tagId") Long tagId,
            @Param("userId") Long userId,
            @Param("admin") boolean admin);

    /**
     * 查询所有已发布文章关联的分类ID（去重）。
     * <p>
     * 仅返回状态为已发布（status=1）且未删除的文章的分类ID，用于构建公开的分类导航树。
     * </p>
     *
     * @return 已发布文章关联的分类ID列表（去重）
     */
    @Select("SELECT DISTINCT a.category_id FROM blog_article a WHERE a.status = 1 AND a.is_deleted = 0 AND a.category_id IS NOT NULL")
    java.util.List<Long> selectPublishedCategoryIds();

    /**
     * 查询所有已发布文章关联的标签ID（去重）。
     * <p>
     * 通过关联表查询已发布且未删除的文章对应的标签ID，用于构建公开的标签导航。
     * </p>
     *
     * @return 已发布文章关联的标签ID列表（去重）
     */
    @Select("SELECT DISTINCT rel.tag_id FROM blog_article_tag rel INNER JOIN blog_article a ON a.id = rel.article_id AND a.status = 1 AND a.is_deleted = 0 WHERE rel.is_deleted = 0")
    java.util.List<Long> selectPublishedTagIds();

    /**
     * 分页查询指定作者的已发布文章（公开页面用）。
     * <p>
     * 查询逻辑：
     * <ul>
     *   <li>通过 author_profile 和 sys_user 关联校验作者身份有效（角色为 AUTHOR、状态为 ACTIVE）</li>
     *   <li>仅查询已发布（status=1）且未删除的文章</li>
     *   <li>支持按分类ID和标签ID过滤</li>
     *   <li>排序支持 mostViewed（按浏览量倒序）和默认（按创建时间倒序）</li>
     * </ul>
     * </p>
     *
     * @param page       分页参数
     * @param authorId   作者用户ID
     * @param sort       排序方式，"mostViewed" 按浏览量排序，其他值按创建时间排序
     * @param categoryId 分类ID，为 null 时不过滤
     * @param tagId      标签ID，为 null 时不过滤
     * @return 分页结果，包含已发布文章列表
     */
    @Select("""
            <script>
            SELECT a.*
            FROM blog_article a
            INNER JOIN author_profile p ON p.user_id = a.author_id
            INNER JOIN sys_user u ON u.id = p.user_id
              AND u.role = 'AUTHOR'
              AND u.status = 'ACTIVE'
              AND u.is_deleted = 0
            WHERE a.author_id = #{authorId}
              AND a.status = 1
              AND a.is_deleted = 0
            <if test="categoryId != null">
              AND a.category_id = #{categoryId}
            </if>
            <if test="tagId != null">
              AND EXISTS (
                SELECT 1 FROM blog_article_tag rel
                WHERE rel.article_id = a.id AND rel.tag_id = #{tagId} AND rel.is_deleted = 0
              )
            </if>
            ORDER BY
            <choose>
              <when test="sort == 'mostViewed'">
                a.view_count DESC, a.create_time DESC, a.id DESC
              </when>
              <otherwise>
                a.create_time DESC, a.id DESC
              </otherwise>
            </choose>
            </script>
            """)
    Page<BlogArticle> selectPublishedAuthorArticlePage(
            Page<BlogArticle> page,
            @Param("authorId") Long authorId,
            @Param("sort") String sort,
            @Param("categoryId") Long categoryId,
            @Param("tagId") Long tagId);

    /**
     * 分页查询全站已发布的文章列表（前台首页/搜索页用）。
     * <p>
     * 查询逻辑：
     * <ul>
     *   <li>仅查询已发布（status=1）且未删除的文章</li>
     *   <li>支持按分类ID列表过滤（多选）</li>
     *   <li>支持按标签ID过滤（通过关联表 EXISTS 子查询）</li>
     *   <li>支持关键词模糊搜索，搜索范围包括文章标题、摘要、分类名称（含父分类）、标签名称</li>
     *   <li>置顶文章始终排在最前</li>
     *   <li>排序支持 mostViewed（按浏览量倒序）、latest（按创建时间倒序）和默认（按分类排序、文章排序、创建时间升序）</li>
     * </ul>
     * </p>
     *
     * @param page        分页参数
     * @param categoryIds 分类ID列表，为 null 或空时不过滤
     * @param tagId       标签ID，为 null 时不过滤
     * @param keyword     搜索关键词，为 null 或空时不过滤
     * @param sort        排序方式，"mostViewed" / "latest" / 其他值
     * @return 分页结果，包含已发布文章列表
     */
    @Select("""
            <script>
            SELECT a.*
            FROM blog_article a
            LEFT JOIN blog_category c
              ON c.id = a.category_id AND c.is_deleted = 0
            LEFT JOIN blog_category pc
              ON pc.id = c.parent_id AND pc.is_deleted = 0
            WHERE a.is_deleted = 0
              AND a.status = 1
            <if test="categoryIds != null and !categoryIds.isEmpty()">
              AND a.category_id IN
              <foreach collection="categoryIds" item="categoryId" open="(" separator="," close=")">
                #{categoryId}
              </foreach>
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
                OR LOCATE(#{keyword}, COALESCE(pc.name, '')) &gt; 0
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
              <when test="sort == 'latest'">
                a.create_time DESC
              </when>
              <otherwise>
                COALESCE(pc.sort, c.sort) ASC,
                CASE WHEN pc.id IS NULL THEN 0 ELSE c.sort END ASC,
                a.sort ASC, a.create_time ASC
              </otherwise>
            </choose>
            </script>
            """)
    Page<BlogArticle> selectPublishedArticlePage(
            Page<BlogArticle> page,
            @Param("categoryIds") java.util.List<Long> categoryIds,
            @Param("tagId") Long tagId,
            @Param("keyword") String keyword,
            @Param("sort") String sort);
}
