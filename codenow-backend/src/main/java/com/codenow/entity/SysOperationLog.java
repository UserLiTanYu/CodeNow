package com.codenow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 操作人 ID
     */
    private Long userId;

    /**
     * 操作人用户名
     */
    private String username;

    /**
     * 操作描述（如"发布文章"）
     */
    private String operation;

    /**
     * 请求方法（如 ArticleController.save）
     */
    private String method;

    /**
     * 请求参数 JSON
     */
    private String params;

    /**
     * 请求 IP
     */
    private String ip;

    /**
     * 耗时（毫秒）
     */
    private Integer duration;

    /**
     * 状态（0=失败, 1=成功）
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
