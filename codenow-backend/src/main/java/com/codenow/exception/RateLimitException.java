package com.codenow.exception;

import lombok.Getter;

/**
 * 接口限流异常
 */
@Getter
public class RateLimitException extends RuntimeException {

    private final int code;

    public RateLimitException(String message) {
        super(message);
        this.code = 429;
    }
}
