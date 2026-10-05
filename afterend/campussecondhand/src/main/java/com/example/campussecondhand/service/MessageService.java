package com.example.campussecondhand.service;

import com.example.campussecondhand.entity.Message;
import com.example.campussecondhand.repository.MessageRepository;
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
            m.setSender(userRepository.selectById(m.getSenderId()));
            putConversation(convMap, userId, m.getSenderId(), m.getSender() != null ? m.getSender().getUsername() : "用户" + m.getSenderId(), m.getSender() != null ? m.getSender().getAvatar() : null, m, true);
        });
        sent.forEach(m -> {
            m.setReceiver(userRepository.selectById(m.getReceiverId()));
            putConversation(convMap, userId, m.getReceiverId(), m.getReceiver() != null ? m.getReceiver().getUsername() : "用户" + m.getReceiverId(), m.getReceiver() != null ? m.getReceiver().getAvatar() : null, m, false);
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

    private void putConversation(java.util.Map<Long, java.util.Map<String, Object>> convMap, Long meId, Long otherId, String username, String avatar, Message msg, boolean fromThem) {
        java.util.Map<String, Object> conv = convMap.computeIfAbsent(otherId, k -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("otherUserId", otherId);
            map.put("username", username);
            map.put("avatar", avatar);
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

    public Message send(Message message) {
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
