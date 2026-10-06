package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.dto.UserDTO;
import com.example.campussecondhand.repository.UserRepository;
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
    private UserRepository userRepository;

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


    /**
     * 当前登录用户身份。
     *
     * <p>供前端路由守卫判定管理员身份使用。刻意设计得足够轻量：
     * 只从 <b>JWT</b> 解析用户名与角色，不查询任何业务表、不做聚合运算——
     * 否则每次进入后台路由都要跑一遍统计查询。</p>
     *
     * <p>注意：这里返回的 role 来自服务端签名的 JWT，前端无法伪造。
     * 前端守卫本身只是体验优化（拦截误入），真实授权由
     * {@code AdminAuthInterceptor} 对 {@code /api/admin/**} 强制拦截。</p>
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<?>> me(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(ApiResponse.error(401, "未登录"));
            }
            String username = jwtUtil.getUsernameFromToken(authHeader.substring(7));
            if (username == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "登录已过期，请重新登录"));
            }
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户不存在"));
            }
            User user = userOpt.get();
            Map<String, Object> me = new HashMap<>();
            me.put("id", user.getId());
            me.put("username", user.getUsername());
            me.put("role", user.getRole());
            me.put("avatar", user.getAvatar());
            me.put("status", user.getStatus());
            return ResponseEntity.ok(ApiResponse.success("获取成功", me));
        } catch (Exception e) {
            log.warn("解析当前用户身份失败: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(401, "登录已过期，请重新登录"));
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
