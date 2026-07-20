package com.codenow.exception;

import lombok.Getter;

/**
 * 业务异常类，用于在业务逻辑中抛出可预期的异常。
 * 支持自定义错误码和错误消息，便于前端根据错误码做差异化处理。
 */
@Getter
public class BusinessException extends RuntimeException {

    /**
     * 错误码，默认为 500
     */
    private final Integer code;

    /**
     * 使用默认错误码 500 构造业务异常
     *
     * @param message 异常消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 使用自定义错误码构造业务异常
     *
     * @param code    错误码
     * @param message 异常消息
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
