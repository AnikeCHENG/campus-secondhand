-- ============================================================================
-- 购物车
--
-- 一件就是一件：二手孤品没有数量概念，因此 cart 一行 = 一个商品，
-- 表中不设 quantity 列。用户重复加入同一商品会撞唯一索引，
-- 由 ReportService 之外的 CartService 捕获 DuplicateKeyException 转成友好提示。
--
-- 无外键：与 orders 表的 product_id 一致，商品被物理删除后购物车里会留下
-- 悬空行，此时 list 接口按 valid=false 返回，前端据此置灰而不是静默消失
-- （与 FavoriteService 对已删商品的既有处理方式保持一致）。
-- ============================================================================

CREATE TABLE IF NOT EXISTS cart (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车';