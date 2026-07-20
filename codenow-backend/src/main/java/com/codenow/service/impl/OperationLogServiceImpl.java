package com.codenow.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.codenow.entity.SysOperationLog;
import com.codenow.mapper.SysOperationLogMapper;
import com.codenow.service.OperationLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Best-effort 异步审计写入。日志异常在内部记录并吞掉，不参与也不回滚调用方业务事务。
 */
@Slf4j
@Service
public class OperationLogServiceImpl extends ServiceImpl<SysOperationLogMapper, SysOperationLog> implements OperationLogService {

    /**
     * 异步保存操作日志。
     * 日志异常在内部记录并吞掉，不参与也不回滚调用方业务事务。
     *
     * @param operationLog 操作日志实体
     */
    @Override
    @Async
    public void saveAsync(SysOperationLog operationLog) {
        try {
            save(operationLog);
        } catch (Exception e) {
            log.error("异步保存操作日志失败: {}", e.getMessage(), e);
        }
    }
}
