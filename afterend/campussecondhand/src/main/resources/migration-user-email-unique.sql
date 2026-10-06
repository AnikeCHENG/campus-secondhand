-- ============================================================================
-- 一次性数据迁移：users.email 增加唯一索引
--
-- 背景：此前 email 唯一性仅由应用层 existsByEmail() 检查保证，并发注册可绕过，
--       导致同一邮箱出现多条记录。补齐数据库层约束。
--
-- 前置校验（必须全部为 0 才可执行）：
--   SELECT email, COUNT(*) c FROM users WHERE email IS NOT NULL
--     GROUP BY email HAVING c > 1;          -- 期望：无结果
--   SELECT COUNT(*) FROM users WHERE email = '';  -- 期望：0（空串会被唯一索引视为重复值）
--   SELECT COUNT(*) FROM users WHERE email IS NULL; -- 允许多个 NULL，无需为 0
--
-- !! 注意：这是一次性迁移脚本，请勿重复执行（本项目尚未引入 Flyway，
--    重复执行 ADD UNIQUE KEY 会因索引已存在而报错）。
--
-- 执行方式：
--   mysql -uroot -p campus_secondhand < migration-user-email-unique.sql
-- ============================================================================

SELECT email, COUNT(*) AS cnt
FROM users
WHERE email IS NOT NULL
GROUP BY email
HAVING cnt > 1;

ALTER TABLE users ADD UNIQUE KEY uk_email (email);

SELECT INDEX_NAME, COLUMN_NAME, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'users'
  AND INDEX_NAME = 'uk_email';
