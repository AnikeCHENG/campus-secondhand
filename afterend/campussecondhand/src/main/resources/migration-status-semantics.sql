-- ============================================================================
-- 一次性数据迁移：products.status 语义统一
--
-- 旧语义：0=在售   1=已售出 2=下架
-- 新语义：0=下架   1=在售   2=已售出
--
-- 映射：旧0(在售) -> 新1(在售)
--       旧1(已售出) -> 新2(已售出)
--       旧2(下架) -> 新0(下架)
--
-- !! 注意：这是一次性迁移脚本，请勿重复执行 !!
--    重复执行会导致状态按 0->1->2->0 循环翻转，数据错乱。
--    本文件不会被 spring.sql.init 自动加载（未在 schema-locations 中配置）。
--
-- 执行方式（任选其一）：
--   1) MySQL 客户端：
--        mysql -uroot -p campus_secondhand < migration-status-semantics.sql
--   2) Navicat / DataGrip：打开该文件后在 campus_secondhand 库中运行
--   3) IDEA Database 工具面板：右键文件 -> Run SQL Script
--
-- 执行前建议先备份：
--   CREATE TABLE products_bak_status AS SELECT * FROM products;
-- ============================================================================

-- 1. 先看一眼迁移前的状态分布，便于事后核对
SELECT status AS old_status,
       CASE status WHEN 0 THEN '旧:在售' WHEN 1 THEN '旧:已售出'
                   WHEN 2 THEN '旧:下架' ELSE '旧:未知' END AS old_meaning,
       COUNT(*) AS cnt
FROM products
GROUP BY status;

-- 2. 语义转换
UPDATE products
SET status = CASE status
                WHEN 0 THEN 1   -- 在售
                WHEN 1 THEN 2   -- 已售出
                WHEN 2 THEN 0   -- 下架
                ELSE status     -- 未知值保持不变，人工介入
              END;

-- 3. 核对迁移结果
SELECT status AS new_status,
       CASE status WHEN 0 THEN '新:下架' WHEN 1 THEN '新:在售'
                   WHEN 2 THEN '新:已售出' ELSE '新:未知' END AS new_meaning,
       COUNT(*) AS cnt
FROM products
GROUP BY status;

-- ============================================================================
-- 数据一致性诊断（只读，不修改数据）：
-- 检查是否存在「已售出(2) 但无成交时间」的异常记录。
-- 如确需清理，请人工确认后单独执行下面注释掉的语句，不要在本脚本中默认执行。
--
-- SELECT id, title, status, sold_time
-- FROM products
-- WHERE status = 2 AND sold_time IS NULL;
--
-- UPDATE products SET status = 0 WHERE status = 2 AND sold_time IS NULL;
-- ============================================================================

