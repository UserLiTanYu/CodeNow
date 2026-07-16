package com.codenow.service.impl;

import com.codenow.exception.BusinessException;
import com.codenow.service.EmailCodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailCodeServiceImpl implements EmailCodeService {
    private static final Duration CODE_TTL = Duration.ofMinutes(10);
    private static final Duration SEND_COOLDOWN = Duration.ofSeconds(60);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.from:}")
    private String mailFrom;

    @Override
    public void sendRegisterCode(String email) {
        sendCode("register", email, "码上记注册验证码");
    }

    @Override
    public void sendResetCode(String email) {
        sendCode("reset", email, "码上记密码重置验证码");
    }

    @Override
    public void verifyRegisterCode(String email, String code) {
        verifyCode("register", email, code);
    }

    @Override
    public void verifyResetCode(String email, String code) {
        verifyCode("reset", email, code);
    }

    private void sendCode(String scene, String email, String subject) {
        String normalizedEmail = normalize(email);
        String cooldownKey = cooldownKey(scene, normalizedEmail);
        Boolean firstSend = redisTemplate.opsForValue().setIfAbsent(cooldownKey, "1", SEND_COOLDOWN);
        if (!Boolean.TRUE.equals(firstSend)) {
            throw new BusinessException(429, "验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisTemplate.opsForValue().set(codeKey(scene, normalizedEmail), code, CODE_TTL);
        try {
            if (mailEnabled) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(mailFrom);
                message.setTo(normalizedEmail);
                message.setSubject(subject);
                message.setText("您的验证码是：" + code + "，10 分钟内有效。若非本人操作，请忽略此邮件。");
                mailSender.send(message);
            } else {
                log.info("本地开发邮件未启用，{} 验证码：{}", normalizedEmail, code);
            }
        } catch (RuntimeException e) {
            redisTemplate.delete(codeKey(scene, normalizedEmail));
            redisTemplate.delete(cooldownKey);
            throw new BusinessException(500, "验证码邮件发送失败，请稍后重试");
        }
    }

    private void verifyCode(String scene, String email, String code) {
        String key = codeKey(scene, normalize(email));
        String storedCode = redisTemplate.opsForValue().get(key);
        if (storedCode == null || !storedCode.equals(code)) {
            throw new BusinessException(400, "验证码错误或已过期");
        }
        redisTemplate.delete(key);
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }

    private String codeKey(String scene, String email) {
        return "auth:" + scene + ":code:" + email;
    }

    private String cooldownKey(String scene, String email) {
        return "auth:" + scene + ":cooldown:" + email;
    }
}
