package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.service.ReviewService;
import com.example.campussecondhand.service.PostService;
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

    @Autowired
    private PostService postService;

    /**
     * 大厅左侧用户名片：昵称 / 头像 / 认证 / 年级 / 三条统计，一次接口返回。
     *
     * <p>nickname 字段：users 表当前没有该列，按裁决以 username 兜底
     * （「昵称可编辑」已记入答辩后 TODO）。</p>
     *
     * <p>grade：从学号解析入学年份，如 {@code 202012345678 → "2020级"}。
     * 学号为空或格式不匹配时返回 {@code null}，前端据此隐藏年级区域，
     * 绝不用「未知年级」之类的占位文案冒充真实数据。</p>
     */
    @GetMapping("/hall-profile")
    public ResponseEntity<ApiResponse<?>> hallProfile(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Optional<User> userOpt = getUserFromToken(authHeader);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
            }
            User user = userOpt.get();

            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("nickname", user.getUsername());
            profile.put("avatar", user.getAvatar());
            profile.put("verified", Integer.valueOf(1).equals(user.getIsStudentVerified()));
            profile.put("grade", parseGrade(user.getStudentNo()));
            profile.put("stats", postService.countStats(user.getId()));

            return ResponseEntity.ok(ApiResponse.success("获取成功", profile));
        } catch (Exception e) {
            log.error("获取大厅名片失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "获取名片失败"));
        }
    }

    /**
     * 从学号解析年级标签。
     *
     * <p>规则：取前 4 位，必须是 4 位纯数字且落在 1980~2100 之间，
     * 才认定为入学年份并拼出「{年份}级」。任一条件不满足返回 null。</p>
     */
    private static String parseGrade(String studentNo) {
        if (studentNo == null || studentNo.trim().length() < 4) {
            return null;
        }
        String prefix = studentNo.trim().substring(0, 4);
        if (!prefix.matches("\\d{4}")) {
            return null;
        }
        int year = Integer.parseInt(prefix);
        if (year < 1980 || year > 2100) {
            return null;
        }
        return year + "级";
    }

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
            // 学号与真实姓名在 User 上是 WRITE_ONLY，序列化时不会自动带出。
            // 本接口是「本人查本人」，鉴权已保证不会越权，故显式补回这两个字段——
            // 否则前端 Profile 的认证信息区永远显示「未认证」。
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("id", user.getId());
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("phone", user.getPhone() != null ? user.getPhone() : "");
            profile.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            profile.put("bio", user.getBio() != null ? user.getBio() : "");
            profile.put("location", user.getLocation() != null ? user.getLocation() : "");
            profile.put("qq", user.getQq() != null ? user.getQq() : "");
            profile.put("wechat", user.getWechat() != null ? user.getWechat() : "");
            profile.put("studentNo", user.getStudentNo() != null ? user.getStudentNo() : "");
            profile.put("realName", user.getRealName() != null ? user.getRealName() : "");
            return ResponseEntity.ok(ApiResponse.success("获取成功", profile));
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
