package com.codenow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

/**
 * 跨域（CORS）配置类，根据配置文件中的允许来源列表设置跨域策略。
 * 支持通过 {@code cors.allowed-origins} 属性自定义允许的来源地址。
 */
@Configuration
public class CorsConfig {

    /**
     * 允许的跨域来源列表
     */
    private final List<String> allowedOrigins;

    /**
     * 构造方法，从配置属性中解析允许的跨域来源列表
     *
     * @param origins 逗号分隔的来源地址字符串
     */
    public CorsConfig(@Value("${cors.allowed-origins:http://localhost:5173,http://localhost}") String origins) {
        this.allowedOrigins = Arrays.stream(origins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    /**
     * 创建跨域过滤器 Bean，允许所有请求头和请求方法，支持携带凭证。
     *
     * @return 跨域过滤器实例
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
