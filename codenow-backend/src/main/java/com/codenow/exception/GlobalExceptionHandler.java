package com.codenow.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.exception.NotRoleException;
import com.codenow.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * REST 全局异常出口。业务异常保留可公开消息，参数和系统异常转换为稳定响应，避免泄露堆栈细节。
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常，将业务错误码和消息返回给客户端
     *
     * @param e 业务异常
     * @return 包含错误信息的响应实体
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<R<Void>> handleBusinessException(BusinessException e) {
        return response(e.getCode(), e.getMessage());
    }

    /**
     * 处理接口限流异常，返回 429 状态码
     *
     * @param e 限流异常
     * @return 包含错误信息的响应实体
     */
    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<R<Void>> handleRateLimitException(RateLimitException e) {
        return response(e.getCode(), e.getMessage());
    }

    /**
     * 处理未登录异常，返回 401 状态码
     *
     * @param e 未登录异常
     * @return 包含登录提示信息的响应实体
     */
    @ExceptionHandler(NotLoginException.class)
    public ResponseEntity<R<Void>> handleNotLoginException(NotLoginException e) {
        return response(401, "登录已失效，请重新登录");
    }

    /**
     * 处理权限不足异常，返回 403 状态码
     *
     * @param e 权限不足异常
     * @return 包含权限提示信息的响应实体
     */
    @ExceptionHandler(NotPermissionException.class)
    public ResponseEntity<R<Void>> handleNotPermissionException(NotPermissionException e) {
        return response(403, "没有权限执行此操作");
    }

    /**
     * 处理角色不足异常，返回 403 状态码
     *
     * @param e 角色不足异常
     * @return 包含权限提示信息的响应实体
     */
    @ExceptionHandler(NotRoleException.class)
    public ResponseEntity<R<Void>> handleNotRoleException(NotRoleException e) {
        return response(403, "没有权限执行此操作");
    }

    /**
     * 处理参数校验异常，提取第一条校验错误消息返回给客户端
     *
     * @param e 参数校验异常
     * @return 包含校验错误信息的响应实体
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return response(400, message);
    }

    /**
     * 处理请求参数错误异常（缺少参数或参数类型不匹配），返回 400 状态码
     *
     * @param e 请求参数异常
     * @return 包含参数错误提示信息的响应实体
     */
    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<R<Void>> handleBadRequest(Exception e) {
        return response(400, "请求参数格式错误");
    }

    /**
     * 处理资源不存在异常，返回 404 状态码
     *
     * @param e 资源不存在异常
     * @return 包含资源不存在提示信息的响应实体
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<R<Void>> handleNotFound(NoResourceFoundException e) {
        return response(404, "请求资源不存在");
    }

    /**
     * 处理请求方法不支持异常，返回 405 状态码
     *
     * @param e 请求方法不支持异常
     * @return 包含方法不支持提示信息的响应实体
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Void>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return response(405, "请求方法不支持");
    }

    /**
     * 处理文件上传大小超限异常，返回 413 状态码
     *
     * @param e 文件大小超限异常
     * @return 包含文件大小限制提示信息的响应实体
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<R<Void>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        return response(413, "上传文件不能超过 25MB");
    }

    /**
     * 兜底异常处理，捕获所有未处理的异常。记录完整堆栈到日志，返回 500 状态码。
     *
     * @param e 未知异常
     * @return 包含通用错误提示信息的响应实体
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Void>> handleException(Exception e) {
        // 记录完整堆栈到日志，但不暴露给客户端
        log.error("系统异常", e);
        return response(500, "系统繁忙，请稍后再试");
    }

    /**
     * 构建统一的错误响应实体
     *
     * @param code    HTTP 状态码
     * @param message 错误消息
     * @return 包含错误信息的响应实体
     */
    private ResponseEntity<R<Void>> response(int code, String message) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(R.error(code, message));
    }
}
