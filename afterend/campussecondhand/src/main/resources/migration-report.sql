-- ============================================================================
-- 举报处理模块
--
-- 列名 create_time / update_time 按接口契约约定。
-- 注意与仓库其他表的命名不一致：users / orders / products 用 created_time，
-- 而 report 契约与 review 表一样用 create_time。实体侧通过
-- @TableField("create_time") 映射到字段 createdTime，保持前端 JSON 字段名为
-- createdTime（AdminReports.vue 按该名字读取），不为了统一而改动前端。
-- ============================================================================

CREATE TABLE IF NOT EXISTS report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '举报ID',
    reporter_id BIGINT NOT NULL COMMENT '举报人ID',
    target_id BIGINT NOT NULL COMMENT '被举报对象ID',
    target_type VARCHAR(10) NOT NULL COMMENT '被举报类型：PRODUCT商品 USER用户',
    reason VARCHAR(100) NOT NULL COMMENT '举报原因，如虚假宣传/违禁品/假冒伪劣/辱骂',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0待处理 1已处理并处罚 2已驳回',
    admin_remark VARCHAR(500) DEFAULT NULL COMMENT '管理员处理备注，内含处罚动作留痕快照',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '举报时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),

    -- 管理端默认按 status=0 + 时间倒序翻页，该索引覆盖该场景
    KEY idx_status_create (status, create_time),

    -- 24 小时防刷查询：WHERE reporter_id=? AND target_type=? AND target_id=?
    --   AND create_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR)
    -- 防刷用计数查询而非唯一索引：时间窗口内唯一、窗口外可重报，唯一索引做不到
    KEY idx_reporter_target (reporter_id, target_type, target_id, create_time),

    -- 管理端按类型筛选 + 关键词搜索（search 命中 reason）时使用
    KEY idx_target_type (target_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户举报';