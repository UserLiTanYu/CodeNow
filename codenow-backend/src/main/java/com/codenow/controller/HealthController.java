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

    @Operation(summary = "健康检查")
    @GetMapping("/api/health")
    public R<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", "UP");
        data.put("timestamp", LocalDateTime.now().toString());

        if (jdbcTemplate != null) {
            try {
                // 查询 Flyway schema 版本
                String version = jdbcTemplate.query(
                        "SELECT installed_rank, version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 1",
                        rs -> rs.next() ? rs.getString("version") + " (" + rs.getString("description") + ")" : "no migrations"
                );
                data.put("dbSchemaVersion", version);
                data.put("database", "connected");
            } catch (Exception e) {
                data.put("database", "error: " + e.getMessage());
            }
        }

        return R.ok(data);
    }
}
