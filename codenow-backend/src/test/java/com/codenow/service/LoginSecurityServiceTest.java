package com.codenow.service;

import com.codenow.dto.CaptchaVO;
import com.codenow.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginSecurityServiceTest {
    private StringRedisTemplate redis;
    private ValueOperations<String, String> values;
    private LoginSecurityService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        service = new LoginSecurityService(redis);
    }

    @Test
    void createsSvgCaptchaAndStoresAnswer() {
        CaptchaVO captcha = service.createCaptcha();
        assertNotNull(captcha.getCaptchaId());
        assertTrue(captcha.getImage().startsWith("data:image/svg+xml;base64,"));
        verify(values).set(startsWith("auth:captcha:"), matches("\\d+"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void captchaCanOnlyBeVerifiedOnce() {
        when(values.get("auth:captcha:test")).thenReturn("7");
        service.verifyCaptcha("test", "7");
        verify(redis).delete("auth:captcha:test");

        when(values.get("auth:captcha:test")).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.verifyCaptcha("test", "7"));
    }

    @Test
    void locksAccountAfterFiveFailuresAndClearsOnSuccess() {
        when(values.get("auth:login:fail:user@example.com")).thenReturn("5");
        assertTrue(service.isLocked("User@example.com"));
        service.clearFailures("User@example.com");
        verify(redis).delete("auth:login:fail:user@example.com");
    }
}
