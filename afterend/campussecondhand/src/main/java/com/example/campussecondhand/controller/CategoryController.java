package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.Category;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.CategoryRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

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

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAllCategories() {
        List<Category> categories = categoryRepository.selectList(null);
        return ResponseEntity.ok(ApiResponse.success("获取成功", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getCategoryById(@PathVariable Long id) {
        Category category = categoryRepository.selectById(id);
        if (category != null) {
            return ResponseEntity.ok(ApiResponse.success("获取成功", category));
        }
        return ResponseEntity.ok(ApiResponse.error(404, "分类不存在"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> createCategory(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Category category) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        categoryRepository.insert(category);
        return ResponseEntity.ok(ApiResponse.success("创建成功", category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id, 
            @RequestBody Category category) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        category.setId(id);
        categoryRepository.updateById(category);
        return ResponseEntity.ok(ApiResponse.success("更新成功", category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    @GetMapping("/parent/{parentId}")
    public ResponseEntity<ApiResponse<?>> getCategoriesByParentId(@PathVariable Long parentId) {
        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        wrapper.eq("parent_id", parentId);
        List<Category> categories = categoryRepository.selectList(wrapper);
        return ResponseEntity.ok(ApiResponse.success("获取成功", categories));
    }
}