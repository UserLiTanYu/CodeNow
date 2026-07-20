package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.BlogTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客标签数据访问接口。
 * <p>
 * 提供对博客标签表（blog_tag）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 每个作者拥有独立的标签体系，文章可通过多对多关系关联多个标签。
 * </p>
 */
@Mapper
public interface BlogTagMapper extends BaseMapper<BlogTag> {
}
