package com.codenow.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void businessException_shouldUseDeclaredHttpStatus() {
        var response = handler.handleBusinessException(new BusinessException(404, "文章不存在"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getCode());
    }

    @Test
    void rateLimitException_shouldReturn429() {
        var response = handler.handleRateLimitException(new RateLimitException("请求过于频繁"));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
    }

    @Test
    void badRequest_shouldReturn400() {
        var exception = new MissingServletRequestParameterException("pageNum", "Integer");

        assertEquals(HttpStatus.BAD_REQUEST, handler.handleBadRequest(exception).getStatusCode());
    }

    @Test
    void methodNotAllowed_shouldReturn405() {
        var exception = new HttpRequestMethodNotSupportedException("TRACE");

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, handler.handleMethodNotAllowed(exception).getStatusCode());
    }

    @Test
    void notLogin_shouldReturn401() {
        var response = handler.handleNotLoginException(mock(NotLoginException.class));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("登录已失效，请重新登录", response.getBody().getMessage());
    }

    @Test
    void notPermission_shouldReturn403() {
        var response = handler.handleNotPermissionException(mock(NotPermissionException.class));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("没有权限执行此操作", response.getBody().getMessage());
    }

    @Test
    void unexpectedException_shouldHideInternalMessage() {
        var response = handler.handleException(new RuntimeException("sensitive database detail"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("系统繁忙，请稍后再试", response.getBody().getMessage());
    }

    @Test
    void oversizedUpload_shouldReturnFriendly413() {
        var response = handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(5 * 1024 * 1024));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("上传文件不能超过 25MB", response.getBody().getMessage());
    }
}
