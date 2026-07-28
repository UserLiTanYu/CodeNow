package com.codenow.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类。禁用默认的安全拦截，将认证职责完全交给 Sa-Token 处理。
 * 同时注册 BCrypt 密码编码器 Bean。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 创建 BCrypt 密码编码器 Bean，用于密码的加密和校验。
     *
     * @return BCrypt 密码编码器实例
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 创建安全过滤器链 Bean，禁用 CSRF 并允许所有请求通过。
     * 认证由 Sa-Token 拦截器负责，此处仅保留 Spring Security 框架的最小配置。
     *
     * @param http HttpSecurity 配置对象
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        // 禁用 Spring Security 的默认拦截，认证由 Sa-Token 处理
        http.csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
