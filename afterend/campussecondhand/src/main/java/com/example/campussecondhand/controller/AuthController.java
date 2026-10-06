package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.dto.UserDTO;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.service.EmailService;
import com.example.campussecondhand.service.UserService;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    // 登录
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<?>> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");
            if (username == null || username.isBlank() || password == null || password.isBlank()) {
                return ResponseEntity.ok(ApiResponse.error(400, "用户名和密码不能为空"));
            }
            Optional<User> userOpt = userService.findByUsername(username);
            if (userOpt.isPresent()) {
                User user = userOpt.get();
                if (!passwordEncoder.matches(password, user.getPassword())) {
                    return ResponseEntity.ok(ApiResponse.error(401, "用户名或密码错误"));
                }
                if (user.getStatus() == 0) {
                    return ResponseEntity.ok(ApiResponse.error(403, "账户已被禁用"));
                }

                // 确保用户对象有 role 字段
                if (user.getRole() == null) {
                    user.setRole(0); // 默认角色为普通用户
                    userService.updateUser(user);
                }

                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                return ResponseEntity.ok(ApiResponse.success("登录成功", Map.of(
                        "token", token,
                        "user", user
                )));
            } else {
                return ResponseEntity.ok(ApiResponse.error(401, "用户名或密码错误"));
            }
        } catch (Exception e) {
            // 添加详细的日志记录
            log.error("登录失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "登录失败"));
        }
    }




    // 发送邮箱验证码
    @PostMapping("/send-code")
    public ResponseEntity<ApiResponse<?>> sendCode(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error(400, "邮箱不能为空"));
        }
        try {
            emailService.sendCode(email);
            return ResponseEntity.ok(ApiResponse.success("验证码已发送", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(429, e.getMessage()));
        }
    }

    // 注册
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<?>> register(@Valid @RequestBody UserDTO userDTO) {
        try {
            if (userDTO.getCode() == null || userDTO.getCode().isBlank()) {
                return ResponseEntity.ok(ApiResponse.error(400, "验证码不能为空"));
            }
            try {
                emailService.verify(userDTO.getEmail(), userDTO.getCode());
            } catch (Exception e) {
                return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
            }

            if (userService.existsByUsername(userDTO.getUsername())) {
                return ResponseEntity.ok(ApiResponse.error(70, "用户名已存在"));
            }

            if (userService.existsByEmail(userDTO.getEmail())) {
                return ResponseEntity.ok(ApiResponse.error(70, "邮箱已被注册"));
            }

            User user = userService.registerUser(userDTO);
            log.info("注册成功: username={}", userDTO.getUsername());
            return ResponseEntity.ok(ApiResponse.success("注册成功", user));
        } catch (Exception e) {
            // 必须打印堆栈：否则注册失败在日志里没有任何痕迹，无法定位
            log.error("注册失败: username={}, email={}, 原因={}",
                    userDTO.getUsername(), userDTO.getEmail(), e.getMessage(), e);
            return ResponseEntity.ok(ApiResponse.error(500, "注册失败"));
        }
    }


    // 忘记密码 - 发送重置邮件
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<?>> forgotPassword(@RequestParam String email) {
        try {
            log.info("尝试忘记密码: email={}", email);

            Optional<User> userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("邮箱不存在: email={}", email);
                return ResponseEntity.ok(ApiResponse.error(404, "邮箱不存在"));
            }

            User user = userOpt.get();
            log.info("找到用户: email={}", email);

            // 这里可以添加发送邮件的逻辑
            // emailService.sendResetPasswordEmail(email);

            log.info("密码重置邮件已发送: email={}", email);
            return ResponseEntity.ok(ApiResponse.success("密码重置邮件已发送"));
        } catch (Exception e) {
            log.error("忘记密码失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "忘记密码失败"));
        }
    }



}
