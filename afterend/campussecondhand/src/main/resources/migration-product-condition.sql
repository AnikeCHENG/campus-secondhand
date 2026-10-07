-- ============================================================================
-- 商品成色与瑕疵（二手交易特色）
--
-- 背景：products.condition 原本是 VARCHAR(20)，存的是自由中文文本
--      （全新 / 几乎全新 / 良好 / 明显使用痕迹 …）。它有两个问题：
--        1. 无法排序与筛选（前端 Products.vue 的成色筛选用的是
--           new/like-new/good/fair，与库里的中文值根本不匹配，该筛选一直失效）；
--        2. 文案自由，同义不同写（"良好" 与 "轻微使用痕迹" 谁更靠前无法判断）。
--      故把它规范化为 TINYINT 的五档等级，VARCHAR 列直接删除——
--      保留两列会产生"以哪列为准"的歧义，答辩时无法自圆其说。
--
-- 等级语义（数字越大越旧）：
--   0 全新        未使用或仅试机
--   1 99新        几乎全新，无明显使用痕迹
--   2 95新        轻微使用痕迹，功能完好
--   3 9成新       有正常使用痕迹
--   4 8成新及以下  明显磨损 / 有瑕疵
--
-- 执行顺序重要：先加列 → 回填 → 再删旧列，任何一步失败都可安全重跑。
-- ============================================================================

-- ---------- 1. 新增成色等级与瑕疵说明 ----------
ALTER TABLE products
    ADD COLUMN condition_level TINYINT DEFAULT 2 COMMENT '成色等级 0全新 1=99新 2=95新 3=9成新 4=8成新及以下' AFTER category,
    ADD COLUMN flaw_description VARCHAR(255) DEFAULT NULL COMMENT '瑕疵描述，如屏幕划痕、电池老化；为空表示无明显瑕疵' AFTER condition_level;

-- ---------- 2. 回填成色等级：中文文本 → 0~4 ----------
-- 注意：condition 是 MySQL 保留字（CONDITION），必须写成 `condition`，
-- 否则报 1064 语法错误——这与 ProductService 里 eq("`condition`", …) 是同一个坑。
-- 未知取值统一落到 2（95新）。宁可给一个中间档，也不要把"全新/8成新"这类
-- 极端值猜错——成色直接影响买家预期，错标比模糊更糟。
UPDATE products SET condition_level = CASE
    WHEN `condition` IN ('全新', '全新未使用', '未使用')                         THEN 0
    WHEN `condition` IN ('几乎全新', '99新')                                      THEN 1
    WHEN `condition` IN ('轻微使用痕迹', '95新')                                  THEN 2
    WHEN `condition` IN ('良好', '轻度磨损', '9成新')                             THEN 3
    WHEN `condition` IN ('明显使用痕迹', '一般', '一般磨损/瑕疵', '8成新及以下')  THEN 4
    ELSE 2
END;

-- ---------- 3. 成色等级是必填项：收紧为 NOT NULL ----------
-- DEFAULT 2 仅作为"绕过接口直接 INSERT"时的兜底，正常发布流程由接口强制校验。
ALTER TABLE products
    MODIFY COLUMN condition_level TINYINT NOT NULL DEFAULT 2 COMMENT '成色等级 0全新 1=99新 2=95新 3=9成新 4=8成新及以下';

-- ---------- 4. 删除旧的自由文本成色列 ----------
-- 同样必须反引号：DROP COLUMN condition 也会撞保留字报 1064。
-- 确认第 2 步已正确回填后再执行；执行此句即不可逆，需先备份。
ALTER TABLE products DROP COLUMN `condition`;

-- ---------- 5. 成色筛选索引 ----------
-- 大厅默认按成色筛选，低基数列单独建索引收益有限，这里留给
-- (status, condition_level) 联合索引，供"只看某成色的在售商品"使用。
CREATE INDEX idx_products_status_condition ON products (status, condition_level);