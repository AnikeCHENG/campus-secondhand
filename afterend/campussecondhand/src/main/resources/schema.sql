-- 校园二手交易平台数据库表结构
-- 根据实体类生成

CREATE DATABASE IF NOT EXISTS campus_secondhand DEFAULT CHARACTER SET utf8mb4 DEFAULT COLLATE utf8mb4_unicode_ci;
USE campus_secondhand;

-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    avatar MEDIUMTEXT DEFAULT NULL COMMENT '头像(Base64 Data URL)',
    bio VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    location VARCHAR(100) DEFAULT NULL COMMENT '所在位置',
    qq VARCHAR(20) DEFAULT NULL COMMENT '联系QQ',
    wechat VARCHAR(50) DEFAULT NULL COMMENT '联系微信',
    status INT DEFAULT 1 COMMENT '状态：1正常，0禁用',
    role INT DEFAULT 0 COMMENT '角色：0普通用户，1管理员',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted INT DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分类表
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    description VARCHAR(200) DEFAULT NULL COMMENT '分类描述',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID',
    icon VARCHAR(500) DEFAULT NULL COMMENT '分类图标',
    sort_order INT DEFAULT 0 COMMENT '排序权重',
    status INT DEFAULT 1 COMMENT '状态：1启用，0禁用',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 商品表
CREATE TABLE IF NOT EXISTS products (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '发布者ID',
    title VARCHAR(100) NOT NULL COMMENT '商品标题',
    description TEXT DEFAULT NULL COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) DEFAULT NULL COMMENT '原价',
    category VARCHAR(50) DEFAULT NULL COMMENT '分类名称',
    `condition` VARCHAR(20) DEFAULT NULL COMMENT '成色',
    images MEDIUMTEXT DEFAULT NULL COMMENT '图片(逗号分隔Base64或URL)',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    status INT DEFAULT 1 COMMENT '商品状态：0下架，1在售，2已售出',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    sold_time DATETIME DEFAULT NULL COMMENT '售出时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id),
    KEY idx_category (category),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    order_no VARCHAR(50) NOT NULL COMMENT '订单号',
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    seller_id BIGINT NOT NULL COMMENT '卖家ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    order_title VARCHAR(100) DEFAULT NULL COMMENT '商品标题快照',
    order_image MEDIUMTEXT DEFAULT NULL COMMENT '商品首图快照(base64)',
    price DECIMAL(10,2) NOT NULL COMMENT '成交价',
    shipping_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '运费(校内自提免运费)',
    status INT DEFAULT 0 COMMENT '状态：0待支付 1待发货 2待收货 3已完成 4已取消',
    payment_method VARCHAR(50) DEFAULT NULL COMMENT '支付方式',
    transaction_id VARCHAR(100) DEFAULT NULL COMMENT '交易流水号',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    paid_time DATETIME DEFAULT NULL COMMENT '支付时间',
    expire_time DATETIME DEFAULT NULL COMMENT '支付截止时间',
    completed_time DATETIME DEFAULT NULL COMMENT '完成时间',
    service_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '平台服务费(卖家承担)',
    seller_income DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '卖家应收金额',
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_buyer_id (buyer_id),
    KEY idx_seller_id (seller_id),
    KEY idx_product_id (product_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 收藏表
CREATE TABLE IF NOT EXISTS favorites (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id),
    KEY idx_user_id (user_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 消息表
CREATE TABLE IF NOT EXISTS messages (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    product_id BIGINT DEFAULT NULL COMMENT '关联商品ID',
    content TEXT DEFAULT NULL COMMENT '消息内容',
    is_read INT DEFAULT 0 COMMENT '是否已读：0未读，1已读',
    created_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    PRIMARY KEY (id),
    KEY idx_sender_id (sender_id),
    KEY idx_receiver_id (receiver_id),
    KEY idx_product_id (product_id),
    KEY idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 初始化分类数据
INSERT INTO categories (name, description, sort_order, status) VALUES
('图书教材', '二手图书、教材、考试资料', 1, 1),
('数码电子', '手机、电脑、平板、耳机等', 2, 1),
('生活用品', '日常用品、宿舍用品', 3, 1),
('服饰鞋包', '衣服、鞋子、包包', 4, 1),
('运动户外', '运动器材、户外装备', 5, 1),
('美妆护肤', '化妆品、护肤品', 6, 1),
('学习用品', '文具、学习工具', 7, 1),
('其他', '其他闲置物品', 8, 1);


-- ============================================================
-- 支付流水表
-- amount 为买家实付（订单价 + 运费），不含卖家承担的服务费
-- trade_no 唯一索引：同一渠道流水不允许重复入账
-- ============================================================
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付流水表';
SELECT 'Schema created successfully!' AS message;
