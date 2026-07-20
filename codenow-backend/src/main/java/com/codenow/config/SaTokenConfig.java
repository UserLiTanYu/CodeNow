package com.codenow.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 路径授权配置。登录、作者角色和管理员角色三组拦截器会叠加匹配；
 * 公开路径仅跳过第一组登录检查，不代表绕过其他可能命中的角色检查。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 使用带 HMAC 签名的 JWT 作为 Token，避免 UUID Token 无法校验签名。
     */
    @Bean
    public StpLogic stpLogicJwt() {
        return new StpLogicJwtForSimple();
    }

    /**
     * 注册 Sa-Token 拦截器，配置三组路径授权规则：
     * <ul>
     *   <li>登录拦截器：对 /api/** 生效，排除公开路径</li>
     *   <li>作者角色拦截器：对 /api/author/** 生效，要求 AUTHOR 或 ADMIN 角色</li>
     *   <li>管理员角色拦截器：对后台管理路径生效，要求 ADMIN 角色</li>
     * </ul>
     *
     * @param registry 拦截器注册器
     */
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
                        "/api/health",
                        "/api/comments/article/**",
                        "/doc.html",
                        "/webjars/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**"
                );

        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkRoleOr("AUTHOR", "ADMIN")))
                .addPathPatterns("/api/author/**");

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
