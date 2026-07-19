package com.codenow.controller;

import com.codenow.common.R;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void healthReturnsUpStatusWithoutDatabase() {
        HealthController controller = new HealthController();
        R<Map<String, Object>> result = controller.health();

        assertEquals(200, result.getCode());
        assertEquals("UP", result.getData().get("status"));
        assertNotNull(result.getData().get("timestamp"));
    }

    @Test
    void healthReturnsSchemaVersionWithDatabase() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class)))
                .thenReturn("4 (article tag soft delete)");

        HealthController controller = new HealthController();
        controller.jdbcTemplate = jdbcTemplate;
        R<Map<String, Object>> result = controller.health();

        assertEquals(200, result.getCode());
        assertEquals("UP", result.getData().get("status"));
        assertEquals("connected", result.getData().get("database"));
        assertEquals("4 (article tag soft delete)", result.getData().get("dbSchemaVersion"));
    }

    @Test
    void healthHandlesDatabaseError() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class)))
                .thenThrow(new RuntimeException("connection refused"));

        HealthController controller = new HealthController();
        controller.jdbcTemplate = jdbcTemplate;
        R<Map<String, Object>> result = controller.health();

        assertEquals(200, result.getCode());
        assertEquals("UP", result.getData().get("status"));
        assertNotNull(result.getData().get("database"));
    }
}
