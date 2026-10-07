package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.service.ReviewService;
import com.example.campussecondhand.service.StudentVerifyService;
import java.util.LinkedHashMap;
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

    @Autowired
    private StudentVerifyService studentVerifyService;

    @Autowired
    private ReviewService reviewService;

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

    /**
     * 学生认证。
     *
     * <p><b>当前为模拟校验，接口预留，未来可对接教务系统或改为人工审核。</b>
     * 校验规则见 {@link StudentVerifyService}。</p>
     */
    @PostMapping("/student-verify")
    public ResponseEntity<ApiResponse<?>> studentVerify(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            Optional<User> userOpt = getUserFromToken(authHeader);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
            }
            String studentNo = body.get("studentNo") == null ? null : String.valueOf(body.get("studentNo"));
            String realName = body.get("realName") == null ? null : String.valueOf(body.get("realName"));

            User user = studentVerifyService.verify(userOpt.get().getId(), studentNo, realName);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("isStudentVerified", true);
            result.put("studentNo", user.getStudentNo());
            // 真实姓名回显给本人，方便确认填错没有
            result.put("realName", user.getRealName());
            return ResponseEntity.ok(ApiResponse.success("认证成功，已享受学生专属免手续费", result));
        } catch (BadRequestException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("学生认证失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "认证失败，请稍后重试"));
        }
    }

    /**
     * 卖家的评价统计：好评率、平均星级、评价总数。
     *
     * <p>全部由 SQL 聚合实时计算，不存冗余字段——冗余的统计值需要处理
     * "评价新增/修改/删除时何时同步"的问题，而评价一经提交不可修改也不可删除，
     * 实时聚合的代价（一次索引扫描）远小于同步逻辑出错的风险。</p>
     */
    @GetMapping("/user/{id}/review-stats")
    public ResponseEntity<ApiResponse<?>> getReviewStats(@PathVariable Long id) {
        try {
            Map<String, Object> stats = reviewService.statsOf(id);
            return ResponseEntity.ok(ApiResponse.success("获取成功", stats));
        } catch (Exception e) {
            log.error("获取评价统计失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "获取失败"));
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
                    "location", user.getLocation() != null ? user.getLocation() : "",
                    // 认证徽章需要这个字段；真实姓名属敏感信息，不在公开接口返回
                    "isStudentVerified", user.isStudentVerifiedUser()
            )));
        } catch (Exception e) {
            log.error("获取用户信息失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "获取失败"));
        }
    }
}
