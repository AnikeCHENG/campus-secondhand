package com.example.campussecondhand.controller;

import com.example.campussecondhand.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本地开发调试接口。
 * 仅在 SPRING_PROFILES_ACTIVE=dev 时注册，生产部署不激活 dev profile 即自动消失。
 * 收件人硬编码，避免被当作邮件炸弹滥用。
 */
@RestController
@RequestMapping("/test")
@Profile("dev")
public class DevTestController {

    @Autowired
    private EmailService emailService;

    /** 发给发件账号自身，方便自测 */
    private static final String TEST_EMAIL = "zfc1514787427@163.com";

    @GetMapping("/mail")
    public String testMail() {
        try {
            emailService.sendCode(TEST_EMAIL);
            return "已发送至 " + TEST_EMAIL + "，记得看垃圾箱";
        } catch (RuntimeException e) {
            // 直接回显真实原因，避免被全局异常处理器统一包装成乱码 500
            return "发送失败：" + e.getMessage();
        }
    }
}
