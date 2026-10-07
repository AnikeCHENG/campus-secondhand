package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.campussecondhand.service.AdminStatsService;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.Message;
import com.example.campussecondhand.entity.Category;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.service.PenaltyService;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.MessageRepository;
import com.example.campussecondhand.repository.CategoryRepository;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.ArrayList;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
/**
 * 平台管理后台。
 *
 * <p>鉴权已上移至 {@code AdminAuthInterceptor}：所有 {@code /api/admin/**}
 * 在进入本类之前就会校验 JWT 中的 role，非管理员直接返回 403。本类不再重复判断，
 * 避免新增端点时漏写检查造成越权。</p>
 */
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PenaltyService penaltyService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AdminStatsService adminStatsService;

    // ==================== 用户管理 ====================

// 获取所有用户
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<?>> getAllUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // 分页参数统一校验：page<1 或 size 越界直接 400，不再静默纠正
        PageParam paging = PageParam.of(page, size);

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            // OR 必须包进 and(...)：否则后续追加的条件会被 OR 拆开
            wrapper.and(q -> q.like("username", search).or().like("email", search));
        }
        wrapper.orderByDesc("created_time", "id");

        // 物理分页：由分页拦截器拼 LIMIT，不是查出全量再在内存里 skip/limit
        IPage<User> paged = userRepository.selectPage(paging.toPage(), wrapper);

        return ResponseEntity.ok(ApiResponse.success("获取成功",
                PageResult.of(paged.getRecords(), paged.getTotal(), paging)));
    }

    // 获取用户详情
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<?>> getUserDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
User user = userRepository.selectById(id);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }

        return ResponseEntity.ok(ApiResponse.success("获取成功", user));
    }

// 禁用/启用用户（处罚逻辑已收敛到 PenaltyService，此处仅做转发）
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateUserStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Integer status) {
        try {
            User user = penaltyService.updateUserStatus(id, status, operatorOf(authHeader));
            return ResponseEntity.ok(ApiResponse.success("更新成功", user));
        } catch (BadRequestException e) {
            // 保持与重构前一致的 HTTP 200 + code=404/400 语义
            return ResponseEntity.ok(ApiResponse.error(404, e.getMessage()));
        }
    }

    /** 从请求头解析操作者昵称，仅用于日志；解析失败不影响业务 */
    private String operatorOf(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return "unknown";
        }
        String username = jwtUtil.getUsernameFromToken(authHeader.substring(7));
        return username == null ? "unknown" : username;
    }

    // ==================== 商品管理 ====================

    // 获取所有商品（支持搜索、筛选、分页）
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<?>> getAllProducts(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // 分页参数统一校验：page<1 或 size 越界直接 400，不再静默纠正
        PageParam paging = PageParam.of(page, size);

com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            wrapper.and(q -> q.like("title", search).or().like("description", search));
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_time", "id");

        // 物理分页：由分页拦截器拼 LIMIT
        IPage<Product> paged = productRepository.selectPage(paging.toPage(), wrapper);

        return ResponseEntity.ok(ApiResponse.success("获取成功",
                PageResult.of(paged.getRecords(), paged.getTotal(), paging)));
    }

    // 获取商品详情
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<?>> getProductDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
Product product = productRepository.selectById(id);
        if (product == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
        }

        return ResponseEntity.ok(ApiResponse.success("获取成功", product));
    }

// 更新商品状态（处罚逻辑已收敛到 PenaltyService，此处仅做转发）
    @PutMapping("/products/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateProductStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Integer status) {
        try {
            Product product = penaltyService.updateProductStatus(id, status, operatorOf(authHeader));
            return ResponseEntity.ok(ApiResponse.success("更新成功", product));
        } catch (BadRequestException e) {
            // 保持与重构前一致的 HTTP 200 + code=400 语义
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        }
    }

    // 批量更新商品状态
    @PutMapping("/products/batch/status")
    public ResponseEntity<ApiResponse<?>> batchUpdateProductStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> params) {
List<?> rawIds = (List<?>) params.get("ids");
        Object rawStatus = params.get("status");
        if (rawIds == null || rawIds.isEmpty() || rawStatus == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "参数错误"));
        }
        Integer status;
        try {
            status = Integer.valueOf(rawStatus.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.error(400, "状态值必须是数字"));
        }

        // 白名单校验：只接受 ProductStatus 中存在的取值
        ProductStatus target = ProductStatus.fromCode(status);
        if (target == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "非法的商品状态值"));
        }

        int updatedCount = 0;
        for (Object rawId : rawIds) {
            Long id = Long.valueOf(rawId.toString());
            Product product = productRepository.selectById(id);
            if (product != null) {
                product.setStatus(target.getCode());
                productRepository.updateById(product);
                updatedCount++;
            }
        }

        return ResponseEntity.ok(ApiResponse.success("批量更新成功", Map.of("updatedCount", updatedCount)));
    }

    // ==================== 订单管理 ====================

    // 获取所有订单
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<?>> getAllOrders(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // 分页参数统一校验：page<1 或 size 越界直接 400，不再静默纠正
        PageParam paging = PageParam.of(page, size);

com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Order> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("created_time", "id");

        // 物理分页：由分页拦截器拼 LIMIT
        IPage<Order> paged = orderRepository.selectPage(paging.toPage(), wrapper);

        return ResponseEntity.ok(ApiResponse.success("获取成功",
                PageResult.of(paged.getRecords(), paged.getTotal(), paging)));
    }

    // 获取订单详情
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<?>> getOrderDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
Order order = orderRepository.selectById(id);
        if (order == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }

        return ResponseEntity.ok(ApiResponse.success("获取成功", order));
    }

    // 更新订单状态
    @PutMapping("/orders/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateOrderStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
String statusStr = params.get("status");
        if (statusStr == null || statusStr.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(400, "状态不能为空"));
        }

        Order order = orderRepository.selectById(id);
        if (order == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }
        try {
            Integer status = Integer.parseInt(statusStr);
            order.setStatus(status);
        } catch (NumberFormatException e) {
            return ResponseEntity.ok(ApiResponse.error(400, "状态值必须是数字"));
        }
        orderRepository.updateById(order);

        return ResponseEntity.ok(ApiResponse.success("更新成功", order));
    }

    // 删除订单
    @DeleteMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<?>> deleteOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
Order order = orderRepository.selectById(id);
        if (order == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }

        orderRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    // ==================== 消息管理 ====================

    // 获取所有消息
    @GetMapping("/messages")
    public ResponseEntity<ApiResponse<?>> getAllMessages(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        // 分页参数统一校验：page<1 或 size 越界直接 400，不再静默纠正
        PageParam paging = PageParam.of(page, size);

com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Message> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            wrapper.like("content", search);
        }
        // 排序与分页都交给 SQL：原来先查全量再在内存里 filter/sorted/skip，
        // 既是假分页，也会把 created_time 为 null 的行悄悄滤掉导致 total 对不上
        wrapper.isNotNull("created_time")
                .orderByDesc("created_time", "id");

        // 物理分页：由分页拦截器拼 LIMIT
        IPage<Message> paged = messageRepository.selectPage(paging.toPage(), wrapper);

        return ResponseEntity.ok(ApiResponse.success("获取成功",
                PageResult.of(paged.getRecords(), paged.getTotal(), paging)));
    }

    // 删除消息
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<?>> deleteMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
Message message = messageRepository.selectById(id);
        if (message == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "消息不存在"));
        }

        messageRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    // ==================== 分类管理 ====================

    // 获取所有分类
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<?>> getAllCategories(
            @RequestHeader("Authorization") String authHeader) {
List<Category> categories = categoryRepository.selectList(null);
        
        // 为每个分类添加商品数量
        List<Map<String, Object>> categoriesWithCount = new ArrayList<>();
        for (Category category : categories) {
            Map<String, Object> categoryMap = new HashMap<>();
            categoryMap.put("id", category.getId());
            categoryMap.put("name", category.getName());
            
            // 统计该分类下的商品数量
            com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> productWrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
            productWrapper.eq("category", category.getName());
            int productCount = productRepository.selectList(productWrapper).size();
            categoryMap.put("productCount", productCount);
            
            categoriesWithCount.add(categoryMap);
        }
        
        return ResponseEntity.ok(ApiResponse.success("获取成功", categoriesWithCount));
    }

    // 添加分类
    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<?>> addCategory(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, String> params) {
String name = params.get("name");
        if (name == null || name.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(400, "分类名称不能为空"));
        }

        Category category = new Category();
        category.setName(name);
        categoryRepository.insert(category);

        return ResponseEntity.ok(ApiResponse.success("添加成功", category));
    }

    // 更新分类
    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<?>> updateCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> params) {
String name = params.get("name");
        if (name == null || name.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(400, "分类名称不能为空"));
        }

        Category category = categoryRepository.selectById(id);
        if (category == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "分类不存在"));
        }

        category.setName(name);
        categoryRepository.updateById(category);

        return ResponseEntity.ok(ApiResponse.success("更新成功", category));
    }

    // 删除分类
    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<?>> deleteCategory(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
Category category = categoryRepository.selectById(id);
        if (category == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "分类不存在"));
        }

        categoryRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

// ==================== 数据统计 ====================

    /**
     * 平台概览。
     *
     * <p>原先用 {@code selectList(null).size()} 取计数——会把整表实体加载进内存
     * 只为得到一个数字，数据量一大就是明显的性能反模式。改用 {@code selectCount}，
     * 由数据库直接返回 COUNT。</p>
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> getStats(
            @RequestHeader("Authorization") String authHeader) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userRepository.selectCount(null));
        stats.put("productCount", productRepository.selectCount(null));
        stats.put("orderCount", orderRepository.selectCount(null));
        stats.put("messageCount", messageRepository.selectCount(null));
        // 在售与已售出分开计数，便于管理员判断平台供给结构
        QueryWrapper<Product> onSale = new QueryWrapper<>();
        onSale.eq("status", ProductStatus.ON_SALE.getCode());
        stats.put("onSaleProductCount", productRepository.selectCount(onSale));
        QueryWrapper<Product> sold = new QueryWrapper<>();
        sold.eq("status", ProductStatus.SOLD.getCode());
        stats.put("soldProductCount", productRepository.selectCount(sold));

        return ResponseEntity.ok(ApiResponse.success("获取成功", stats));
    }

    /**
     * 财务统计：累计成交额、平台手续费总额、卖家应收合计、近 6 个月趋势。
     *
     * <p>手续费口径见 {@link com.example.campussecondhand.service.AdminStatsService}：
     * 仅统计已支付且未取消的订单。</p>
     */
    @GetMapping("/stats/finance")
    public ResponseEntity<ApiResponse<?>> getFinance(
            @RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(ApiResponse.success("获取成功", adminStatsService.finance()));
    }

    /** 近 N 天每日新增用户/商品/订单趋势，默认 30 天 */
    @GetMapping("/stats/trend")
    public ResponseEntity<ApiResponse<?>> getTrend(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "30") Integer days) {
        int d = (days == null || days < 1) ? 30 : Math.min(days, 365);
        return ResponseEntity.ok(ApiResponse.success("获取成功", adminStatsService.dailyTrend(d)));
    }

    /** 商品分类交易热度（饼图数据源） */
    @GetMapping("/stats/category")
    public ResponseEntity<ApiResponse<?>> getCategoryHeat(
            @RequestHeader("Authorization") String authHeader) {
return ResponseEntity.ok(ApiResponse.success("获取成功", adminStatsService.categoryHeat()));
    }
}

