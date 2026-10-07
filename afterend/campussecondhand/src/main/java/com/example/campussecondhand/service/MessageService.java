package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Message;
import com.example.campussecondhand.entity.User;
import com.example.campussecondhand.repository.MessageRepository;
import com.example.campussecondhand.repository.ProductRepository;
import com.example.campussecondhand.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    public List<Message> findByReceiverId(Long userId) {
        List<Message> messages = messageRepository.findByReceiverId(userId);
        messages.forEach(m -> {
            if (m.getSender() == null) {
                m.setSender(userRepository.selectById(m.getSenderId()));
            }
        });
        return messages;
    }

    public List<Message> findBySenderId(Long userId) {
        List<Message> messages = messageRepository.findBySenderId(userId);
        messages.forEach(m -> {
            if (m.getReceiver() == null) {
                m.setReceiver(userRepository.selectById(m.getReceiverId()));
            }
        });
        return messages;
    }

    public List<Message> findConversation(Long user1Id, Long user2Id) {
        List<Message> messages = messageRepository.findConversation(user1Id, user2Id);
        messageRepository.markConversationRead(user2Id, user1Id);
        messages.forEach(m -> {
            if (m.getSender() == null) {
                m.setSender(userRepository.selectById(m.getSenderId()));
            }
            if (m.getReceiver() == null) {
                m.setReceiver(userRepository.selectById(m.getReceiverId()));
            }
        });
        return messages;
    }

    public List<java.util.Map<String, Object>> findConversations(Long userId) {
        List<Message> received = messageRepository.findByReceiverId(userId);
        List<Message> sent = messageRepository.findBySenderId(userId);
        java.util.Map<Long, java.util.Map<String, Object>> convMap = new java.util.HashMap<>();
        received.forEach(m -> {
            User other = userRepository.selectById(m.getSenderId());
            m.setSender(other);
            putConversation(convMap, userId, m.getSenderId(),
                    other != null ? other.getUsername() : "用户" + m.getSenderId(),
                    other != null ? other.getAvatar() : null,
                    other != null && other.isStudentVerifiedUser(), m, true);
        });
        sent.forEach(m -> {
            User other = userRepository.selectById(m.getReceiverId());
            m.setReceiver(other);
            putConversation(convMap, userId, m.getReceiverId(),
                    other != null ? other.getUsername() : "用户" + m.getReceiverId(),
                    other != null ? other.getAvatar() : null,
                    other != null && other.isStudentVerifiedUser(), m, false);
        });
        return convMap.values().stream()
                .sorted((a, b) -> {
                    Object t1 = a.get("lastMessageTime");
                    Object t2 = b.get("lastMessageTime");
                    if (t1 == null && t2 == null) return 0;
                    if (t1 == null) return 1;
                    if (t2 == null) return -1;
                    return ((java.time.LocalDateTime) t2).compareTo((java.time.LocalDateTime) t1);
                })
                .toList();
    }

    private void putConversation(java.util.Map<Long, java.util.Map<String, Object>> convMap, Long meId, Long otherId, String username, String avatar, boolean studentVerified, Message msg, boolean fromThem) {
        java.util.Map<String, Object> conv = convMap.computeIfAbsent(otherId, k -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
map.put("otherUserId", otherId);
        map.put("userId", otherId);
        map.put("username", username);
        map.put("avatar", avatar);
        // 聊天窗口顶部的认证徽章需要这个字段
        map.put("studentVerified", studentVerified);
            map.put("lastMessage", "");
            map.put("lastMessageTime", null);
            map.put("unreadCount", 0);
            return map;
        });
        Object time = conv.get("lastMessageTime");
        if (time == null || (msg.getCreatedTime() != null && msg.getCreatedTime().isAfter((java.time.LocalDateTime) time))) {
            conv.put("lastMessage", msg.getContent());
            conv.put("lastMessageTime", msg.getCreatedTime());
        }
        if (fromThem && msg.getIsRead() != null && msg.getIsRead() == 0) {
            conv.put("unreadCount", ((Number) conv.get("unreadCount")).intValue() + 1);
        }
    }

    public Message findById(Long id) {
        return messageRepository.selectById(id);
    }

    public int countUnread(Long userId) {
        return messageRepository.countUnread(userId);
    }

    /** 单条消息内容长度上限 */
    public static final int MAX_CONTENT_LENGTH = 1000;

    /**
     * 发送消息。
     *
     * <p>此前本方法不做任何校验：内容可为空、可超长、可以给自己发消息、
     * 接收人不存在也照发入库，导致脏数据与前端莫名失败。此处集中兜住。</p>
     *
     * @throws IllegalArgumentException 校验不通过，调用方转为 400 业务响应
     */
    public Message send(Message message) {
        Long senderId = message.getSenderId();
        Long receiverId = message.getReceiverId();

        if (senderId == null || receiverId == null) {
            throw new IllegalArgumentException("发送人或接收人为空");
        }
        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("不能给自己发消息");
        }
        if (userRepository.selectById(receiverId) == null) {
            throw new IllegalArgumentException("接收用户不存在");
        }

        String content = message.getContent();
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }
        content = content.trim();
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("消息内容不能超过 " + MAX_CONTENT_LENGTH + " 个字符");
        }

        // 附带商品上下文时校验商品确实存在，避免挂到已删除商品上
        if (message.getProductId() != null
                && productRepository.selectById(message.getProductId()) == null) {
            message.setProductId(null);
        }

        message.setContent(content);
        message.setCreatedTime(LocalDateTime.now());
        message.setIsRead(0);
        messageRepository.insert(message);
        return message;
    }

    public boolean markAsRead(Long id) {
        Message message = messageRepository.selectById(id);
        if (message != null) {
            message.setIsRead(1);
            messageRepository.updateById(message);
            return true;
        }
        return false;
    }

    public boolean markAllAsRead(Long userId) {
        List<Message> messages = messageRepository.findByReceiverId(userId);
        messages.forEach(m -> {
            m.setIsRead(1);
            messageRepository.updateById(m);
        });
        return true;
    }

    public int delete(Long id) {
        return messageRepository.deleteById(id);
    }
}
