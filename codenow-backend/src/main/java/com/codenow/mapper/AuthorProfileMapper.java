package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.AuthorProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作者资料数据访问接口。
 * <p>
 * 提供对作者资料表（author_profile）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 存储作者的个人简介、专业领域、网站链接等扩展信息。
 * </p>
 */
@Mapper
public interface AuthorProfileMapper extends BaseMapper<AuthorProfile> {
}
