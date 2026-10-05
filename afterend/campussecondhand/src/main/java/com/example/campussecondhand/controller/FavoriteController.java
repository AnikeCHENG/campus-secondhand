package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.Favorite;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.FavoriteRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

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

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyFavorites(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userOpt.get().getId());
        List<Favorite> favorites = favoriteRepository.selectList(wrapper);
        return ResponseEntity.ok(ApiResponse.success("获取成功", favorites));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getFavoriteById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Favorite favorite = favoriteRepository.selectById(id);
        if (favorite == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "收藏不存在"));
        }
        if (!favorite.getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.ok(ApiResponse.error(403, "无权访问此收藏"));
        }
        return ResponseEntity.ok(ApiResponse.success("获取成功", favorite));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createFavorite(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Favorite favorite) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        favorite.setUserId(userOpt.get().getId());
        favoriteRepository.insert(favorite);
        return ResponseEntity.ok(ApiResponse.success("收藏成功", favorite));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteFavorite(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Favorite favorite = favoriteRepository.selectById(id);
        if (favorite == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "收藏不存在"));
        }
        if (!favorite.getUserId().equals(userOpt.get().getId())) {
            return ResponseEntity.ok(ApiResponse.error(403, "无权操作此收藏"));
        }
        favoriteRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("取消收藏成功", null));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<?>> checkFavorite(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        QueryWrapper<Favorite> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userOpt.get().getId()).eq("product_id", productId);
        Favorite favorite = favoriteRepository.selectOne(wrapper);
        return ResponseEntity.ok(ApiResponse.success("获取成功", favorite != null));
    }
}