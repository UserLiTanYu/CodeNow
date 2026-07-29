package com.codenow.mapper;

import com.codenow.dto.SiteStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/** 关于本站公开统计的一次性聚合查询。 */
@Mapper
public interface SiteStatsMapper {
    @Select("""
            SELECT
                (SELECT COUNT(*) FROM blog_article
                    WHERE status = 1 AND is_deleted = 0) AS article_count,
                (SELECT COUNT(*) FROM sys_user
                    WHERE role = 'AUTHOR' AND status = 'ACTIVE' AND is_deleted = 0) AS author_count,
                (SELECT COUNT(*) FROM blog_category
                    WHERE is_deleted = 0) AS category_count,
                (SELECT COALESCE(SUM(view_count), 0) FROM blog_article
                    WHERE status = 1 AND is_deleted = 0) AS total_views
            """)
    SiteStatsVO selectPublicStats();
}
