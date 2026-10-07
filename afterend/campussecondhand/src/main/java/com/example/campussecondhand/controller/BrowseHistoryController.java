package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.BrowseHistoryService;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 浏览历史。
 *
 * <p>历史的写入发生在商品详情接口内（见 {@code ProductController#detail}），
 * 本 Controller 只负责读取与清空。</p>
 */
@RestController
@RequestMapping("/api/history")
@CrossOrigin(origins = "*")
public class BrowseHistoryController {

    @Autowired
    private BrowseHistoryService browseHistoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private Optional<User> getUserFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Optional.empty();
            }
            String token = authHeader.substring(7);
            String username = jwtUtil.getUsernameFromToken(token);
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /** 浏览历史：按最近浏览时间倒序，物理分页（写入时已裁剪至最多 50 条） */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<?>> list(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        PageParam paging = PageParam.of(page, size);
        IPage<Map<String, Object>> result =
                browseHistoryService.pageHistory(userOpt.get().getId(), paging);
        return ResponseEntity.ok(ApiResponse.success("获取成功", PageResult.of(result)));
    }

    /** 清空我的浏览历史 */
    @DeleteMapping
    public ResponseEntity<ApiResponse<?>> clear(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        int deleted = browseHistoryService.clearHistory(userOpt.get().getId());
        return ResponseEntity.ok(ApiResponse.success("已清空浏览记录", deleted));
    }
}
