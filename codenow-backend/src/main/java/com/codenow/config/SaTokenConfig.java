package com.codenow.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 使用带 HMAC 签名的 JWT 作为 Token，避免 UUID Token 无法校验签名。
     */
    @Bean
    public StpLogic stpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",
                        "/api/auth/captcha",
                        "/api/auth/register",
                        "/api/auth/register/code",
                        "/api/auth/password/code",
                        "/api/auth/password/reset",
                        "/api/blog/**",
                        "/api/comments/article/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**"
                );

        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkRole("ADMIN")))
                .addPathPatterns(
                        "/api/articles/**",
                        "/api/categories/**",
                        "/api/tags/**",
                        "/api/upload/**",
                        "/api/logs/**",
                        "/api/admin/**"
                );
    }
}
