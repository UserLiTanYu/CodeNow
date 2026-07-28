package com.codenow.exception;

import lombok.Getter;

/**
 * 接口限流异常
 */
/**
 * 接口限流异常，当请求频率超过限制时抛出。
 * HTTP 状态码固定为 429（Too Many Requests）。
 */
@Getter
public class RateLimitException extends RuntimeException {

    /**
     * 错误码，固定为 429
     */
    private final int code;

    /**
     * 构造限流异常
     *
     * @param message 异常消息，通常为"请求过于频繁"等提示信息
     */
    public RateLimitException(String message) {
        super(message);
        this.code = 429;
    }
}
