package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface ReportRepository extends BaseMapper<Report> {

    /**
     * 统计「该举报人对该目标」在 {@code since} 之后是否已有举报记录，用于 24 小时防刷。
     *
     * <p>用计数查询而非唯一索引：唯一索引只能做到「永久唯一」，
     * 而需求是「24 小时内唯一、窗口外可重新举报」。</p>
     *
     * <p>命中 {@code idx_reporter_target} 复合索引，四列都是等值/范围条件。</p>
     */
    @Select("""
        SELECT COUNT(*) FROM report
        WHERE reporter_id = #{reporterId}
          AND target_type = #{targetType}
          AND target_id = #{targetId}
          AND create_time >= #{since}
        """)
    long countRecentReports(@Param("reporterId") Long reporterId,
                            @Param("targetType") String targetType,
                            @Param("targetId") Long targetId,
                            @Param("since") LocalDateTime since);
}