package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import com.example.campussecondhand.entity.Post;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.PostType;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.repository.PostRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.PostService;
import com.example.campussecondhand.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 大厅动态接口。
 *
 * <p>分页沿用项目基线 {@link PageParam} + {@link PageResult}：
 * 请求参数 {@code page}/{@code size}，返回 {@code {list, total, page, size}}。</p>
 */
@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    @Autowired
    private PostService postService;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 发布动态。
     *
     * <p>未登录一律 401（而不是 200 + code=401）：这个接口会写库，
     * 让调用方能用 HTTP 状态码就判定成败。</p>
     */
    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {

        Optional<User> userOpt = resolveUser(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(401, "未登录或登录已过期"));
        }

        PostService.CreateCommand cmd = new PostService.CreateCommand(
                str(body.get("type")),
                str(body.get("content")),
                strList(body.get("tags")),
                strList(body.get("images")),
                productField(body, "title"),
                decimalField(body, "price"),
                productField(body, "description"),
                productField(body, "category"));

        Post post = postService.create(userOpt.get().getId(), cmd);
        postService.enrich(List.of(post));
        return ResponseEntity.ok(ApiResponse.success("发布成功", post));
    }

    /** 动态列表：type 可选，空表示全部 */
    @GetMapping
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        PageParam paging = PageParam.of(page, size);

        // 非法 type 不静默降级为「全部」：返回空列表，避免用户以为筛选生效了
        if (type != null && !type.isBlank() && PostType.fromCode(type) == null) {
            return ResponseEntity.ok(ApiResponse.success("获取成功",
                    new PageResult<>(List.of(), 0, paging.page(), paging.size())));
        }

        List<Post> all = postService.list(type, 1, Integer.MAX_VALUE);
        int total = all.size();
        int from = (int) Math.min(paging.offset(), total);
        int to = Math.min(from + paging.size(), total);
        List<Post> slice = from >= total ? List.of() : List.copyOf(all.subList(from, to));

        return ResponseEntity.ok(ApiResponse.success("获取成功",
                new PageResult<>(slice, total, paging.page(), paging.size())));
    }

    /** 类型字典，供前端渲染筛选项，避免前端硬编码枚举文案 */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<?>> types() {
        return ResponseEntity.ok(ApiResponse.success("获取成功",
                PostType.all().stream()
                        .map(t -> Map.of("value", t.name(), "label", t.getLabel()))
                        .toList()));
    }

    /* ---------------- 内部工具 ---------------- */

    private Optional<User> resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        try {
            String username = jwtUtil.getUsernameFromToken(authHeader.substring(7));
            if (username == null) {
                return Optional.empty();
            }
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    /** 宽松解析字符串数组：单值也接受，自动包成一元数组 */
    private static List<String> strList(Object v) {
        if (v == null) {
            return List.of();
        }
        if (v instanceof List<?> list) {
            return list.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(String::valueOf)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        String single = String.valueOf(v).trim();
        return single.isEmpty() ? List.of() : List.of(single);
    }

    private static String productField(Map<String, Object> body, String key) {
        Object pi = body.get("productInfo");
        if (!(pi instanceof Map<?, ?> map)) {
            return null;
        }
        Object v = map.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    private static BigDecimal decimalField(Map<String, Object> body, String key) {
        Object pi = body.get("productInfo");
        if (!(pi instanceof Map<?, ?> map)) {
            return null;
        }
        Object v = map.get(key);
        if (v == null) {
            return null;
        }
        String raw = String.valueOf(v).trim();
        if (raw.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(raw);
        } catch (NumberFormatException e) {
            throw new BadRequestException("价格格式不正确：" + raw);
        }
    }
}