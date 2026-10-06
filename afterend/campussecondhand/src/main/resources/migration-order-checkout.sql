-- ============================================================================
-- 一次性数据迁移：订单收银台所需字段
--
-- 背景：原 orders 表仅快照了 price，缺少商品标题/图片、运费与支付截止时间，
--       导致收银台无法独立展示交易要素，且商品被删改后订单信息失真。
--
-- 新增列：
--   order_title    商品标题快照（对齐 products.title）
--   order_image    商品首图快照（products.images 为逗号分隔的 base64 Data URL，
--                  故取 SUBSTRING_INDEX(...,',',1)；必须 MEDIUMTEXT 以容纳 base64）
--   shipping_fee   运费（校内自提免运费，当前恒为 0，保留字段以便扩展邮费规则）
--   expire_time    支付截止时间 = created_time + 30 分钟
--
-- !! 注意：这是一次性迁移脚本，请勿重复执行（本项目尚未引入 Flyway）。
--
-- 执行方式：
--   mysql -uroot -p campus_secondhand < migration-order-checkout.sql
-- ============================================================================

ALTER TABLE orders
    ADD COLUMN order_title  VARCHAR(100)       NULL COMMENT '商品标题快照' AFTER product_id,
    ADD COLUMN order_image  MEDIUMTEXT         NULL COMMENT '商品首图快照(base64)' AFTER order_title,
    ADD COLUMN shipping_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费' AFTER price,
    ADD COLUMN expire_time  DATETIME           NULL COMMENT '支付截止时间' AFTER paid_time;

-- 历史订单回填快照与支付截止时间
UPDATE orders o
LEFT JOIN products p ON p.id = o.product_id
SET o.order_title  = p.title,
    o.order_image  = SUBSTRING_INDEX(p.images, ',', 1),
    o.shipping_fee = 0.00,
    o.expire_time  = DATE_ADD(COALESCE(o.created_time, NOW()), INTERVAL 30 MINUTE)
WHERE o.order_title IS NULL;

-- 核对：不应有 order_title 仍为 NULL 的订单（商品确实不存在的除外）
SELECT id, order_no, product_id, order_title, shipping_fee, expire_time
FROM orders
ORDER BY id;
