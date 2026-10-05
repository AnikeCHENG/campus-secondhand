package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private Optional<User> getUserFromToken(String token) {
        try {
            if (token == null || !token.startsWith("Bearer ")) {
                return Optional.empty();
            }
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            log.error("获取用户信息失败: ", e);
            return Optional.empty();
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<?>> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            Optional<User> userOpt = getUserFromToken(authHeader);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
            }

            User user = userOpt.get();
            return ResponseEntity.ok(ApiResponse.success("获取成功", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                    "bio", user.getBio() != null ? user.getBio() : "",
                    "location", user.getLocation() != null ? user.getLocation() : "",
                    "qq", user.getQq() != null ? user.getQq() : "",
                    "wechat", user.getWechat() != null ? user.getWechat() : ""
            )));
        } catch (Exception e) {
            log.error("获取用户资料失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "获取资料失败"));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<?>> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> profileData) {
        try {
            Optional<User> userOpt = getUserFromToken(authHeader);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
            }

            User user = userOpt.get();

            if (profileData.containsKey("username")) {
                String username = profileData.get("username").trim();
                if (username.isEmpty()) {
                    return ResponseEntity.ok(ApiResponse.error(400, "用户名不能为空"));
                }
                if (username.length() < 2 || username.length() > 20) {
                    return ResponseEntity.ok(ApiResponse.error(400, "用户名需要在2-20个字符之间"));
                }
                user.setUsername(username);
            }

            if (profileData.containsKey("avatar")) {
                user.setAvatar(profileData.get("avatar"));
            }

            if (profileData.containsKey("bio")) {
                String bio = profileData.get("bio");
                if (bio != null && bio.length() > 255) {
                    return ResponseEntity.ok(ApiResponse.error(400, "个人简介不能超过255个字符"));
                }
                user.setBio(bio);
            }

            if (profileData.containsKey("location")) {
                String location = profileData.get("location");
                if (location != null && location.length() > 100) {
                    return ResponseEntity.ok(ApiResponse.error(400, "所在位置不能超过100个字符"));
                }
                user.setLocation(location);
            }

            if (profileData.containsKey("qq")) {
                String qq = profileData.get("qq");
                if (qq != null && qq.length() > 20) {
                    return ResponseEntity.ok(ApiResponse.error(400, "QQ号不能超过20个字符"));
                }
                user.setQq(qq);
            }

            if (profileData.containsKey("wechat")) {
                String wechat = profileData.get("wechat");
                if (wechat != null && wechat.length() > 50) {
                    return ResponseEntity.ok(ApiResponse.error(400, "微信号不能超过50个字符"));
                }
                user.setWechat(wechat);
            }

            userRepository.updateById(user);

            return ResponseEntity.ok(ApiResponse.success("保存成功", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                    "bio", user.getBio() != null ? user.getBio() : "",
                    "location", user.getLocation() != null ? user.getLocation() : "",
                    "qq", user.getQq() != null ? user.getQq() : "",
                    "wechat", user.getWechat() != null ? user.getWechat() : ""
            )));
        } catch (Exception e) {
            log.error("更新用户资料失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "保存失败"));
        }
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<ApiResponse<?>> getUserById(@PathVariable Long id) {
        try {
            User user = userRepository.selectById(id);
            if (user == null) {
                return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
            }

            return ResponseEntity.ok(ApiResponse.success("获取成功", Map.of(
                    "id", user.getId(),
                    "username", user.getUsername(),
                    "email", user.getEmail(),
                    "phone", user.getPhone() != null ? user.getPhone() : "",
                    "avatar", user.getAvatar() != null ? user.getAvatar() : "",
                    "bio", user.getBio() != null ? user.getBio() : "",
                    "location", user.getLocation() != null ? user.getLocation() : ""
            )));
        } catch (Exception e) {
            log.error("获取用户信息失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "获取失败"));
        }
    }
}
