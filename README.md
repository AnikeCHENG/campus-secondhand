# 校园二手交易平台

基于 **Spring Boot + Vue 3** 的全栈校园二手交易平台，提供商品发布、搜索浏览、在线交易、站内消息、管理后台等完整功能。

---

## 项目简介

校园二手交易平台是一个面向高校学生的废旧物品再利用平台，旨在为在校学生提供一个便捷、安全的二手物品交易渠道。项目采用前后端分离架构，后端基于 Spring Boot 提供 RESTful API，前端基于 Vue 3 + Element Plus 构建现代化用户界面，同时内置完整的管理后台。

---

## 技术栈

### 后端

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 3.1.2 |
| 语言 | Java | 17 |
| ORM | MyBatis-Plus | 3.5.3.1 |
| 数据库 | MySQL | 8.0 |
| 认证 | JWT (jjwt) | 0.11.5 |
| 安全 | Spring Security + BCrypt | - |
| 构建 | Maven | 3.x |

### 前端

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Vue 3 (Composition API) | 3.5 |
| 构建 | Vite | 7.3 |
| UI 组件 | Element Plus | 2.3 |
| 状态管理 | Pinia | 3.0 |
| 路由 | Vue Router | 4.6 |
| 测试 | Vitest + Playwright | - |

---

## 功能模块

### 用户端

| 模块 | 功能 |
|------|------|
| 用户管理 | 注册、登录、JWT 认证、个人资料编辑、密码重置 |
| 商品浏览 | 商品列表、关键词搜索、分类筛选、价格区间、成色筛选、排序 |
| 商品发布 | 发布商品（含图片上传）、草稿保存、自动分类 |
| 商品详情 | 图片画廊、卖家信息、浏览量统计 |
| 交易管理 | 下单购买、订单状态跟踪（待支付→已支付→已发货→已完成→已取消） |
| 收藏功能 | 添加/取消收藏、收藏列表 |
| 站内消息 | 买卖双方实时聊天、未读消息提醒、消息轮询 |

### 管理后台

| 模块 | 功能 |
|------|------|
| 仪表盘 | 平台数据统计（用户数、商品数、订单数、消息数） |
| 用户管理 | 用户列表、搜索、启用/禁用 |
| 商品管理 | 商品列表、搜索筛选、上架/下架/重新上架、批量操作 |
| 订单管理 | 订单列表、状态筛选、状态变更、删除 |
| 分类管理 | 分类增删改、商品数量统计 |
| 消息管理 | 全站消息查看、搜索、删除 |

---

## 项目结构

```
campus-secondhand/
├── afterend/                          # 后端 Spring Boot 项目
│   └── campussecondhand/
│       ├── src/main/java/com/example/campussecondhand/
│       │   ├── common/                # 统一响应封装 (ApiResponse)
│       │   ├── config/                # Spring Security + MyBatis-Plus 配置
│       │   ├── controller/            # 8 个控制器 (Auth/User/Product/Order/
│       │   │                          #   Category/Favorite/Message/Admin)
│       │   ├── dto/                   # 数据传输对象 (UserDTO)
│       │   ├── entity/                # 6 个实体类 (User/Product/Order/
│       │   │                          #   Category/Favorite/Message)
│       │   ├── exception/             # 全局异常处理
│       │   ├── repository/            # MyBatis-Plus Mapper 接口
│       │   ├── service/               # 业务逻辑层
│       │   └── util/                  # JWT 工具类
│       └── src/main/resources/
│           └── application.properties # 应用配置
│
├── frontend/                          # 前端 Vue 3 项目
│   └── campus-secondhand/
│       ├── src/
│       │   ├── api/                   # API 服务层 (auth/product/message/order/user)
│       │   ├── components/            # 可复用组件 (DashboardCard/HeroBanner/...)
│       │   ├── router/                # 路由配置 + 全局守卫
│       │   ├── stores/                # Pinia 状态管理 (notification)
│       │   └── views/                 # 页面视图
│       │       ├── Home.vue           # 首页
│       │       ├── Login.vue          # 登录
│       │       ├── Register.vue       # 注册
│       │       ├── Products.vue       # 商品列表
│       │       ├── ProductDetail.vue  # 商品详情
│       │       ├── PostProduct.vue    # 发布商品
│       │       ├── Messages.vue       # 站内消息
│       │       ├── Profile.vue        # 个人中心
│       │       └── Admin/             # 管理后台 (5个页面)
│       ├── vite.config.js             # Vite 配置 (含 API 代理)
│       └── package.json
│
├── auto_categorize_products.py        # 商品自动分类脚本
├── create_products.py                 # 测试数据生成脚本
├── update_product_images.py           # 图片更新脚本
├── update_product_prices.py           # 价格更新脚本
├── test_login.py                      # 登录测试脚本
├── test_register.py                   # 注册测试脚本
├── 基于Spring Boot的校园二手交易平台的设计与实现.docx   # 毕业论文
└── 数据库表结构.xlsx                   # 数据库设计文档
```

---

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.x
- MySQL 8.0+
- Node.js 18+
- npm / pnpm

### 1. 数据库初始化

```sql
CREATE DATABASE waste_recycle_platform DEFAULT CHARACTER SET utf8mb4;
```

修改 `afterend/campussecondhand/src/main/resources/application.properties` 中的数据库连接信息：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/waste_recycle_platform
spring.datasource.username=root
spring.datasource.password=your_password
```

### 2. 启动后端

```bash
cd afterend/campussecondhand
mvn spring-boot:run
```

后端服务运行在 `http://localhost:8080`

### 3. 启动前端

```bash
cd frontend/campus-secondhand
npm install
npm run dev
```

前端开发服务器运行在 `http://localhost:5173`，API 请求自动代理到后端 8080 端口。

### 4. 访问

- 用户端：`http://localhost:5173`
- 管理后台：`http://localhost:5173/admin`（需要管理员账号）

---

## API 接口概览

| 模块 | 基路径 | 接口数 | 说明 |
|------|--------|--------|------|
| 认证 | `/api/auth` | 3 | 登录、注册、忘记密码 |
| 用户 | `/api/user` | 3 | 个人资料查询与更新 |
| 商品 | `/api/products` | 10 | 发布、搜索、分类、详情、上下架 |
| 订单 | `/api/orders` | 6 | 创建、查询、状态管理 |
| 分类 | `/api/categories` | 6 | 分类 CRUD + 层级查询 |
| 收藏 | `/api/favorites` | 5 | 收藏/取消/列表/检查 |
| 消息 | `/api/messages` | 8 | 发送、对话、未读、已读 |
| 管理 | `/api/admin` | 18 | 用户/商品/订单/分类/消息管理 + 统计 |
| **合计** | | **59** | |

---

## 数据库设计

### 核心表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `users` | 用户表 | id, username, email, password(BCrypt), phone, avatar, role, status |
| `products` | 商品表 | id, user_id, title, description, price, original_price, category, condition, images, view_count, status |
| `orders` | 订单表 | id, order_no, buyer_id, seller_id, product_id, price, status, payment_method |
| `categories` | 分类表 | id, name, description, parent_id, icon, sort_order |
| `favorites` | 收藏表 | id, user_id, product_id |
| `messages` | 消息表 | id, sender_id, receiver_id, product_id, content, is_read |

---

## 项目亮点

- **前后端分离**：Spring Boot RESTful API + Vue 3 SPA，架构清晰
- **完整业务流程**：注册→登录→浏览→发布→购买→消息沟通→订单跟踪，全链路闭环
- **管理后台**：内置完整的数据管理面板，支持用户/商品/订单/分类/消息全维度管理
- **JWT 认证**：无状态 Token 认证，支持双存储策略（localStorage/sessionStorage）
- **自动分类**：商品发布时根据标题关键词智能匹配分类
- **消息系统**：买卖双方实时站内通信，未读消息轮询提醒
- **图片处理**：前端图片压缩（800px + JPEG 0.7），节省带宽和存储
- **草稿功能**：商品发布支持本地草稿保存，防止编辑丢失
- **组件化开发**：Vue 3 Composition API + Element Plus，代码可维护性高

---

## 许可证

MIT License
