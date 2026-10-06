-- ============================================================================
-- 一次性数据迁移：浏览历史表
--
-- 背景：个人中心的「浏览历史」此前是前端假数据（ref([]) 从未赋值），
--       商品详情接口也不记录浏览。本次补齐后端存储。
--
-- 设计要点：
--   uk_user_product  唯一索引是 INSERT ... ON DUPLICATE KEY UPDATE 的前提。
--                     没有它，重复浏览同一商品会插入重复行，历史列表出现多条同一商品。
--   idx_user_time    服务于「按 last_view_time 倒序取最近 50 条」。
--   DATETIME(3)      必须毫秒精度：NOW() 只有秒级，同一秒内浏览两个商品会得到
--                     相同时间戳，倒序排序退化，「重新访问过的排最前」失效。
--
-- 不设 create_time：首访时间在业务上无展示价值，且与 last_view_time 语义重叠。
--
-- !! 注意：这是一次性迁移脚本，请勿重复执行（项目尚未引入 Flyway）。
-- ============================================================================

CREATE TABLE IF NOT EXISTS browse_history (
    id             BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT   NOT NULL                COMMENT '用户ID',
    product_id     BIGINT   NOT NULL                COMMENT '商品ID',
    last_view_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近浏览时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_user_time (user_id, last_view_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '浏览历史表';

SELECT COUNT(*) AS browse_history_rows FROM browse_history;
