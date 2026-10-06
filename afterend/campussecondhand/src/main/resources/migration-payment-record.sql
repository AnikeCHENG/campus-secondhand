-- ============================================================================
-- 一次性数据迁移：支付流水表
--
-- 背景：模拟支付此前仅在 orders 表上写 payment_method/transaction_id，
--       缺少独立的支付流水，无法对账、无法追溯，也无法为将来接入
--       真实支付渠道（支付宝/微信）保留扩展结构。
--
-- 设计要点：
--   amount      买家实付 = price + shipping_fee。
--               不含服务费——服务费由卖家承担，不进入买家支付金额。
--   pay_method  余额 / 支付宝 / 微信。后端不做白名单校验，前端传什么记什么。
--   trade_no    渠道流水号，加唯一索引：同一渠道流水不允许重复入账，
--               这是支付系统的基本完整性约束（防重复扣款/重复入账）。
--
-- !! 注意：这是一次性迁移脚本，请勿重复执行（项目尚未引入 Flyway）。
-- ============================================================================

CREATE TABLE IF NOT EXISTS payment_record (
    id          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '流水ID',
    order_id    BIGINT         NOT NULL                COMMENT '订单ID',
    order_no    VARCHAR(50)    NOT NULL                COMMENT '订单编号(冗余便于对账)',
    amount      DECIMAL(10, 2) NOT NULL                COMMENT '支付金额(买家实付,不含卖家承担的服务费)',
    pay_method  VARCHAR(50)    NOT NULL                COMMENT '支付方式:余额/支付宝/微信',
    trade_no    VARCHAR(100)   NOT NULL                COMMENT '渠道流水号',
    create_time DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_trade_no (trade_no),
    KEY idx_order_id (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '支付流水表';

SELECT COUNT(*) AS payment_record_rows FROM payment_record;
