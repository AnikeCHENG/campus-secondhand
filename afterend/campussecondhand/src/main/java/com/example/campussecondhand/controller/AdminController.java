package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.Order;
import com.example.campussecondhand.entity.Message;
import com.example.campussecondhand.entity.Category;
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
public class AdminController {

    @Autowired
    private UserRepository userRepository;

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

    private boolean isAdmin(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return false;
            }
            String token = authHeader.substring(7);
            Integer role = jwtUtil.getRoleFromToken(token);
            return role != null && role == 1;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== 用户管理 ====================

    // 获取所有用户
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<?>> getAllUsers(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            wrapper.like("username", search).or().like("email", search);
        }

        // 计算分页参数
        int offset = (page - 1) * pageSize;

        // 查询用户列表
        List<User> users = userRepository.selectList(wrapper.orderByDesc("created_time"));
        // 手动分页
        List<User> paginatedUsers = users.stream()
                .skip(offset)
                .limit(pageSize)
                .collect(java.util.stream.Collectors.toList());

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("items", paginatedUsers);
        result.put("total", users.size());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }

    // 获取用户详情
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<?>> getUserDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        User user = userRepository.selectById(id);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }

        return ResponseEntity.ok(ApiResponse.success("获取成功", user));
    }

    // 禁用/启用用户
    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateUserStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Integer status) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        User user = userRepository.selectById(id);
        if (user == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "用户不存在"));
        }
        user.setStatus(status);
        userRepository.updateById(user);

        return ResponseEntity.ok(ApiResponse.success("更新成功", user));
    }

    // ==================== 商品管理 ====================

    // 获取所有商品（支持搜索、筛选、分页）
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<?>> getAllProducts(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Product> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            wrapper.like("title", search).or().like("description", search);
        }
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        if (status != null) {
            wrapper.eq("status", status);
        }

        // 计算分页参数
        int offset = (page - 1) * pageSize;

        // 查询商品列表
        List<Product> products = productRepository.selectList(wrapper.orderByDesc("created_time"));
        // 手动分页
        List<Product> paginatedProducts = products.stream()
                .skip(offset)
                .limit(pageSize)
                .collect(java.util.stream.Collectors.toList());

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("items", paginatedProducts);
        result.put("total", products.size());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }

    // 获取商品详情
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<?>> getProductDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        Product product = productRepository.selectById(id);
        if (product == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
        }

        return ResponseEntity.ok(ApiResponse.success("获取成功", product));
    }

    // 更新商品状态
    @PutMapping("/products/{id}/status")
    public ResponseEntity<ApiResponse<?>> updateProductStatus(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id,
            @RequestBody Integer status) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        Product product = productRepository.selectById(id);
        if (product == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "商品不存在"));
        }
        product.setStatus(status);
        productRepository.updateById(product);

        return ResponseEntity.ok(ApiResponse.success("更新成功", product));
    }

    // 批量更新商品状态
    @PutMapping("/products/batch/status")
    public ResponseEntity<ApiResponse<?>> batchUpdateProductStatus(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> params) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        List<Long> ids = (List<Long>) params.get("ids");
        Integer status = (Integer) params.get("status");

        if (ids == null || ids.isEmpty() || status == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "参数错误"));
        }

        int updatedCount = 0;
        for (Long id : ids) {
            Product product = productRepository.selectById(id);
            if (product != null) {
                product.setStatus(status);
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
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Order> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (status != null) {
            wrapper.eq("status", status);
        }

        // 计算分页参数
        int offset = (page - 1) * pageSize;

        // 查询订单列表
        List<Order> orders = orderRepository.selectList(wrapper.orderByDesc("created_time"));
        // 手动分页
        List<Order> paginatedOrders = orders.stream()
                .skip(offset)
                .limit(pageSize)
                .collect(java.util.stream.Collectors.toList());

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("items", paginatedOrders);
        result.put("total", orders.size());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }

    // 获取订单详情
    @GetMapping("/orders/{id}")
    public ResponseEntity<ApiResponse<?>> getOrderDetail(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

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
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

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
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

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
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Message> wrapper = new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (search != null && !search.isEmpty()) {
            wrapper.like("content", search);
        }

        // 计算分页参数
        int offset = (page - 1) * pageSize;

        // 查询消息列表
        List<Message> messages = messageRepository.selectList(wrapper).stream()
                .sorted((m1, m2) -> m2.getCreatedTime().compareTo(m1.getCreatedTime()))
                .collect(java.util.stream.Collectors.toList());
        // 手动分页
        List<Message> paginatedMessages = messages.stream()
                .skip(offset)
                .limit(pageSize)
                .collect(java.util.stream.Collectors.toList());

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("items", paginatedMessages);
        result.put("total", messages.size());
        result.put("page", page);
        result.put("pageSize", pageSize);

        return ResponseEntity.ok(ApiResponse.success("获取成功", result));
    }

    // 删除消息
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<ApiResponse<?>> deleteMessage(
            @RequestHeader("Authorization") String authHeader,
            @PathVariable Long id) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

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
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

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
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

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
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

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
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        Category category = categoryRepository.selectById(id);
        if (category == null) {
            return ResponseEntity.ok(ApiResponse.error(404, "分类不存在"));
        }

        categoryRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    // ==================== 数据统计 ====================

    // 获取平台统计数据
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<?>> getStats(
            @RequestHeader("Authorization") String authHeader) {
        if (!isAdmin(authHeader)) {
            return ResponseEntity.ok(ApiResponse.error(403, "权限不足"));
        }

        // 获取用户数量
        int userCount = userRepository.selectList(null).size();
        // 获取商品数量
        int productCount = productRepository.selectList(null).size();
        // 获取订单数量
        int orderCount = orderRepository.selectList(null).size();
        // 获取消息数量
        int messageCount = messageRepository.selectList(null).size();

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("productCount", productCount);
        stats.put("orderCount", orderCount);
        stats.put("messageCount", messageCount);

        return ResponseEntity.ok(ApiResponse.success("获取成功", stats));
    }
}

