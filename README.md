# Token 制造 ERP

轻量级制造业 ERP，打通销售、MRP、采购、生产、库存、质量。

## 技术栈

- 前端：Vue 3 + TypeScript + Vite + Element Plus + Pinia + Axios
- 后端：Java 21 + Spring Boot 3 + Spring Security + MyBatis-Plus
- 数据库：MySQL 8 / Redis 7

## 目录结构

```
erp/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue3 前端
└── docker-compose.yml # Redis（MySQL 使用本机）
```

## 开发环境

| 组件 | 地址 | 账号 |
|---|---|---|
| MySQL | 127.0.0.1:3306 | root / 123456 |
| Redis | 127.0.0.1:6379 | 无密码 |

数据库：`token_erp`

## 启动

### 后端

```bash
cd backend
mvn spring-boot:run
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

前端默认 http://localhost:5173 ，`/api` 代理到后端 8080。

## 数据库

建表脚本在 `backend/src/main/resources/sql/`，按顺序执行：
`schema.sql` → `schema_biz.sql` → `schema_inventory.sql` → `init_data.sql`

## 状态

- [x] 开发环境
- [x] 工程骨架
- [x] 数据库设计（48 张表）
- [x] 后端基础框架
- [ ] 前端基础框架
