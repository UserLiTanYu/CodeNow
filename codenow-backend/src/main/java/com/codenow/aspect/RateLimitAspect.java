package com.codenow.aspect;

import com.codenow.annotation.RateLimit;
import com.codenow.exception.RateLimitException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collections;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate stringRedisTemplate;
    private DefaultRedisScript<Long> rateLimitScript;

    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua")));
        rateLimitScript.setResultType(Long.class);
    }

    @Before("@annotation(com.codenow.annotation.RateLimit)")
    public void before(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);

        // 构建限流 Key
        String key = buildKey(rateLimit, method);

        // 获取客户端 IP
        String ip = getClientIp();

        // 限流 Key: rate_limit:{key}:{ip}
        String redisKey = "rate_limit:" + key + ":" + ip;

        // 当前时间戳（毫秒）
        long now = System.currentTimeMillis();
        long windowStart = now - (long) rateLimit.timeWindow() * 1000;

        try {
            // 执行 Lua 脚本
            Long result = stringRedisTemplate.execute(
                    rateLimitScript,
                    Collections.singletonList(redisKey),
                    String.valueOf(windowStart),
                    String.valueOf(now),
                    String.valueOf(rateLimit.maxCount()),
                    String.valueOf(rateLimit.timeWindow())
            );

            if (result == null || result == 0) {
                throw new RateLimitException(rateLimit.message());
            }
        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            // Redis 不可用时降级放行，保证主业务不受影响
            log.warn("限流 Redis 操作失败，降级放行: {}", e.getMessage());
        }
    }

    private String buildKey(RateLimit rateLimit, Method method) {
        if (rateLimit.key() != null && !rateLimit.key().isEmpty()) {
            return rateLimit.key();
        }
        // 默认使用 请求路径
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest().getRequestURI();
            }
        } catch (Exception ignored) {
        }
        // fallback: 类名.方法名
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "unknown";
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
}
