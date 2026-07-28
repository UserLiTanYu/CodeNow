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
     *   <li>作者角色拦截器：对 /api/author/** 生效，仅允许 AUTHOR 角色</li>
     *   <li>管理员角色拦截器：对后台管理路径生效，要求 ADMIN 角色</li>
     * </ul>
     *
     * @param registry 拦截器注册器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 第一组：登录拦截器 —— 所有 /api/** 接口默认要求登录，以下路径排除在外
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/login",            // 登录接口
                        "/api/auth/captcha",          // 获取验证码
                        "/api/auth/register",         // 用户注册
                        "/api/auth/register/code",    // 注册验证码
                        "/api/auth/password/code",    // 重置密码验证码
                        "/api/auth/password/reset",   // 密码重置
                        "/api/blog/**",               // 博客前台公开接口（文章列表、详情等）
                        "/api/health",                // 健康检查
                        "/api/comments/article/**",   // 文章评论（公开读取）
                        "/doc.html",                  // Knife4j 接口文档
                        "/webjars/**",                // Knife4j 静态资源
                        "/v3/api-docs/**",            // OpenAPI 文档
                        "/swagger-ui/**",             // Swagger UI
                        "/swagger-resources/**"       // Swagger 资源
                );

        // 第二组：作者角色拦截器 —— 作者工作台只管理当前作者自己的内容
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkRole("AUTHOR")))
                .addPathPatterns("/api/author/**");

        // 第三组：管理员角色拦截器 —— 后台管理接口，仅允许 ADMIN 角色访问
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkRole("ADMIN")))
                .addPathPatterns(
                        "/api/articles/**",   // 文章管理
                        "/api/categories/**", // 分类管理
                        "/api/tags/**",       // 标签管理
                        "/api/upload/**",     // 文件上传
                        "/api/logs/**",       // 日志查看
                        "/api/admin/**"       // 其他后台管理功能
                );
    }
}
