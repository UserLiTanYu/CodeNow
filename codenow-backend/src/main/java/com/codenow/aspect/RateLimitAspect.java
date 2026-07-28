package com.codenow.aspect;

import com.codenow.annotation.RateLimit;
import com.codenow.common.IpUtils;
import com.codenow.exception.RateLimitException;
import jakarta.annotation.PostConstruct;
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

/**
 * Redis Lua 滑动窗口限流切面。默认按“请求 URI + 连接来源 IP”分桶，不信任客户端转发头；
 * Lua 原子完成窗口计数，Redis 故障时采取 fail-open，以业务可用性优先。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    /** Redis 字符串操作模板，用于执行 Lua 限流脚本 */
    private final StringRedisTemplate stringRedisTemplate;

    /** 限流 Lua 脚本对象，在初始化时从 classpath 加载 */
    private DefaultRedisScript<Long> rateLimitScript;

    /**
     * 初始化方法，Bean 创建后自动调用。从 classpath 加载限流 Lua 脚本，
     * 设置脚本返回类型为 Long（1=放行，0=拒绝）。
     */
    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("scripts/rate_limit.lua")));
        rateLimitScript.setResultType(Long.class);
    }

    /**
     * 前置通知：在标注了 {@link RateLimit} 注解的方法执行前进行限流检查。
     * 根据请求 URI 和客户端 IP 构建 Redis Key，通过 Lua 脚本原子地完成
     * 滑动窗口内的请求计数与阈值判断。超出限制时抛出 {@link RateLimitException}。
     * Redis 不可用时采取 fail-open 策略，降级放行以保证业务可用性。
     *
     * @param joinPoint 连接点，包含被拦截方法的信息
     * @throws RateLimitException 当请求频率超过阈值时抛出
     */
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

    /**
     * 构建限流 Key。优先使用注解中自定义的 key，未配置时依次降级为
     * 请求 URI、类名.方法名。
     *
     * @param rateLimit 限流注解实例
     * @param method    被拦截的方法对象
     * @return 限流 Key 字符串
     */
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

    /**
     * 获取客户端真实 IP 地址。限流场景下直接使用 getRemoteAddr()，
     * 不信任客户端伪造的 X-Forwarded-For 头，防止绕过限流。
     *
     * @return 客户端 IP 地址，获取失败时返回 "unknown"
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "unknown";
            // 限流场景：直接使用 getRemoteAddr()，不信任客户端伪造的 X-Forwarded-For
            return IpUtils.getRealIp(attributes.getRequest());
        } catch (Exception e) {
            return "unknown";
        }
    }
}
