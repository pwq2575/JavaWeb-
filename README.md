# 企业资源调度与协同治理平台 (ERS)

面向中小企业的轻量资源管理 Web 应用：**借物 → 续借/转交 → 归还** 的可追溯闭环，加上场地预约、采购审批、信誉机制与文化投票。

> 课程项目（Java Web）· 前后端分离 · 两周 MVP
>
> 仓库：`git@github.com:pwq2575/JavaWeb-.git`（`main` 分支）

---

## 1. 功能一览

| 模块 | 普通用户 | 管理员 |
|---|---|---|
| 账号 | 注册、登录、查看个人资料 | 用户增删改查、重置密码、分配角色 |
| 资产 | 浏览目录、借出、续借（≤3 次）、归还、发起转交 | 资产台账增删改查、导出 Excel |
| 转交 | 审批他人的转交申请（我持有的资产） | 同左 + 代发起 |
| 场地 | 单日 30 分钟切片预约、取消、标记已使用 | 场地增删改查 |
| 采购 | 提交申请（名称 + 理由）、删除已驳回申请 | 同意入库 / 驳回 |
| 投票 | 投票（每人一次不可改）、查看结果 | 创建草稿、发布、提前关闭 |
| 信誉 | 查看信誉分与扣分流水 | 配置信誉阈值（50/60/70/80） |
| 看板 | 我的工作台（待办、借用、预约） | 组织看板（KPI + 三图）、导出 |

规则要点：借期固定 3 天；逾期每日 -5（当天仅一次）；场地爽约一次性 -5；信誉 < 阈值时限制借高价值（≥500 元）资产；采购审批**不**自动生成资产（本期）。

---

## 2. 技术栈

| 层 | 选型 |
|---|---|
| 后端 | Java 17 · Spring Boot 3.2 · Spring MVC · Spring Security 6 + JWT · MyBatis-Plus 3.5 |
| 数据库 | MySQL 8（InnoDB / utf8mb4） |
| 前端 | Vue 3（`<script setup>`）· Vite 5 · Element Plus · Pinia · Vue Router 4 · Axios · ECharts 5 |
| 导出 | EasyExcel |
| 测试 | JUnit 5 · Spring Boot Test · Mockito · MockMvc |
| 部署 | Nginx 托管 `dist` + `java -jar` |

不使用 JSP；不依赖 Spring Cloud（单体规模，必要时可加网关演示）。

---

## 3. 目录结构

```
企业资源调度与协同治理平台/
├─ db/                    # 数据库脚本
│  ├─ schema.sql          # 建表（12 表）
│  └─ data.sql            # 种子数据（密码占位 __DEMO__）
├─ docs/                  # 全部设计与交付文档
│  ├─ *_PRD.md
│  ├─ *-ia-*.md
│  ├─ *-visual-scheme-*.md
│  ├─ *-数据库设计说明书-*.md
│  ├─ *-API接口契约-*.md
│  ├─ *-权限矩阵-*.md
│  ├─ *-部署运行手册-*.md
│  ├─ *-测试计划与用例-*.md
│  └─ *-文档索引与缺口清单-*.md
├─ ers-backend/           # 后端（规划中）
│  └─ src/main/java/com/ers/{controller,service,mapper,entity,dto,vo,security,config,common}
├─ ers-web/               # 前端（规划中）
│  └─ src/{api,router,stores,views,components,utils}
└─ .env.example           # 环境变量模板
```

---

## 4. 快速启动

### 4.1 数据库

```powershell
# 建库 + 账号（root 执行一次）
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS ers CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
mysql -uroot -p -e "CREATE USER IF NOT EXISTS 'ers_app'@'localhost' IDENTIFIED BY '<你的密码>';"
mysql -uroot -p -e "GRANT SELECT,INSERT,UPDATE,DELETE,CREATE,ALTER,INDEX,DROP,REFERENCES ON ers.* TO 'ers_app'@'localhost'; FLUSH PRIVILEGES;"

# 导入（mysql 客户端读不了中文路径 → 先复制到 ASCII 临时路径）
Copy-Item .\db\schema.sql "$env:TEMP\ers_schema.sql" -Force
Copy-Item .\db\data.sql   "$env:TEMP\ers_data.sql"   -Force
mysql -uers_app -p ers -e "source $env:TEMP/ers_schema.sql"
mysql -uers_app -p ers -e "source $env:TEMP/ers_data.sql"
```

### 4.2 后端（8080）

```powershell
cd ers-backend
# 需要 JDK 17+ 与 Maven 3.9+；IDEA 可直接打开 pom.xml
mvn spring-boot:run
# 接口文档：http://localhost:8080/swagger-ui.html
```

### 4.3 前端（5173）

```powershell
cd ers-web
pnpm install
pnpm dev
# http://localhost:5173  （Vite proxy 将 /api 转发到 8080）
```

### 4.4 演示账号

| 账号 | 密码 | 角色 | 用途 |
|---|---|---|---|
| admin | 123456 | 管理员 | 全部管理功能 |
| zhangsan | 123456 | 普通用户 | 正常借用、有转交待审批 |
| lisi | 123456 | 普通用户 | 有逾期记录，信誉 85 |
| wangwu | 123456 | 普通用户 | 信誉 55，借高价值被拒的演示 |
| zhaoliu | 123456 | 普通用户 | 持有资产 |

> 密码由后端 `DataInitializer` 在首次启动时把 `__DEMO__` 替换为 BCrypt 密文；正式部署请修改。

---

## 5. 文档

| 文档 | 说明 |
|---|---|
| [PRD](docs/企业资源调度与协同治理平台_PRD.md) | 需求、边界、决策记录（D1–D15） |
| [IA 信息架构](docs/企业资源调度与协同治理平台-ia-2026-11-17.md) | 站点地图、23 页、用户路径、内容模块、交互要求 |
| [视觉方案](docs/企业资源调度与协同治理平台-visual-scheme-2026-11-17.md) | Design Tokens、组件氛围、动效、可访问性 |
| [数据库设计](docs/企业资源调度与协同治理平台-数据库设计说明书-2026-11-17.md) | ER、字段字典、状态机、并发策略 |
| [API 契约](docs/企业资源调度与协同治理平台-API接口契约-2026-11-17.md) | 53 个接口、统一响应、错误码 |
| [权限矩阵](docs/企业资源调度与协同治理平台-权限矩阵-2026-11-17.md) | 页面/接口权限、数据范围、越权测试清单 |
| [部署手册](docs/企业资源调度与协同治理平台-部署运行手册-2026-11-17.md) | 环境准备、启动、排错、演示脚本 |
| [测试计划](docs/企业资源调度与协同治理平台-测试计划与用例-2026-11-17.md) | 用例、边界、冒烟、验收对照 |
| [文档索引](docs/企业资源调度与协同治理平台-文档索引与缺口清单-2026-11-17.md) | 必要/可选文档、环境缺口、待确认项 |

---

## 6. 课程要求对照

| 要求 | 落地 |
|---|---|
| Java 技术栈 | Spring Boot 3 + Spring MVC + MyBatis-Plus |
| 多层架构 | controller → service → mapper；前端 views → api → stores |
| 数据库文件存储 | MySQL 8 数据目录 `.ibd` + `mysqldump` 备份 |
| 管理员/普通用户两类 | `role = ADMIN / USER`，权限矩阵与双重校验 |
| 管理员增删查改 | 用户、资产、场地、投票全 CRUD；采购审批 |
| 普通用户浏览/下载/部分操作 | 资产目录、个人记录导出、借用/预约/投票 |
| 注册后使用 | 注册仅产生 USER；Spring Security 过滤链 |
| 安全性 | JWT、BCrypt、参数校验、SQL 注入/XSS/CSRF/越权控制 |
| 界面与交互 | Element Plus + 自定视觉令牌；列表四段式、弹层审批、局部刷新 |

---

## 7. 开发约定

- 分支：`main` 保护；功能分支 `feat/xxx`。
- 提交：`feat|fix|docs|chore: 描述`。
- 接口变更先改 `docs/...-API接口契约`，再写代码。
- 表结构变更同步 `db/schema.sql` 与《数据库设计说明书》。
- 禁止硬编码阈值（60/500/3/3），一律读 `sys_config`。
