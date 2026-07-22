package com.codenow.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.codenow.common.R;
import com.codenow.entity.SysOperationLog;
import com.codenow.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
/**
 * 操作日志控制器。
 * 提供操作日志的分页查询功能，供管理后台审计使用。
 */
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * 分页查询操作日志。
     * 按时间倒序查询操作日志列表。
     */
    @Operation(summary = "分页查询操作日志", description = "按时间倒序查询操作日志列表")
    @GetMapping
    public R<Page<SysOperationLog>> list(
            @Parameter(description = "当前页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页条数", example = "10") @RequestParam(defaultValue = "10") Integer pageSize) {
        //分页查询操作日志，按创建时间倒序排列
        Page<SysOperationLog> page = operationLogService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<SysOperationLog>().orderByDesc(SysOperationLog::getCreateTime)
        );
        //返回操作日志分页数据
        return R.ok(page);
    }
}
