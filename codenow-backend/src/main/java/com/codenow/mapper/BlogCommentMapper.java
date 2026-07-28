package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.entity.BlogComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 博客评论数据访问接口。
 * <p>
 * 提供对博客评论表（blog_comment）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 包含按作者范围查询评论和悲观锁查询等自定义方法。
 * </p>
 */
@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {

    /**
     * 分页查询作者文章下的评论列表（后台管理用）。
     * <p>
     * 查询逻辑：
     * <ul>
     *   <li>通过 INNER JOIN blog_article 关联文章表，确保文章未删除</li>
     *   <li>非管理员只能查看自己 author_id 匹配的文章下的评论</li>
     *   <li>支持按文章ID过滤</li>
     *   <li>返回评论ID、文章ID、父评论ID、用户ID、内容、昵称、状态、创建时间及文章标题</li>
     *   <li>结果按创建时间倒序排列</li>
     * </ul>
     * </p>
     *
     * @param page      分页参数
     * @param articleId 文章ID，为 null 时不过滤
     * @param userId    当前操作用户ID
     * @param admin     是否为管理员，true 时跳过作者范围限制
     * @return 分页结果，包含评论列表及关联的文章标题
     */
    @Select("""
            <script>
            SELECT c.id, c.article_id, c.parent_id, c.user_id, c.content, c.nickname,
                   c.status, c.create_time, a.title AS article_title
            FROM blog_comment c
            INNER JOIN blog_article a ON a.id = c.article_id AND a.is_deleted = 0
            WHERE 1 = 1
            <if test="!admin">AND a.author_id = #{userId}</if>
            <if test="articleId != null">AND c.article_id = #{articleId}</if>
            ORDER BY c.create_time DESC, c.id DESC
            </script>
            """)
    Page<BlogComment> selectAuthorCommentPage(Page<BlogComment> page,
                                               @Param("articleId") Long articleId,
                                               @Param("userId") Long userId,
                                               @Param("admin") boolean admin);

    /**
     * 使用悲观锁查询指定评论（用于并发安全的状态变更操作）。
     * <p>
     * 查询逻辑：
     * <ul>
     *   <li>通过 INNER JOIN blog_article 关联文章表，确保文章未删除</li>
     *   <li>非管理员只能锁定自己 author_id 匹配的文章下的评论</li>
     *   <li>使用 FOR UPDATE 加行锁，防止并发修改冲突</li>
     * </ul>
     * </p>
     *
     * @param id     评论ID
     * @param userId 当前操作用户ID
     * @param admin  是否为管理员，true 时跳过作者范围限制
     * @return 评论实体，不存在或无权限时返回 null
     */
    @Select("""
            <script>
            SELECT c.*
            FROM blog_comment c
            INNER JOIN blog_article a ON a.id = c.article_id AND a.is_deleted = 0
            WHERE c.id = #{id}
            <if test="!admin">AND a.author_id = #{userId}</if>
            FOR UPDATE
            </script>
            """)
    BlogComment selectAuthorCommentForUpdate(@Param("id") Long id,
                                              @Param("userId") Long userId,
                                              @Param("admin") boolean admin);
}
