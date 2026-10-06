package com.example.campussecondhand.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.campussecondhand.entity.BrowseHistory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BrowseHistoryRepository extends BaseMapper<BrowseHistory> {

    /**
     * 记录一次浏览：不存在则插入，已存在则只更新时间。
     *
     * <p>依赖 {@code uk_user_product} 唯一索引；没有该索引 upsert 会退化为重复插入。</p>
     *
     * <p>用 {@code NOW(3)} 而非 {@code NOW()}：后者只有秒级精度，
     * 同一秒内浏览 A 再浏览 B 会得到相同时间戳，倒序排序退化为不确定，
     * 「重新访问过的商品应排到最前」就会失效。列必须为 {@code DATETIME(3)}。</p>
     */
    @Insert("""
        INSERT INTO browse_history (user_id, product_id, last_view_time)
        VALUES (***REMOVED***{userId}, ***REMOVED***{productId}, NOW(3))
        ON DUPLICATE KEY UPDATE last_view_time = NOW(3)
        """)
    void upsertView(@Param("userId") Long userId, @Param("productId") Long productId);

    /**
     * 裁剪历史，只保留最近 {@code keep} 条。
     *
     * <p>子查询外套一层派生表 {@code t} 是 MySQL 的限制：
     * 不能在 DELETE 的目标表上直接 SELECT 同一张表。</p>
     *
     * <p>排序追加 {@code id DESC} 作为最终 tiebreaker：即使毫秒级时间戳仍相同
     * （极少见），结果也保持确定而非随数据库执行计划抖动。</p>
     */
    @Delete("""
        DELETE FROM browse_history
        WHERE user_id = ***REMOVED***{userId}
          AND product_id NOT IN (
            SELECT product_id FROM (
              SELECT product_id FROM browse_history
              WHERE user_id = ***REMOVED***{userId}
              ORDER BY last_view_time DESC, id DESC
              LIMIT ***REMOVED***{keep}
            ) t
          )
        """)
    int trimToRecent(@Param("userId") Long userId, @Param("keep") int keep);

    /** 清空指定用户的浏览历史 */
    @Delete("DELETE FROM browse_history WHERE user_id = ***REMOVED***{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
