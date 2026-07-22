package com.codenow.controller;

import com.codenow.common.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查端点，供运维和 CI 验证服务状态与数据库 schema 版本。
 */
@Tag(name = "Health", description = "健康检查")
@RestController
public class HealthController {

    @Autowired(required = false)
    JdbcTemplate jdbcTemplate;

    /**
     * 健康检查。
     * 检查服务状态和数据库连接，返回当前 Flyway schema 版本。
     */
    @Operation(summary = "健康检查")
    @GetMapping("/api/health")
    public R<Map<String, Object>> health() {
        //构建健康检查响应数据
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now().toString());

        //如果数据库连接可用，查询Flyway schema版本信息
        if (jdbcTemplate != null) {
            try {
                //查询Flyway schema版本
                String version = jdbcTemplate.query(
                        "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1",
                        rs -> rs.next() ? rs.getString("version") + " (" + rs.getString("description") + ")" : "no migrations"
                );
                data.put("dbSchemaVersion", version);
                data.put("database", "connected");
            } catch (Exception e) {
                //数据库查询失败时记录错误信息
                data.put("database", "error: " + e.getMessage());
            }
        }

        //返回健康检查结果
        return R.ok(data);
    }
}
