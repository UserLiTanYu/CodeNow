package com.codenow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.codenow.entity.SysOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统操作日志数据访问接口。
 * <p>
 * 提供对系统操作日志表（sys_operation_log）的基本 CRUD 操作，继承 MyBatis-Plus 的 BaseMapper。
 * 记录用户在系统中的关键操作，用于操作审计和问题追踪。
 * </p>
 */
@Mapper
public interface SysOperationLogMapper extends BaseMapper<SysOperationLog> {
}
