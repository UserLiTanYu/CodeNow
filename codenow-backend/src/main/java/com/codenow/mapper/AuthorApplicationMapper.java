package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.AuthorApplication;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作者申请数据访问接口。
 * <p>
 * 提供对作者申请表（author_application）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 记录用户申请成为作者的审批流程数据。
 * </p>
 */
@Mapper
public interface AuthorApplicationMapper extends BaseMapper<AuthorApplication> {
}
