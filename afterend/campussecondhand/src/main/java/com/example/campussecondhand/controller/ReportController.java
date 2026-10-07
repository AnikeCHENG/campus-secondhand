package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.Report;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.ReportService;
import com.example.campussecondhand.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * C 端举报提交。
 *
 * <p>与管理端的查询/处理接口分开放在两个类：二者的鉴权方式不同
 * （本类需要登录，管理端需要管理员角色），混在一起会让
 * 「哪些端点需要什么权限」变得难以一眼看清。</p>
 */
@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    @Autowired
    private ReportService reportService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 提交举报。
     *
     * <p>Body: {@code { targetId, targetType, reason }}，
     * targetType 取 PRODUCT 或 USER。</p>
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> submit(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        try {
            Optional<User> userOpt = getUserFromToken(authHeader);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
            }

            String targetType = body.get("targetType") == null ? null : String.valueOf(body.get("targetType"));
            Long targetId = parseLong(body.get("targetId"));
            String reason = body.get("reason") == null ? null : String.valueOf(body.get("reason"));

            Report report = reportService.submit(userOpt.get().getId(), targetType, targetId, reason);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", report.getId());
            result.put("targetType", report.getTargetType());
            result.put("targetId", report.getTargetId());
            return ResponseEntity.ok(ApiResponse.success("举报已提交，等待处理", result));
        } catch (BadRequestException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("提交举报失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "举报提交失败，请稍后重试"));
        }
    }

    private Optional<User> getUserFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Optional.empty();
            }
            return userRepository.findByUsername(jwtUtil.getUsernameFromToken(authHeader.substring(7)));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 前端可能传字符串或数字，两种都要接住 */
    private Long parseLong(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(raw).trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("举报对象 ID 格式不正确");
        }
    }
}