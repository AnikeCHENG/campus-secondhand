package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.campussecondhand.common.PageParam;
import com.example.campussecondhand.common.PageResult;
import java.util.Objects;
import java.util.stream.Collectors;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.Review;
import com.example.campussecondhand.exception.BadRequestException;
import com.example.campussecondhand.service.ReviewService;
import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.enums.ProductStatus;
import com.example.campussecondhand.enums.OrderStatus;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.OrderFeeService;
import com.example.campussecondhand.service.OrderService;
import com.example.campussecondhand.service.PaymentService.PayResult;
import com.example.campussecondhand.service.ProductService;
import com.example.campussecondhand.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private static final Logger log = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderFeeService orderFeeService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ReviewService reviewService;

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

/**
     * 与我相关的订单（我买到的 + 我卖出的），物理分页。
     *
     * <p>商品与卖家信息按当页 id 批量补齐，避免每条订单两次 selectById 的 N+1；
     * 输出字段与改造前完全一致，前端契约不变。</p>
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<?>> getMyOrders(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        PageParam paging = PageParam.of(page, size);
        Long userId = userOpt.get().getId();

        IPage<Order> paged = orderRepository.selectPage(
                paging.toPage(),
                new QueryWrapper<Order>()
                        // OR 必须整体包进 and(...)，否则会泄漏到后续追加的条件之外
                        .and(q -> q.eq("buyer_id", userId).or().eq("seller_id", userId))
                        .orderByDesc("created_time", "id"));

        List<Map<String, Object>> orderList = toOrderViews(paged.getRecords());

        return ResponseEntity.ok(ApiResponse.success("获取成功",
                PageResult.of(orderList, paged.getTotal(), paging)));
    }

    /**
     * 订单列表项组装：批量取商品与卖家，字段保持与历史实现一致。
     *
     * <p>图片是 base64 Data URL，载荷内含逗号，必须用 firstImage 而非 split(",")。</p>
     */
    private List<Map<String, Object>> toOrderViews(List<Order> orders) {
        List<Long> productIds = orders.stream()
                .map(Order::getProductId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Product> products = productIds.isEmpty() ? Map.of()
                : productRepository.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> sellerIds = orders.stream()
                .map(Order::getSellerId).filter(Objects::nonNull).distinct().toList();
        Map<Long, User> sellers = sellerIds.isEmpty() ? Map.of()
                : userRepository.selectBatchIds(sellerIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            // id 必须是订单主键：前端后续用它在 /api/orders/{id} 上做支付/取消/收货
            orderMap.put("id", order.getId());
            orderMap.put("orderNo", order.getOrderNo());
            orderMap.put("productId", order.getProductId());

            Product product = products.get(order.getProductId());
            if (product != null) {
                orderMap.put("productTitle", product.getTitle());
                orderMap.put("productImage", OrderService.firstImage(product.getImages()));
            } else {
                orderMap.put("productTitle", "商品已删除");
                orderMap.put("productImage", "");
            }

            orderMap.put("price", order.getPrice() != null ? order.getPrice().toString() : "0");
            orderMap.put("status", order.getStatus());
            orderMap.put("createdAt", order.getCreatedTime() != null ? order.getCreatedTime().toString() : "");
            orderMap.put("sellerId", order.getSellerId());

            User seller = sellers.get(order.getSellerId());
            orderMap.put("sellerName", seller != null ? seller.getUsername() : "未知卖家");

            // 补齐买卖双方 ID：前端聊天页按 userId 开会话，缺了它无从发起
            orderMap.put("buyerId", order.getBuyerId());

            result.add(orderMap);
        }
        return result;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> getOrderById(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }
        Long userId = userOpt.get().getId();
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return ResponseEntity.ok(ApiResponse.error(403, "无权访问此订单"));
        }
        // 惰性过期：超时未支付的订单在此自动取消并释放商品
        orderService.autoCancelIfExpired(order);
        // 返回收银台所需的全部交易要素
        return ResponseEntity.ok(ApiResponse.success("获取成功", orderService.buildCashierView(order)));
    }

    /** 创建订单（收银台入口） */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createOrder(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Long> request) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Long productId = request.get("productId");
        if (productId == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "商品ID不能为空"));
        }
        try {
            Order order = orderService.createOrder(productId, userOpt.get().getId());
            // 返回订单 ID 与编号，前端据此跳转收银台
            return ResponseEntity.ok(ApiResponse.success("创建成功",
                    orderService.buildCashierView(order)));
        } catch (OrderService.OrderBusinessException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        }
    }

    /**
     * 模拟支付。
     *
     * <p>请求体：{@code {"pay_method": "余额|支付宝|微信"}}。
     * 后端不做白名单校验，前端传什么就记什么，但不允许为空。</p>
     *
     * <p>支付渠道由 {@code PaymentService} 抽象，当前为本地 Mock 实现，
     * 不对接任何真实支付网关。</p>
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<?>> payOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        if (orderRepository.selectById(id) == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }
        String payMethod = body == null ? null : body.get("pay_method");

        // 归属校验、过期检查、状态守卫均在服务层完成；
        // 业务失败通过返回值表达而非抛异常，避免回滚掉「超时取消 + 商品释放」
        PayResult result = orderService.payOrder(id, payMethod, userOpt.get().getId());
        if (!result.isSuccess()) {
            return ResponseEntity.ok(ApiResponse.error(result.getCode(), result.getMessage()));
        }

        Order paid = orderRepository.selectById(id);
        Map<String, Object> data = new LinkedHashMap<>(orderService.buildCashierView(paid));
        data.put("tradeNo", result.getTradeNo());
        data.put("paymentChannel", orderService.getPaymentChannel());
        return ResponseEntity.ok(ApiResponse.success(result.getMessage(), data));
    }

/**
     * 提交评价：仅订单买家本人、仅已完成订单、一单一评。
     *
     * <p>权限与状态校验在 {@link ReviewService} 内完成，这里只负责取当前登录用户
     * 并把 {@link BadRequestException} 翻译成 400。</p>
     */
    @PostMapping("/{id}/review")
    public ResponseEntity<ApiResponse<?>> submitReview(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        try {
            Integer rating = body == null || body.get("rating") == null
                    ? null
                    : Integer.valueOf(String.valueOf(body.get("rating")).trim());
            String content = body == null || body.get("content") == null
                    ? null
                    : String.valueOf(body.get("content"));

            Review review = reviewService.submit(id, userOpt.get().getId(), rating, content);
            return ResponseEntity.ok(ApiResponse.success("评价成功", toReviewView(review)));
        } catch (BadRequestException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            log.error("提交评价失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "评价失败，请稍后重试"));
        }
    }

    /** 订单评价回显，供详情页显示已评价内容；未评价返回 data=null 而非 404 */
    @GetMapping("/{id}/review")
    public ResponseEntity<ApiResponse<?>> getReview(@PathVariable Long id) {
        try {
            Review review = reviewService.findByOrderId(id);
            return ResponseEntity.ok(ApiResponse.success("获取成功",
                    review == null ? null : toReviewView(review)));
        } catch (Exception e) {
            log.error("获取评价失败: ", e);
            return ResponseEntity.ok(ApiResponse.error(500, "获取失败"));
        }
    }

    private Map<String, Object> toReviewView(Review review) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", review.getId());
        view.put("orderId", review.getOrderId());
        view.put("productId", review.getProductId());
        view.put("reviewerId", review.getReviewerId());
        view.put("targetId", review.getTargetId());
        view.put("rating", review.getRating());
        view.put("content", review.getContent());
        view.put("createTime", review.getCreateTime());
        return view;
    }

    /** 取消订单：商品恢复为在售 */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<?>> cancelOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }
        Long userId = userOpt.get().getId();
        // 买家可取消自己的待支付订单；卖家不能单方面取消（应走下架流程）
        if (!order.getBuyerId().equals(userId)) {
            return ResponseEntity.ok(ApiResponse.error(403, "只有买家可以取消该订单"));
        }
        try {
            Order cancelled = orderService.cancelOrder(id);
            return ResponseEntity.ok(ApiResponse.success("订单已取消，商品已恢复在售",
                    orderService.buildCashierView(cancelled)));
        } catch (OrderService.OrderBusinessException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> updateOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id, 
            @RequestBody Order order) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Order existingOrder = orderRepository.selectById(id);
        if (existingOrder == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }
        Long userId = userOpt.get().getId();
        if (!existingOrder.getBuyerId().equals(userId) && !existingOrder.getSellerId().equals(userId)) {
            return ResponseEntity.ok(ApiResponse.error(403, "无权操作此订单"));
        }
        
        // 检查订单状态变更为已完成 / 已取消，并同步商品状态
        if (order.getStatus() != null && !order.getStatus().equals(existingOrder.getStatus())) {
            OrderStatus target = OrderStatus.fromCode(order.getStatus());
            if (target == null) {
                return ResponseEntity.ok(ApiResponse.error(400, "非法的订单状态值"));
            }
            if (target == OrderStatus.COMPLETED) {
                // 确认收货：商品保持已售出
                productService.markAsSold(existingOrder.getProductId());
            } else if (target == OrderStatus.CANCELLED) {
                // 取消订单：必须释放商品，否则商品永远无法再次购买
                productService.markAsOnSale(existingOrder.getProductId());
            }
        }
        
        // 只允许更新状态字段，防止越权修改价格/买卖家等
        if (order.getStatus() != null) {
            existingOrder.setStatus(order.getStatus());
        }
        if (order.getPaidTime() != null) {
            existingOrder.setPaidTime(order.getPaidTime());
        }
        if (order.getPaymentMethod() != null) {
            existingOrder.setPaymentMethod(order.getPaymentMethod());
        }
        if (order.getTransactionId() != null) {
            existingOrder.setTransactionId(order.getTransactionId());
        }
        existingOrder.setUpdatedTime(java.time.LocalDateTime.now());
        orderRepository.updateById(existingOrder);
        return ResponseEntity.ok(ApiResponse.success("更新成功", existingOrder));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> deleteOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }
        Long userId = userOpt.get().getId();
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            return ResponseEntity.ok(ApiResponse.error(403, "无权操作此订单"));
        }
        orderRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
}