package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ProductRepository extends BaseMapper<Product> {

    /** 按状态查询，status 传 ProductStatus.getCode()，不在 SQL 中写魔法数字 */
    @Select("SELECT * FROM products WHERE status = #{status} ORDER BY created_time DESC")
    List<Product> findByStatus(@Param("status") Integer status);

    @Select("SELECT * FROM products WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<Product> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM products WHERE category = #{category} AND status = #{status} ORDER BY created_time DESC")
    List<Product> findByCategoryAndStatus(@Param("category") String category, @Param("status") Integer status);

    @Select("SELECT * FROM products WHERE title LIKE CONCAT('%', #{keyword}, '%') AND status = #{status} ORDER BY created_time DESC")
    List<Product> searchByKeywordAndStatus(@Param("keyword") String keyword, @Param("status") Integer status);

    @Select("SELECT * FROM products WHERE id = #{id}")
    Product findById(@Param("id") Long id);
}
