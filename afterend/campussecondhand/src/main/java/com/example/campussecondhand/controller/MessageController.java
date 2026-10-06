package com.example.campussecondhand.controller;

import com.example.campussecondhand.common.ApiResponse;
import com.example.campussecondhand.entity.Message;
import com.example.campussecondhand.entity.Product;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.UserRepository;
import com.example.campussecondhand.service.MessageService;
import com.example.campussecondhand.service.ProductService;
import com.example.campussecondhand.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class MessageController {

    private static final Logger log = LoggerFactory.getLogger(MessageController.class);

    /** 从 Authorization 头解析当前用户；头缺失、格式错误、token 无效一律返回空 */
    private Optional<User> getUserFromToken(String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Optional.empty();
            }
            String username = jwtUtil.getUsernameFromToken(authHeader.substring(7));
            if (username == null) {
                return Optional.empty();
            }
            return userRepository.findByUsername(username);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Autowired
    private MessageService messageService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/received")
    public ResponseEntity<ApiResponse<?>> receivedMessages(@RequestHeader("Authorization") String authHeader) {
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
            List<Message> messages = messageService.findByReceiverId(userOpt.get().getId());
            return ResponseEntity.ok(ApiResponse.success("获取成功", messages));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
        }
    }

    @GetMapping("/sent")
    public ResponseEntity<ApiResponse<?>> sentMessages(@RequestHeader("Authorization") String authHeader) {
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
            List<Message> messages = messageService.findBySenderId(userOpt.get().getId());
            return ResponseEntity.ok(ApiResponse.success("获取成功", messages));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
        }
    }

    @GetMapping("/conversation/{otherUserId}")
    public ResponseEntity<ApiResponse<?>> conversation(
            @PathVariable Long otherUserId,
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
            List<Message> messages = messageService.findConversation(userOpt.get().getId(), otherUserId);
            return ResponseEntity.ok(ApiResponse.success("获取成功", messages));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<?>> conversations(@RequestHeader("Authorization") String authHeader) {
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
            return ResponseEntity.ok(ApiResponse.success("获取成功", messageService.findConversations(userOpt.get().getId())));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, e.getMessage()));
        }
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<?>> unreadCount(@RequestHeader("Authorization") String authHeader) {
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
            int count = messageService.countUnread(userOpt.get().getId());
            return ResponseEntity.ok(ApiResponse.success("获取成功", Map.of("count", count)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(401, "无效的token"));
        }
    }

    /**
     * 发送消息。
     *
     * <p>此前直接 {@code params.get("receiverId").toString()}：字段缺失时 NPE，
     * 被 catch 吞成「发送失败: null」——既看不出原因，也没有堆栈可查。
     * 现在所有参数先做存在性与类型校验，并区分「业务失败(400)」与
     * 「参数缺失(400)」、「服务端异常(500)」三类。</p>
     */
    @PostMapping("/send")
    public ResponseEntity<ApiResponse<?>> send(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody Map<String, Object> params) {
        Optional<User> userOpt = getUserFromToken(authHeader);
        if (userOpt.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error(401, "未登录或登录已过期"));
        }

        Long receiverId = parseLong(params.get("receiverId"));
        if (receiverId == null) {
            return ResponseEntity.ok(ApiResponse.error(400, "缺少接收人 receiverId"));
        }
        String content = params.get("content") == null ? null : String.valueOf(params.get("content"));
        if (content == null || content.isBlank()) {
            return ResponseEntity.ok(ApiResponse.error(400, "消息内容不能为空"));
        }
        Long productId = parseLong(params.get("productId"));

        try {
            Message message = new Message();
            message.setSenderId(userOpt.get().getId());
            message.setReceiverId(receiverId);
            message.setProductId(productId);
            message.setContent(content);

            Message sent = messageService.send(message);
            log.info("发送私信: from={} to={} productId={} contentLength={}",
                    sent.getSenderId(), sent.getReceiverId(), sent.getProductId(),
                    sent.getContent() == null ? 0 : sent.getContent().length());
            return ResponseEntity.ok(ApiResponse.success("发送成功", sent));
        } catch (IllegalArgumentException e) {
            // 业务校验失败：属于可预期的客户端问题，返回 400 且无需打印堆栈
            return ResponseEntity.ok(ApiResponse.error(400, e.getMessage()));
        } catch (Exception e) {
            // 必须打印堆栈，否则线上无法定位（此前整个控制器零日志，异常全部静默吞掉）
            log.error("发送私信异常: receiverId={}", receiverId, e);
            return ResponseEntity.ok(ApiResponse.error(500, "发送失败，请稍后重试"));
        }
    }

    /** 安全地把任意值转成 Long；无法转换时返回 null 而不是抛异常 */
    private static Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @PutMapping("/read/{id}")
    public ResponseEntity<ApiResponse<?>> markAsRead(
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

            boolean success = messageService.markAsRead(id);
            if (success) {
                return ResponseEntity.ok(ApiResponse.success("已标记为已读", null));
            }
            return ResponseEntity.ok(ApiResponse.error(404, "消息不存在"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "操作失败"));
        }
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<?>> markAllAsRead(@RequestHeader("Authorization") String authHeader) {
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

            messageService.markAllAsRead(userOpt.get().getId());
            return ResponseEntity.ok(ApiResponse.success("已全部标记为已读", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "操作失败"));
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

            Message message = messageService.findById(id);
            if (message == null) {
                return ResponseEntity.ok(ApiResponse.error(404, "消息不存在"));
            }
            Long userId = userOpt.get().getId();
            if (!message.getSenderId().equals(userId) && !message.getReceiverId().equals(userId)) {
                return ResponseEntity.ok(ApiResponse.error(403, "无权操作"));
            }

            int deleted = messageService.delete(id);
            if (deleted > 0) {
                return ResponseEntity.ok(ApiResponse.success("删除成功", null));
            }
            return ResponseEntity.ok(ApiResponse.error(404, "消息不存在"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(500, "删除失败"));
        }
    }
}
