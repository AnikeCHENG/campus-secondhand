package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.LikeRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LikeRecordRepository extends BaseMapper<LikeRecord> {
    @Select("SELECT COUNT(*) FROM like_record WHERE post_id = ***REMOVED***{postId}")
    int countByPostId(@Param("postId") Long postId);

    @Select("SELECT COUNT(*) FROM like_record WHERE post_id = ***REMOVED***{postId} AND user_id = ***REMOVED***{userId}")
    int countByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);

    @Delete("DELETE FROM like_record WHERE post_id = ***REMOVED***{postId} AND user_id = ***REMOVED***{userId}")
    int deleteByPostIdAndUserId(@Param("postId") Long postId, @Param("userId") Long userId);
}
