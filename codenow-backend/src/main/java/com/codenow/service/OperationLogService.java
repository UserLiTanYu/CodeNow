package com.codenow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.codenow.entity.SysOperationLog;

public interface OperationLogService extends IService<SysOperationLog> {

    /**
     * 异步保存操作日志
     */
    void saveAsync(SysOperationLog operationLog);
}
