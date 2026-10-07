package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.Review;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

@Mapper
public interface ReviewRepository extends BaseMapper<Review> {

    @Select("SELECT * FROM review WHERE order_id = #{orderId} LIMIT 1")
    Review findByOrderId(@Param("orderId") Long orderId);

    /**
     * 卖家评价聚合。
     *
     * <p>一条 SQL 算完好评率、平均星级与总数，而不是查三次再在内存里算：
     * 多次查询之间若有并发写入，三个数字可能来自不同瞬间，对不上账。</p>
     *
     * <p>{@code COALESCE(SUM(...), 0)} 处理"从未被评价"的卖家——
     * 无记录时 SUM 返回 NULL，直接除会得到 null 而前端会显示 "null%"。</p>
     */
    @Select("""
        SELECT COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN rating >= 4 THEN 1 ELSE 0 END), 0) AS good,
               COALESCE(AVG(rating), 0) AS avg_rating
        FROM review
        WHERE target_id = #{sellerId}
        """)
    Map<String, Object> aggregateByTargetId(@Param("sellerId") Long sellerId);
}