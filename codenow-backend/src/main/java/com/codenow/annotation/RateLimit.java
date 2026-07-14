package com.codenow.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口限流注解，基于 Redis 滑动窗口算法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流 Key 前缀（默认使用请求路径）
     */
    String key() default "";

    /**
     * 时间窗口内的最大请求数
     */
    int maxCount() default 10;

    /**
     * 时间窗口（秒）
     */
    int timeWindow() default 1;

    /**
     * 触发限流时的提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
