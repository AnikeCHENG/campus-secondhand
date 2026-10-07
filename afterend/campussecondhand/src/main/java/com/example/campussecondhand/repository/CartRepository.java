package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.Cart;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartRepository extends BaseMapper<Cart> {

    /** 购物车中的商品 ID，按加入时间倒序（与前端展示顺序一致） */
    @Select("SELECT product_id FROM cart WHERE user_id = #{userId} ORDER BY create_time DESC, id DESC")
    List<Long> findProductIdsByUserId(@Param("userId") Long userId);

    /** 移除单个商品；不命中也算成功，保持幂等 */
    @Delete("DELETE FROM cart WHERE user_id = #{userId} AND product_id = #{productId}")
    int deleteOne(@Param("userId") Long userId, @Param("productId") Long productId);

    /** 结算成功后批量清空已下单的商品，避免逐条 delete */
    @Delete("""
        <script>
        DELETE FROM cart WHERE user_id = #{userId}
        AND product_id IN
        <foreach collection="productIds" item="pid" open="(" separator="," close=")">#{pid}</foreach>
        </script>
        """)
    int deleteByProductIds(@Param("userId") Long userId,
                           @Param("productIds") List<Long> productIds);
}