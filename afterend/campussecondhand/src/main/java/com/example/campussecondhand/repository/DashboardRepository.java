package com.example.campussecondhand.repository;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface DashboardRepository {
    @Select("""
        SELECT
          (SELECT COUNT(*) FROM products WHERE status = #{onSaleStatus} AND user_id = #{userId}) AS sellingCount,
          (SELECT COUNT(*) FROM orders
            WHERE (buyer_id = #{userId} OR seller_id = #{userId})
              AND status IN (0,1,2)) AS pendingOrderCount,
          (SELECT COUNT(*) FROM messages
            WHERE receiver_id = #{userId} AND is_read = 0) AS unreadMessageCount
        """)
    Map<String, Object> stats(@Param("userId") Long userId, @Param("onSaleStatus") Integer onSaleStatus);

    @Select("""
        SELECT u.id AS id, u.username AS username, u.avatar AS avatar,
          GREATEST(
            IFNULL(MAX(o.created_time), '1970-01-01'),
            IFNULL(MAX(m.created_time), '1970-01-01'),
            IFNULL(MAX(p.created_time), '1970-01-01'),
            IFNULL(u.updated_time, '1970-01-01')
          ) AS lastActiveTime
        FROM users u
        LEFT JOIN orders o ON o.buyer_id = u.id OR o.seller_id = u.id
        LEFT JOIN messages m ON m.sender_id = u.id OR m.receiver_id = u.id
        LEFT JOIN products p ON p.user_id = u.id
        GROUP BY u.id, u.username, u.avatar, u.updated_time
        ORDER BY lastActiveTime DESC
        LIMIT 5
        """)
    List<Map<String, Object>> activeUsers();
}
