package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.CommentRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CommentRecordRepository extends BaseMapper<CommentRecord> {
    @Select("SELECT * FROM comments WHERE post_id = #{postId} ORDER BY created_time ASC LIMIT #{size} OFFSET #{offset}")
    List<CommentRecord> findByPostId(@Param("postId") Long postId, @Param("size") int size, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM comments WHERE post_id = #{postId}")
    int countByPostId(@Param("postId") Long postId);
}
