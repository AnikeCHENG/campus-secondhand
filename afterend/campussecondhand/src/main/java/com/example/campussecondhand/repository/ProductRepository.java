package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ProductRepository extends BaseMapper<Product> {

    @Select("SELECT * FROM products WHERE status = 0 ORDER BY created_time DESC")
    List<Product> findAllAvailable();

    @Select("SELECT * FROM products WHERE user_id = ***REMOVED***{userId} ORDER BY created_time DESC")
    List<Product> findByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM products WHERE category = ***REMOVED***{category} AND status = 0 ORDER BY created_time DESC")
    List<Product> findByCategory(@Param("category") String category);

    @Select("SELECT * FROM products WHERE title LIKE CONCAT('%', ***REMOVED***{keyword}, '%') AND status = 0 ORDER BY created_time DESC")
    List<Product> searchByKeyword(@Param("keyword") String keyword);

    @Select("SELECT * FROM products WHERE id = ***REMOVED***{id}")
    Product findById(@Param("id") Long id);
}
