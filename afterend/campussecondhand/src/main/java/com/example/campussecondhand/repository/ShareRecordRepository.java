package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.ShareRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShareRecordRepository extends BaseMapper<ShareRecord> {
    @Select("SELECT COUNT(*) FROM share_record WHERE post_id = ***REMOVED***{postId}")
    int countByPostId(@Param("postId") Long postId);
}
