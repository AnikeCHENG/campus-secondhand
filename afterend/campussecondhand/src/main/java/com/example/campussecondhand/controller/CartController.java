package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.CartService;
import com.example.campussecondhand.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 购物车。
 *
 * <p>二手孤品无数量概念，故只有 add / remove，没有 updateQuantity。</p>
 */
@RestController
@RequestMapping("/api/cart")
@CrossOrigin(origins = "*")
public class CartController {

    private static final Logger log = LoggerFactory.getLogger(CartController.class);

    @Autowired
    private CartService cartService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtUtil jwtUtil;

    /** 加入购物车。Body: {@code {productId}} */
    @PostMapping("/add")
    public ResponseEntity<ApiResponse<?>> add(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        try {
            cartService.add(userOpt.get().getId(), parseLong(body.get("productId")));
            return ResponseEntity.ok(ApiResponse.success("已加入购物车", Map.of("count", cartService.count(userOpt.get().getId()))));
        } catch (BadRequestException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("加入购物车失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "加入购物车失败，请稍后重试"));
        }
    }

    /** 购物车列表：返回 { items: [...] } */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<?>> list(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("items", cartService.listItems(userOpt.get().getId()));
        return ResponseEntity.ok(ApiResponse.success("获取成功", data));
    }

    /** 购物车件数：返回 { count } */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<?>> count(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success("获取成功", Map.of("count", 0)));
        }
        return ResponseEntity.ok(ApiResponse.success("获取成功",
                Map.of("count", cartService.count(userOpt.get().getId()))));
    }

    /** 移出购物车 */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<?>> remove(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        cartService.remove(userOpt.get().getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("已移出购物车", null));
    }

    /**
     * 结算。Body: {@code {productIds:[1,2,3]}}
     *
     * <p>返回 {@code {orderIds, totalAmount, skipped:[{productId,reason}]}}，
     * 部分商品下单失败不影响其余。</p>
     */
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<?>> checkout(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> body) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录"));
        }
        try {
            List<Long> productIds = new ArrayList<>();
            Object raw = body.get("productIds");
            if (raw instanceof List<?> list) {
                for (Object item : list) {
                    if (item != null) {
                        productIds.add(parseLong(item));
                    }
                }
            }
            Map<String, Object> result = cartService.checkout(userOpt.get().getId(), productIds);
            return ResponseEntity.ok(ApiResponse.success("结算成功", result));
        } catch (BadRequestException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("结算失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "结算失败，请稍后重试"));
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
            throw new BadRequestException("商品 ID 格式不正确");
        }
    }
}