package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.CommentRecord;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.PostInteractionService;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductInteractionController {

    @Autowired
    private PostInteractionService postInteractionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<?>> toggleLike(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = currentUser(authHeader).getId();
            Map<String, Object> result = postInteractionService.toggleLike(userId, id);
            return ResponseEntity.ok(ApiResponse.success("操作成功", result));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<?>> stats(@PathVariable Long id, @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            Long userId = null;
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                currentUser(authHeader);
                userId = currentUser(authHeader).getId();
            }
            return ResponseEntity.ok(ApiResponse.success("获取成功", postInteractionService.getStats(userId, id)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/{id}/comments")
    public ResponseEntity<ApiResponse<?>> comments(@PathVariable Long id, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("获取成功", postInteractionService.listComments(id, page, size)));
    }

    @PostMapping("/{id}/comment")
    public ResponseEntity<ApiResponse<?>> addComment(@PathVariable Long id, @RequestHeader("Authorization") String authHeader, @RequestBody Map<String, Object> body) {
        try {
            Long userId = currentUser(authHeader).getId();
            String content = String.valueOf(body.getOrDefault("content", "")).trim();
            if (content.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "评论内容不能为空"));
            }
            Long parentId = body.get("parentId") == null ? null : Long.valueOf(String.valueOf(body.get("parentId")));
            CommentRecord comment = postInteractionService.addComment(userId, id, parentId, content);
            return ResponseEntity.ok(ApiResponse.success("评论成功", comment));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }

    @PostMapping("/{id}/share")
    public ResponseEntity<ApiResponse<?>> share(@PathVariable Long id, @RequestHeader("Authorization") String authHeader) {
        try {
            Long userId = currentUser(authHeader).getId();
            return ResponseEntity.ok(ApiResponse.success("分享成功", postInteractionService.share(userId, id)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }

    private User currentUser(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtUtil.getUsernameFromToken(token);
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }
        return userOpt.get();
    }
}
