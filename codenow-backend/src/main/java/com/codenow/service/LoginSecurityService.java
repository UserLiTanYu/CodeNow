package com.codenow.service;

import com.codenow.dto.CaptchaVO;
import com.codenow.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoginSecurityService {
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);
    private static final Duration FAILURE_TTL = Duration.ofMinutes(15);
    private static final int MAX_FAILURES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;

    public CaptchaVO createCaptcha() {
        int left = RANDOM.nextInt(9) + 1;
        int right = RANDOM.nextInt(9) + 1;
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(captchaKey(captchaId), String.valueOf(left + right), CAPTCHA_TTL);
        String text = left + " + " + right + " = ?";
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='150' height='44' viewBox='0 0 150 44'>"
                + "<rect width='150' height='44' rx='6' fill='#f4f7fb'/><path d='M4 35L146 9M8 8L140 38' stroke='#d7e3f1'/>"
                + "<text x='75' y='29' text-anchor='middle' font-family='Arial' font-size='21' font-weight='700' fill='#315f91'>"
                + text + "</text></svg>";
        String image = "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new CaptchaVO(captchaId, image);
    }

    public void verifyCaptcha(String captchaId, String answer) {
        String key = captchaKey(captchaId);
        String expected = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        if (expected == null || !expected.equals(answer == null ? "" : answer.trim())) {
            throw new BusinessException(400, "图形验证码错误或已过期");
        }
    }

    public boolean isLocked(String account) {
        String value = redisTemplate.opsForValue().get(failureKey(account));
        return value != null && Integer.parseInt(value) >= MAX_FAILURES;
    }

    public void recordFailure(String account) {
        String key = failureKey(account);
        Long failures = redisTemplate.opsForValue().increment(key);
        if (failures != null && failures == 1) redisTemplate.expire(key, FAILURE_TTL);
    }

    public void clearFailures(String account) {
        redisTemplate.delete(failureKey(account));
    }

    private String captchaKey(String id) {
        return "auth:captcha:" + id;
    }

    private String failureKey(String account) {
        return "auth:login:fail:" + account.trim().toLowerCase();
    }
}
