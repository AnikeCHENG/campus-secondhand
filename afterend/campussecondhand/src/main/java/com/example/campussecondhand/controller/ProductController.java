package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import java.util.Objects;
import java.util.stream.Collectors;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.BrowseHistoryService;
import com.example.campussecondhand.service.ProductService;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private BrowseHistoryService browseHistoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 大厅商品列表（物理分页）。
     *
     * <p>筛选与排序下推到 SQL，与分页在同一条语句内完成；
     * 若只分页而把筛选留在前端，结果只会覆盖当前页。
     * 卖家信息按当页 id 批量补齐，避免每条商品一次 selectById 的 N+1。</p>
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<?>> list(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String condition,
            @RequestParam(required = false, defaultValue = "newest") String sort) {

        PageParam paging = PageParam.of(page, size);
        IPage<Product> paged = productService.pageAvailable(
                paging, category, keyword, minPrice, maxPrice, condition, sort);

        List<Long> sellerIds = paged.getRecords().stream()
                .map(Product::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (!sellerIds.isEmpty()) {
            Map<Long, User> sellers = userRepository.selectBatchIds(sellerIds).stream()
                    .collect(Collectors.toMap(User::getId, u -> u));
            paged.getRecords().forEach(p -> p.setSeller(sellers.get(p.getUserId())));
        }

        return ResponseEntity.ok(ApiResponse.success("获取成功", PageResult.of(paged)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> myProducts(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.getUsernameFromToken(token);
            if (username == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
            }
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户不存在"));
            }
            List<Product> products = productService.findByUserId(userOpt.get().getId());
            return ResponseEntity.ok(ApiResponse.success("获取成功", products));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
        }
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<?>> byCategory(@PathVariable String category) {
        List<Product> products = productService.findByCategory(category);
        products.forEach(p -> {
            User seller = userRepository.selectById(p.getUserId());
            if (seller != null) p.setSeller(seller);
        });
        return ResponseEntity.ok(ApiResponse.success("获取成功", products));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<?>> search(@RequestParam String keyword) {
        List<Product> products = productService.searchByKeyword(keyword);
        products.forEach(p -> {
            User seller = userRepository.selectById(p.getUserId());
            if (seller != null) p.setSeller(seller);
        });
        return ResponseEntity.ok(ApiResponse.success("获取成功", products));
    }

    /**
     * 商品详情。
     *
     * <p>浏览历史在此顺带记录，但 {@code Authorization} 必须是<b>可选</b>请求头：
     * 商品详情允许游客访问，若设为必填会把未登录访客直接挡在 401。
     * 解析不到用户就跳过记录，绝不影响详情本身的返回。</p>
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> detail(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Product product = productService.findByIdWithViewCount(id);
        if (product != null) {
            User seller = userRepository.selectById(product.getUserId());
            if (seller != null) product.setSeller(seller);
            recordBrowseHistory(authHeader, id);
            return ResponseEntity.ok(ApiResponse.success("获取成功", product));
        }
        return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
    }

    /**
     * 记录浏览历史。未登录或 token 无效时静默跳过。
     *
     * <p>整段包 try-catch：历史记录属于附带能力，绝不能因为它失败
     * 而让用户看不到商品详情。</p>
     */
    private void recordBrowseHistory(String authHeader, Long productId) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return;
            }
            String username = jwtUtil.getUsernameFromToken(authHeader.substring(7));
            if (username == null) {
                return;
            }
            Optional<User> userOpt = userRepository.findByUsername(username);
            userOpt.ifPresent(user -> browseHistoryService.recordView(user.getId(), productId));
        } catch (Exception e) {
            // 静默忽略：不影响详情返回
        }
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> create(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> params) {
        try {
            // 从 token 中获取当前登录用户
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.getUsernameFromToken(token);
            if (username == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
            }
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户不存在"));
            }
            Long userId = userOpt.get().getId();

            Product product = new Product();
            product.setUserId(userId);
            product.setTitle((String) params.get("title"));
            product.setDescription((String) params.get("description"));
            product.setPrice(new BigDecimal(params.get("price").toString()));
            if (params.get("originalPrice") != null) {
                product.setOriginalPrice(new BigDecimal(params.get("originalPrice").toString()));
            }
            product.setCategory((String) params.get("category"));
            product.setCondition((String) params.get("condition"));
            product.setImages((String) params.get("images"));

            Product created = productService.create(product);
            return ResponseEntity.ok(ApiResponse.success("发布成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "发布失败: " + e.getMessage()));
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ApiResponse<?>> update(
            @PathVariable Long id,
            @RequestBody Map<String, Object> params) {
        try {
            Product product = productService.findById(id);
            if (product == null) {
                return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
            }

            if (params.get("title") != null) product.setTitle((String) params.get("title"));
            if (params.get("description") != null) product.setDescription((String) params.get("description"));
            if (params.get("price") != null) product.setPrice(new BigDecimal(params.get("price").toString()));
            if (params.get("originalPrice") != null) product.setOriginalPrice(new BigDecimal(params.get("originalPrice").toString()));
            if (params.get("category") != null) product.setCategory((String) params.get("category"));
            if (params.get("condition") != null) product.setCondition((String) params.get("condition"));
            if (params.get("images") != null) product.setImages((String) params.get("images"));

            Product updated = productService.update(product);
            return ResponseEntity.ok(ApiResponse.success("更新成功", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "更新失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<?>> delete(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.getUsernameFromToken(token);
            if (username == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
            }
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户不存在"));
            }

            Product product = productService.findById(id);
            if (product == null) {
                return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
            }
            if (!product.getUserId().equals(userOpt.get().getId())) {
                return ResponseEntity.ok(ApiResponse.error(403, "无权操作"));
            }

            int deleted = productService.delete(id);
            if (deleted > 0) {
                return ResponseEntity.ok(ApiResponse.success("删除成功", null));
            }
            return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "删除失败"));
        }
    }

    @PutMapping("/sold/{id}")
    public ResponseEntity<ApiResponse<?>> markAsSold(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String username = jwtUtil.getUsernameFromToken(token);
            if (username == null) {
                return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
            }
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(401, "用户不存在"));
            }

            Product product = productService.findById(id);
            if (product != null && !product.getUserId().equals(userOpt.get().getId())) {
                return ResponseEntity.ok(ApiResponse.error(403, "无权操作"));
            }

            boolean success = productService.markAsSold(id);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("已标记为已售出", null));
            }
            return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "操作失败"));
        }
    }

    // 批量更新商品图片和删除没有对应图片的商品
    /**
     * 图片迁移端点。<b>一次性数据迁移脚本专用，不对用户暴露。</b>
     *
     * <p>它必须遍历<b>全部</b>在售商品，逐个改写图片路径或删除无图商品，
     * 因此这里刻意调用不加分页的 {@code ProductService#findAllAvailable()}：
     * 一旦改成按页遍历，只会处理到第一页就静默漏掉其余商品。</p>
     */
    @PostMapping("/update-images")
    public ResponseEntity<ApiResponse<?>> updateImages(@RequestBody Map<String, Object> params) {
        try {
            List<String> imageNames = (List<String>) params.get("imageNames");
            if (imageNames == null || imageNames.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.error(400, "图片列表不能为空"));
            }

            // 获取所有商品
            List<Product> products = productService.findAllAvailable();
            int updatedCount = 0;
            int deletedCount = 0;

            for (Product product : products) {
                String title = product.getTitle();
                if (imageNames.contains(title)) {
                    // 更新商品图片
                    String imagePath = "/src/images/" + title + ".avif";
                    product.setImages(imagePath);
                    productService.update(product);
                    updatedCount++;
                } else {
                    // 删除没有对应图片的商品
                    productService.delete(product.getId());
                    deletedCount++;
                }
            }

            return ResponseEntity.ok(ApiResponse.success("操作成功", Map.of(
                    "updated", updatedCount,
                    "deleted", deletedCount
            )));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "操作失败: " + e.getMessage()));
        }
    }
}
