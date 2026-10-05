package com.example.campussecondhand.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private final StringRedisTemplate redisTemplate;
    private final JavaMailSender mailSender;
    private final String from;
    private final SecureRandom random = new SecureRandom();

    public EmailService(StringRedisTemplate redisTemplate, JavaMailSender mailSender,
                        @Value("${spring.mail.username}") String from) {
        this.redisTemplate = redisTemplate;
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendCode(String email) {
        String lastKey = "email:code:last:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lastKey))) {
            throw new RuntimeException("60秒内只能发送一次");
        }

        String dailyKey = "email:code:daily:" + email + ":" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        Long count = redisTemplate.opsForValue().increment(dailyKey);
        if (count != null && count == 1) {
            redisTemplate.expire(dailyKey, Duration.ofDays(1));
        }
        if (count != null && count > 10) {
            throw new RuntimeException("今日发送次数已达上限");
        }

        int code = 100000 + random.nextInt(900000);
        String codeStr = String.valueOf(code);
        redisTemplate.opsForValue().set("email:code:" + email, codeStr, Duration.ofMinutes(5));
        redisTemplate.opsForValue().set(lastKey, "1", Duration.ofSeconds(60));
        redisTemplate.delete("email:code:attempts:" + email);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(email);
        message.setSubject("校园二手注册验证码");
        message.setText("你的验证码是：" + codeStr + "，5分钟内有效。");
        mailSender.send(message);
    }

    public void verify(String email, String code) {
        String stored = redisTemplate.opsForValue().get("email:code:" + email);
        if (stored == null) {
            throw new RuntimeException("验证码不存在或已过期");
        }

        String attemptsKey = "email:code:attempts:" + email;
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr == null ? 0 : Integer.parseInt(attemptsStr);

        if (!stored.equals(code)) {
            attempts++;
            if (attempts > 5) {
                redisTemplate.delete("email:code:" + email);
                redisTemplate.delete(attemptsKey);
            } else {
                redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts), Duration.ofMinutes(5));
            }
            throw new RuntimeException("验证码错误");
        }

        redisTemplate.delete("email:code:" + email);
        redisTemplate.delete(attemptsKey);
    }
}
