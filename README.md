# Token 制造 ERP

轻量级制造业 ERP，打通销售、MRP、采购、生产、库存、质量。

## 技术栈

- 前端：Vue 3 + TypeScript + Vite + Element Plus + Pinia + Axios
- 后端：Java 21 + Spring Boot 3 + Spring Security + MyBatis-Plus
- 数据库：MySQL 8（本机）/ Redis 7（Docker）

## 目录结构

```
erp/
├── backend/          # Spring Boot 后端
├── frontend/         # Vue3 前端
├── docs/             # 需求文档
└── docker-compose.yml # Redis（MySQL 使用本机）
```

## 开发环境

| 组件 | 地址 | 账号 |
|---|---|---|
| MySQL | 127.0.0.1:3306 | root / <见 .env 的 DB_PASSWORD> |
| Redis | 127.0.0.1:6379 | 无密码 |

数据库：`token_erp`

---

## 启动教程

### 前置环境（必须已安装）

| 依赖 | 版本要求 | 检查命令 |
|---|---|---|
| JDK | **21**（必须，代码用 21 编译） | `java -version` |
| Maven | 3.6+ | `mvn -v` |
| Node.js | 18+ | `node -v` |
| MySQL | 8.x（本机安装） | 端口 3306 |
| Redis | 7.x（Docker 运行） | 端口 6379 |

> ⚠️ **重要**：JDK 必须用 21。若 `JAVA_HOME` 指向 JDK 17，`mvn` 会报
> `UnsupportedClassVersionError`。JDK 21 位置：
> `C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot`

### 第 1 步：启动 MySQL 和 Redis

- **MySQL**（本机服务）：确保 Windows 服务里 MySQL 已运行，端口 3306
- **Redis**（Docker）：
  ```powershell
  # 启动 Docker Desktop 后执行
  docker compose up -d
  ```
  容器 `token-erp-redis` 自动重启（`restart: unless-stopped`），下次开机只需启动 Docker Desktop。

### 第 2 步：启动后端（端口 8080）

```powershell
cd D:\erp\backend
# 关键：显式指定 JDK 21
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
mvn spring-boot:run
```

启动成功标志：日志出现 `Started TokenErpApplication in X seconds`。

### 第 3 步：启动前端（端口 5173）

```powershell
cd D:\erp\frontend
npm install        # 首次需要，之后可跳过
npm run dev
```

启动后浏览器打开 **http://localhost:5173**

---

## 接口文档（Swagger UI）

后端启动后访问：

- **Swagger UI**：http://localhost:8080/swagger-ui/index.html
- **接口文档 JSON**：http://localhost:8080/v3/api-docs

在线调试步骤：先调用 `POST /api/auth/login` 拿到 token → 点右上角 **Authorize** 填入 `Bearer <token>` → 即可在线调用其他接口。

---

## 登录账号

| 用户名 | 密码 | 说明 |
|---|---|---|
| `admin` | `admin123` | 超级管理员，拥有全部权限 |

---

## 后台运行（不占用当前窗口）

```powershell
# 后端（后台）
cd D:\erp\backend
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
Start-Process mvn.cmd -ArgumentList "spring-boot:run","-DskipTests" -WindowStyle Hidden

# 前端（后台）
cd D:\erp\frontend
Start-Process npm.cmd -ArgumentList "run","dev" -WindowStyle Hidden
```

查看是否启动成功：
```powershell
Get-NetTCPConnection -LocalPort 8080,5173 -State Listen
```

---

## 数据库初始化（仅全新部署时）

建表脚本在 `backend/src/main/resources/sql/`，按顺序执行：

```
schema.sql → schema_biz.sql → schema_inventory.sql → init_data.sql
```

> 日常启动不需要此步骤。权限码和默认角色由后端 `DataInitializer` 启动时自动初始化。

---

## 常见坑

1. **JDK 版本报错** → 先设 `$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"`
2. **Redis 连不上** → 先启动 Docker Desktop，再 `docker compose up -d`
3. **端口被占用** → `Get-NetTCPConnection -LocalPort 8080` 查到 PID 后 `Stop-Process -Id <PID> -Force`
4. **前端改了代码没反应** → Vite 热更新，刷新浏览器即可

---

## 状态

- [x] 开发环境
- [x] 工程骨架
- [x] 数据库设计（48 张表）
- [x] 后端基础框架
- [x] 前端基础框架
- [x] 权限体系（角色-权限码校验）
- [x] 库存并发锁、幂等控制
- [x] 反审核、禁止自审
