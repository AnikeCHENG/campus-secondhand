package com.example.campussecondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.OrderRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.OrderFeeService;
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
            orderMap.put("id", order.getOrderNo());
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
        return ResponseEntity.ok(ApiResponse.success("获取成功", order));
    }

    @PostMapping
    @org.springframework.transaction.annotation.Transactional
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
        
        // 检查商品是否存在
        Product product = productRepository.selectById(productId);
        if (product == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
        }
        if (product.getStatus() != null && product.getStatus() == 1) {
            return ResponseEntity.ok(ApiResponse.error(400, "商品已售出"));
        }

        // 获取买家ID
        Long buyerId = userOpt.get().getId();

        // 不能购买自己的商品
        if (product.getUserId() != null && product.getUserId().equals(buyerId)) {
            return ResponseEntity.ok(ApiResponse.error(400, "不能购买自己的商品"));
        }

        // 创建订单
        Order order = new Order();
        order.setOrderNo("ORD" + System.currentTimeMillis() + String.format("%04d", (int)(Math.random() * 10000)));
        order.setProductId(productId);
        order.setBuyerId(buyerId);
        order.setSellerId(product.getUserId() != null ? product.getUserId() : buyerId);
        order.setPrice(product.getPrice());
        order.setStatus(0);
        order.setCreatedTime(java.time.LocalDateTime.now());

        User seller = userRepository.selectById(order.getSellerId());
        boolean studentVerified = seller != null && seller.getIsStudentVerified() != null && seller.getIsStudentVerified() == 1;
        java.math.BigDecimal fee = orderFeeService.calculateServiceFee(product.getPrice(), studentVerified);
        order.setServiceFee(fee);
        order.setSellerIncome(product.getPrice().subtract(fee).setScale(2, java.math.RoundingMode.HALF_UP));

        orderRepository.insert(order);

        // 订单创建成功后再标记商品售出
        productService.markAsSold(productId);

        return ResponseEntity.ok(ApiResponse.success("创建成功", order));
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
        
        // 检查订单状态是否变更为已完成
        if (order.getStatus() != null && order.getStatus() == 3 && existingOrder.getStatus() != 3) {
            // 同步更新商品状态为已售出
            Product product = productRepository.selectById(existingOrder.getProductId());
            if (product != null) {
                productService.markAsSold(product.getId());
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