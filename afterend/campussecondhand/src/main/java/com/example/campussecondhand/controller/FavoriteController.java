package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.BrowseHistoryService;
import com.example.campussecondhand.service.FavoriteService;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 收藏与浏览历史。
 *
 * <p>所有收藏端点以 {@code productId} 为维度（而非收藏记录 id），
 * 前端无需先查列表即可直接取消收藏。集合操作保持幂等。</p>
 */
@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private BrowseHistoryService browseHistoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

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

    /** 我的收藏：含商品图/标题/价格/状态，按收藏时间倒序，物理分页 */
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
                favoriteService.pageFavorites(userOpt.get().getId(), paging);
        return ResponseEntity.ok(ApiResponse.success("获取成功", PageResult.of(result)));
    }

    /**
     * 是否已收藏，供商品详情页回显。
     *
     * <p>{@code Authorization} 必须是<b>可选</b>请求头：未登录时返回 false，
     * 而不是抛 MissingRequestHeader 变成 401——商品详情允许游客访问，
     * 收藏态回显属于附带能力，不该把游客挡在门外。</p>
     */
    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<?>> check(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long productId) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        boolean favorited = userOpt.isPresent()
                && favoriteService.isFavorited(userOpt.get().getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("获取成功", favorited));
    }

    /** 收藏商品；重复收藏返回原状态，不报错 */
    @PostMapping("/{productId}")
    public ResponseEntity<ApiResponse<?>> add(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "请先登录后再收藏"));
        }
        if (productRepository.selectById(productId) == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
        }
        boolean added = favoriteService.addFavorite(userOpt.get().getId(), productId);
        return ResponseEntity.ok(ApiResponse.success(
                added ? "收藏成功" : "已收藏", favoriteService.isFavorited(userOpt.get().getId(), productId)));
    }

    /** 取消收藏；未收藏也返回成功（幂等） */
    @DeleteMapping("/{productId}")
    public ResponseEntity<ApiResponse<?>> remove(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long productId) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        favoriteService.removeFavorite(userOpt.get().getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("已取消收藏", false));
    }
}
