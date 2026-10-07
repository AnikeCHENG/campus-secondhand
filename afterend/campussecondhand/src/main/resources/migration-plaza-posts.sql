-- ============================================================================
-- 大厅动态（posts）
--
-- 设计说明：
-- 1. posts 从一开始就带 product_id 列。SELL/FREE 类型发布时可携带 productInfo，
--    由 PostService 在同一事务内创建商品并回填 product_id；其余类型该列为 NULL。
--    之所以现在就加列（而不是「先建表、以后再加」），是为了避免二次迁移脚本
--    在已上线的库上执行，也让「动态是否关联商品」成为查询期可直接判断的字段。
-- 2. type 取值与后端 enums/PostType.java 严格一致：SELL/SEEK/FREE/WARN/CHAT。
--    注意与旧版前端 Plaza.vue 里的 sell/buy/warning/chat 不同名，那批值已随
--    mock 数据一并下线。
-- 3. tags / images 均为逗号分隔的字符串列（与 products.images 的既有做法一致，
--    项目没有真实文件上传接口，图片以 Base64 或 URL 形式存储）。
-- 4. 不建 likes/comments 冗余列，计数实时查 like_record / comments，
--    且仅当 product_id 非空时才统计（见接口实现）。
-- ============================================================================

CREATE TABLE IF NOT EXISTS posts (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '动态ID',
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    type VARCHAR(20) NOT NULL COMMENT '动态类型：SELL出售 SEEK求购 FREE免费送 WARN避雷 CHAT闲聊',
    content TEXT NOT NULL COMMENT '动态正文',
    tags VARCHAR(255) DEFAULT NULL COMMENT '标签，逗号分隔',
    images MEDIUMTEXT DEFAULT NULL COMMENT '图片，逗号分隔(Base64或URL)',
    product_id BIGINT DEFAULT NULL COMMENT '关联商品ID；仅SELL/FREE携带productInfo时非空',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    PRIMARY KEY (id),

    -- 首屏按时间倒序翻页（WHERE 1=1 ORDER BY created_time DESC）
    KEY idx_created (created_time),

    -- type 筛选：GET /api/posts?type=SELL
    KEY idx_type_created (type, created_time),

    -- hall-profile 的 posts/seeking 统计：WHERE user_id=? AND type=?
    KEY idx_user_type (user_id, type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='大厅动态';

-- ============================================================================
-- 种子数据（演示用）
--
-- 性质说明：种子是「数据」，前端 mock 是「代码里的假数据」。
-- 后者硬编码在 .vue 里、随构建产物进仓库、无法被真实数据取代；
-- 前者是数据库内容，会被真实发布覆盖，也可被一键清空。
-- 演示时大厅不至于空，同时代码里不留任何占位假数据。
--
-- 幂等：全部 INSERT 依赖 (id) 主键，重复执行会触发主键冲突而非静默重复插入。
-- 故用 INSERT IGNORE，重复执行安全。
--
-- product_id 关联的是演示库里已存在的商品（当前 owner 均为 user_id=1）：
--   1 测试商品-九成新自行车 / 2 测试商品-台灯 / 4 iPad 第九代 64G
--   5 捷安特山地车 / 6 罗技鼠标 G102 / 8 宿舍小台灯
-- SELL/FREE 三条关联商品，其余（SEEK/CHAT/WARN）product_id 为 NULL，
-- 前端据此隐藏点赞/评论按钮（现有互动接口挂在 /api/products 下，
-- 且 like_record 等表没有 target_type 列，post 与 product 的 ID 会碰撞）。
-- ============================================================================

INSERT IGNORE INTO posts (id, user_id, type, content, tags, images, product_id, created_time) VALUES
(1, 1, 'SELL',
 '毕业清仓：九成新自行车，车况良好，附送车锁和打气筒。东校区自提，可先试骑。',
 '自行车,毕业清仓', NULL, 1, DATE_SUB(NOW(), INTERVAL 2 HOUR)),

(2, 3, 'SEEK',
 '求购 2026 年线性代数真题及解析，任意版本都行，最好带完整答案。考研复试用，图书馆那几本答案太散了。',
 '教材,考研', NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 HOUR)),

(3, 1, 'CHAT',
 '毕业季准备清空宿舍，有没有人想一起组个「闲置互换」小组？把用不上的东西换成想要的，比扔掉划算。',
 '毕业季,闲置互换', NULL, NULL, DATE_SUB(NOW(), INTERVAL 1 DAY)),

(4, 4, 'WARN',
 '提醒：这周有人卖二手平板虚标成色，描述写着「全新」实际有明显划痕。平板和耳机这类贵重物品，尽量校内面交当面验机，别只看照片就转账。',
 '校园避雷,面交提醒', NULL, NULL, DATE_SUB(NOW(), INTERVAL 2 DAY)),

(5, 1, 'FREE',
 '搬宿舍剩两盏台灯，灯罩完好、光线柔和，直接送给需要的同学。西校区宿舍楼下自提，数量有限先到先得。',
 '免费送,台灯', NULL, 2, DATE_SUB(NOW(), INTERVAL 3 DAY)),

(6, 6, 'SEEK',
 '求购一个罗技鼠标，G102 或同价位都行，主要用来写代码，要求滚轮不飘。预算 80 以内。',
 '数码,鼠标', NULL, NULL, DATE_SUB(NOW(), INTERVAL 4 DAY)),

(7, 1, 'CHAT',
 '想问下大家平时怎么处理毕业季的书籍？我这边教材都太新了，闲置卖掉价格低，送人又不舍得分，有没有人一起打包处理的？',
 '毕业季,教材', NULL, NULL, DATE_SUB(NOW(), INTERVAL 5 DAY)),

(8, 3, 'SELL',
 '出宿舍闲置小台灯，护眼款可调亮度，用了半年没什么磕碰。价格好商量，能自提优先。',
 '台灯,宿舍', NULL, 8, DATE_SUB(NOW(), INTERVAL 6 DAY));