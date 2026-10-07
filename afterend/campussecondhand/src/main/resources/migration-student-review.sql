-- ============================================================================
-- 学生认证（真实姓名 + 学号）与交易评价
--
-- 说明：users.is_student_verified 已在 plaza-schema.sql 中以 INT DEFAULT 0 建过，
--       本次不重复添加、也不改类型——INT 存 0/1 与 TINYINT 等价，
--       改类型只会带来无谓的迁移风险。本文件只补齐缺失的 student_no 与 real_name。
-- ============================================================================

-- ---------- 1. 学生认证：补齐学号与真实姓名 ----------
ALTER TABLE users
    ADD COLUMN student_no VARCHAR(20) DEFAULT NULL COMMENT '学号' AFTER is_student_verified,
    ADD COLUMN real_name VARCHAR(50) DEFAULT NULL COMMENT '真实姓名，仅认证时填写' AFTER student_no;

-- 学号唯一：一个学号只能对应一个账号，否则可被冒用他人身份骗取免手续费。
-- 注意 NULL 不参与唯一索引，因此未认证用户可以有任意多条 NULL。
ALTER TABLE users ADD UNIQUE KEY uk_student_no (student_no);

-- ---------- 2. 交易评价 ----------
-- 一单一评：唯一索引 uk_order_id 是"重复评价"的最终防线。
-- 即使应用层漏判，数据库也会直接拒绝第二次插入。
CREATE TABLE IF NOT EXISTS review (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评价ID',
    order_id BIGINT NOT NULL COMMENT '订单ID，一单一评',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    reviewer_id BIGINT NOT NULL COMMENT '评价人（买家）',
    target_id BIGINT NOT NULL COMMENT '被评价人（卖家）',
    rating TINYINT NOT NULL COMMENT '评分1-5',
    content VARCHAR(255) DEFAULT NULL COMMENT '评价内容，50字以内',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '评价时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_id (order_id),
    KEY idx_target_id (target_id),
    KEY idx_reviewer_id (reviewer_id),
    CONSTRAINT fk_review_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单评价';

-- 好评率查询是"按 target_id 聚合 + rating>=4 计数"，联合索引让这条查询走索引而非全表扫
CREATE INDEX idx_review_target_rating ON review (target_id, rating);