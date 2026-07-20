package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.SysOperationLog;

/**
 * 操作日志服务接口
 */
public interface OperationLogService extends IService<SysOperationLog> {

    /**
     * 异步保存操作日志
     *
     * @param operationLog 操作日志实体
     */
    void saveAsync(SysOperationLog operationLog);
}
