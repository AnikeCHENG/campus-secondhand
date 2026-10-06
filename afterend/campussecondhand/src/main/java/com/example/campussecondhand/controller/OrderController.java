package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.campussecondhand.common.ApiResponse;
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
import com.example.campussecondhand.service.ProductService;
import com.example.campussecondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

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
    public ResponseEntity<ApiResponse<?>> getMyOrders(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Long userId = userOpt.get().getId();
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("buyer_id", userId).or().eq("seller_id", userId);
        List<Order> orders = orderRepository.selectList(wrapper);
        
        // 转换为前端期望的格式
        List<Map<String, Object>> orderList = new ArrayList<>();
        for (Order order : orders) {
            Map<String, Object> orderMap = new HashMap<>();
            // id 必须是订单主键：前端后续用它在 /api/orders/{id} 上做支付/取消/收货
            orderMap.put("id", order.getId());
            orderMap.put("orderNo", order.getOrderNo());
            orderMap.put("productId", order.getProductId());
            
            // 获取商品信息
            Product product = productRepository.selectById(order.getProductId());
            if (product != null) {
                orderMap.put("productTitle", product.getTitle());
                orderMap.put("productImage", product.getImages() != null ? product.getImages().split(",")[0] : "");
            } else {
                orderMap.put("productTitle", "商品已删除");
                orderMap.put("productImage", "");
            }
            
            orderMap.put("price", order.getPrice() != null ? order.getPrice().toString() : "0");
            orderMap.put("status", order.getStatus());
            orderMap.put("createdAt", order.getCreatedTime() != null ? order.getCreatedTime().toString() : "");
            orderMap.put("sellerId", order.getSellerId());
            
            // 获取卖家信息
            User seller = userRepository.selectById(order.getSellerId());
            orderMap.put("sellerName", seller != null ? seller.getUsername() : "未知卖家");
            
            orderList.add(orderMap);
        }
        
        return ResponseEntity.ok(ApiResponse.success("获取成功", orderList));
    }

    @GetMapping("/my/buyer")
    public ResponseEntity<ApiResponse<?>> getMyBuyerOrders(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("buyer_id", userOpt.get().getId());
        List<Order> orders = orderRepository.selectList(wrapper);
        return ResponseEntity.ok(ApiResponse.success("获取成功", orders));
    }

    @GetMapping("/my/seller")
    public ResponseEntity<ApiResponse<?>> getMySellerOrders(@RequestHeader("Authorization") String authHeader) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        wrapper.eq("seller_id", userOpt.get().getId());
        List<Order> orders = orderRepository.selectList(wrapper);
        return ResponseEntity.ok(ApiResponse.success("获取成功", orders));
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

    /** 模拟支付：仅待支付且未过期的订单可支付 */
    @PostMapping("/{id}/pay")
    public ResponseEntity<ApiResponse<?>> payOrder(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }
        Order order = orderRepository.selectById(id);
        if (order == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "订单不存在"));
        }
        // 仅买家可支付
        if (!order.getBuyerId().equals(userOpt.get().getId())) {
            return ResponseEntity.ok(ApiResponse.error(403, "只有买家可以支付该订单"));
        }
        String method = body == null ? null : body.get("paymentMethod");
        if (method == null || method.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error(400, "请选择支付方式"));
        }
        try {
            Order paid = orderService.payOrder(id, method);
            return ResponseEntity.ok(ApiResponse.success("支付成功",
                    orderService.buildCashierView(paid)));
        } catch (OrderService.OrderBusinessException e) {
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        }
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