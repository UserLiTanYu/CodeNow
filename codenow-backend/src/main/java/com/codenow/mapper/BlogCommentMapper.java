package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.entity.BlogComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BlogCommentMapper extends BaseMapper<BlogComment> {

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
