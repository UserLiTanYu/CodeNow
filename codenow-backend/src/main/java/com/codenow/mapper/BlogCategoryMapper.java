package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.BlogCategory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 博客分类数据访问接口。
 * <p>
 * 提供对博客分类表（blog_category）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 支持父子分类的层级结构，每个作者拥有独立的分类体系。
 * </p>
 */
@Mapper
public interface BlogCategoryMapper extends BaseMapper<BlogCategory> {
}
