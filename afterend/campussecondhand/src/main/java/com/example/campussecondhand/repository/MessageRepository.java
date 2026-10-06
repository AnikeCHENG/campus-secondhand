package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.Message;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import java.util.List;
import java.util.Map;

@Mapper
public interface MessageRepository extends BaseMapper<Message> {

    @Select("SELECT * FROM messages WHERE receiver_id = #{userId} ORDER BY created_time DESC")
    List<Message> findByReceiverId(@Param("userId") Long userId);

    @Select("SELECT * FROM messages WHERE sender_id = #{userId} ORDER BY created_time DESC")
    List<Message> findBySenderId(@Param("userId") Long userId);

    @Select("SELECT * FROM messages WHERE (sender_id = #{user1Id} AND receiver_id = #{user2Id}) OR (sender_id = #{user2Id} AND receiver_id = #{user1Id}) ORDER BY created_time ASC")
    List<Message> findConversation(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);

    @Select("SELECT COUNT(*) FROM messages WHERE receiver_id = #{userId} AND is_read = 0")
    int countUnread(@Param("userId") Long userId);

    @Select("""
        SELECT
          CASE WHEN sender_id = #{userId} THEN receiver_id ELSE sender_id END AS other_user_id,
          u.username,
          u.avatar,
          (SELECT content FROM messages m2 WHERE (m2.sender_id = m.sender_id AND m2.receiver_id = m.receiver_id) OR (m2.sender_id = m.receiver_id AND m2.receiver_id = m.sender_id) ORDER BY m2.created_time DESC LIMIT 1) AS lastMessage,
          (SELECT created_time FROM messages m2 WHERE (m2.sender_id = m.sender_id AND m2.receiver_id = m.receiver_id) OR (m2.sender_id = m.receiver_id AND m2.receiver_id = m.sender_id) ORDER BY m2.created_time DESC LIMIT 1) AS lastMessageTime,
          (SELECT COUNT(*) FROM messages m3 WHERE m3.sender_id = CASE WHEN m.sender_id = #{userId} THEN m.receiver_id ELSE m.sender_id END AND m3.receiver_id = #{userId} AND m3.is_read = 0) AS unreadCount
        FROM messages m
        LEFT JOIN users u ON u.id = CASE WHEN m.sender_id = #{userId} THEN m.receiver_id ELSE m.sender_id END
        WHERE m.sender_id = #{userId} OR m.receiver_id = #{userId}
        GROUP BY CASE WHEN m.sender_id = #{userId} THEN m.receiver_id ELSE m.sender_id END, u.username, u.avatar
        ORDER BY lastMessageTime DESC
        """)
    List<Map<String, Object>> findConversations(@Param("userId") Long userId);

    @Update("UPDATE messages SET is_read = 1 WHERE sender_id = #{otherUserId} AND receiver_id = #{userId} AND is_read = 0")
    int markConversationRead(@Param("otherUserId") Long otherUserId, @Param("userId") Long userId);
}
