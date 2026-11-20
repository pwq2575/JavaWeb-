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
| 资产 | 浏览目录、借出、续借（≤3 次）、归还、申请接手（对他人持有的资产） | 资产台账增删改查、导出 Excel |
| 转交 | B 申请接手 → 持有者 A 在「转交审批」确认（同意后换人不换归还日） | 同左；**无「代发起」**，审批权只属于当前持有者 |
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
├─ ers-backend/           # 后端（✅ 10 个模块 / 53 个接口全部实现，接口冒烟 79/79 通过）
│  ├─ pom.xml             # Spring Boot 3.2.5 / Java 17 / MyBatis-Plus / JWT / EasyExcel
│  └─ src/main/java/com/ers/{common,config,security,auth,user,asset,transfer,credit,room,procurement,vote,admin,export,job}
├─ ers-web/               # 前端（✅ Vue 3 + Vite + Element Plus，20 个页面/组件全部实现，构建通过）
│  └─ src/{api,router,stores,views,utils,styles,layout}
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

# 导入（mysql 客户端读不了中文绝对路径 → 切到 db 目录后用相对文件名，实测可用）
cd db
mysql -uers_app -p --default-character-set=utf8mb4 ers -e "source schema.sql"
mysql -uers_app -p --default-character-set=utf8mb4 ers -e "source data.sql"
cd ..
```

### 4.2 后端（8080）

```powershell
cd ers-backend
# 需要 JDK 17+ 与 Maven 3.9+；IDEA 可直接打开 pom.xml
mvn spring-boot:run
# 接口文档：http://localhost:8080/swagger-ui.html
```

> **我的机器上已装好 Maven 3.9.9**（`C:\Users\Lenovo\tools\apache-maven-3.9.9`，已加入用户 PATH）。
> 如果新开的终端仍提示 `mvn 不是可运行的程序`：说明该窗口是在安装前打开的，**关掉重开**即可；
> 不想重开就在当前窗口先执行：
> ```powershell
> $env:Path += ";C:\Users\Lenovo\tools\apache-maven-3.9.9\bin"
> mvn -v   # 应显示 Apache Maven 3.9.9
> ```

启动成功后控制台会打印（Spring Boot 自身不输出地址，这里由 `ErsApplication.startupBanner` 补上）：

```
====================================================================
  ERS 后端启动完成  (profile: dev)
  应用地址     : http://localhost:8080
  接口文档     : http://localhost:8080/swagger-ui.html
  OpenAPI JSON : http://localhost:8080/v3/api-docs
  停止服务     : Ctrl + C
====================================================================
```

> **中文日志乱码**：中文 Windows 控制台代码页是 936（GBK），而 Spring Boot 默认用 UTF-8 写控制台。项目已在 `application.yml` 的 `logging.charset.console` 默认设为 `GBK`（可用环境变量 `LOG_CONSOLE_CHARSET` 覆盖）。若你的终端是 UTF-8（执行过 `chcp 65001`），启动前设 `LOG_CONSOLE_CHARSET=UTF-8`。

### 4.3 前端（5173）

```powershell
cd ers-web            # 必须先进 ers-web！在仓库根目录跑 npm 会报 ENOENT: package.json
npm install           # 首次；包已锁定 package-lock.json
npm run dev           # http://localhost:5173  （Vite proxy 将 /api 转发到 8080）
npm run build         # 生产构建，产物在 ers-web/dist（Nginx 托管）
```

> **统一用 npm，不要用 pnpm**：本机 pnpm 会在依赖链接阶段报 `stack overflow` 或 `ERR_PNPM_IGNORED_BUILDS`（esbuild/vue-demi 构建脚本被拦），已移除 `pnpm-lock.yaml`、`pnpm-workspace.yaml` 与 `.npmrc`。若之前用 pnpm 装过，先删掉 `ers-web/node_modules` 再 `npm install`。
> npm 12 会提示 `install-scripts ... blocked`（esbuild 的二进制来自 `@esbuild/win32-x64` 可选依赖，不影响构建/启动，可忽略）。
> 如遇 `esbuild spawn EPERM`，说明 node 子进程被安全软件拦截，关闭拦截或在普通终端重跑即可。

前端页面清单（与 IA §4 一一对应）：

| 区域 | 路由 | 页面 |
|---|---|---|
| 公共 | `#/login`、`#/register` | 登录（含演示账号一键填充）、注册 |
| 员工 | `#/home` | 我的工作台：4 KPI + 在借 + 待审批转交 |
| 员工 | `#/assets`、`#/assets/:id` | 资产台账（四段式列表）、资产详情（状态卡 + 时间线 + 动作栏） |
| 员工 | `#/transfers` | 转交审批（incoming/outgoing、同意/驳回） |
| 员工 | `#/rooms` | 场地预约（18 切片网格、我的预约） |
| 员工 | `#/procurements` | 采购申请（提交/审批/删除已驳回） |
| 员工 | `#/votes`、`#/votes/:id` | 投票列表（卡片）、投票详情（投前隐藏票数） |
| 员工 | `#/my/borrows`、`#/my/credit` | 我的借用（续借/归还/导出）、我的信誉（流水） |
| 管理 | `#/admin/dashboard` | 看板：8 KPI + 4 图（类别价值/状态分布/借用趋势/场地使用） |
| 管理 | `#/admin/assets` | 资产 CRUD + 停用/启用 + 导出台账 |
| 管理 | `#/admin/users` | 用户 CRUD + 重置密码 |
| 管理 | `#/admin/rooms` | 场地 CRUD |
| 管理 | `#/admin/config` | 策略配置（信誉阈值/高价值阈值/借期/续借上限） |

> **前端状态**：`npm run build` 已通过（2266 个模块，dist 约 2.7 MB，已按 vue / element-plus / echarts 分包）；
> `node scripts/check-sfc.mjs` 对 20 个 SFC + 6 个 JS 做语法校验全通过。
> 本机若命令被沙箱拦截，可在普通终端执行 `npm run dev` 后浏览器验证。

### 4.4 演示账号

| 账号 | 密码 | 角色 | 用途 |
|---|---|---|---|
| admin | 123456 | 管理员 | 全部管理功能 |
| zhangsan | 123456 | 普通用户 | 正常借用、有转交待审批 |
| lisi | 123456 | 普通用户 | 有逾期记录，信誉 85（启动后实为 75，见下） |
| wangwu | 123456 | 普通用户 | 信誉 55，借高价值被拒的演示 |
| zhaoliu | 123456 | 普通用户 | 持有资产 |

> 密码由后端 `DataInitializer` 在首次启动时把 `__DEMO__` 替换为 BCrypt 密文；正式部署请修改。
>
> **启动后的实际信誉分**：种子数据里 asset1（张三，due 2026-11-19）与 asset3（李四，due 2026-11-14）已过期，服务启动时的逾期扫描会对持有人各扣 5 分，因此实际为 **zhangsan 90 / lisi 75**（另有爽约记录同样在启动时结算）。演示要"原始分数"时，请先重置库再当天演示；分数每天还会随逾期继续下降，这是业务规则而非缺陷（见开发日志 §7.2 L-03）。

### 4.5 后端验证（已实测通过）

```powershell
# 1) 登录：返回 code=0、token、user（有效期 7200 秒）
curl.exe -s -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{\"username\":\"admin\",\"password\":\"123456\"}'

# 2) 带令牌取当前用户
curl.exe -s http://localhost:8080/api/auth/me -H "Authorization: Bearer <上一步的 token>"

# 3) 不带令牌访问 /api/auth/me → HTTP 401 + {"code":40100,...}
# 4) 密码错误 → HTTP 401 + {"code":40100,"message":"账号或密码错误"}
```

已验证项：登录签发 JWT、`/me` 鉴权、401 统一响应体、注册（密码强度与唯一性校验）、启动时种子密码 BCrypt 化。

### 4.6 后端接口冒烟（79 项全通过）

| 模块 | 覆盖 | 结果 |
|---|---|---|
| 认证 | 5 个种子账号登录、无令牌 401、密码错误 401 | ✅ |
| 资产 | 列表/关键字/状态/分类筛选、详情+流转记录、借出、高价值信誉拦截、续借 3 次上限、归还、管理端增删改停用 | ✅ |
| 转交 | 申请接手（`{assetId, applyReason}`，接收人由令牌推出）、incoming 待办、持有者自申请 422、在库 422、防重复 409、非持有者审批 403、审批换人不换期、驳回后可再申请 | ✅ |
| 场地 | 3 间场地、18 切片/未来 14 天、重复订 409、超期 422、取消/使用时序、我的预约 | ✅ |
| 采购 | 提交、同意、驳回（理由必填）、重复审批 409、软删除 | ✅ |
| 投票 | 投前隐藏票数、投票、重复 409、关闭后 422、管理员草稿→发布→关闭 | ✅ |
| 我的 | 借用记录、预约、信誉分与阈值、工作台聚合 | ✅ |
| 管理端 | 看板 KPI/趋势/分布、用户 CRUD+重置密码、场地 CRUD、阈值配置校验 | ✅ |
| 导出 | 资产台账 xlsx（管理员）、个人借用记录、越权 403 | ✅ |

> 冒烟脚本在本机 `.tmp/smoke_all.ps1`（未入库）。重置数据的顺序见《部署运行手册》第 6 节：先 `schema.sql` 再 `data.sql`，**必须先启动应用**让 `DataInitializer` 把 `__DEMO__` 转成 BCrypt。

### 4.7 后端自动化回归（`mvn test`，56 例 / 17 个测试类）

同一批断言已迁移为可入库的集成测试，跑在独立库 `ers_test` 上，**不会写演示库 `ers`**：

```bash
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS ers_test DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_0900_ai_ci; GRANT ALL ON ers_test.* TO 'ers_app'@'localhost';"
mvn -f ers-backend/pom.xml test      # 沙箱内需加 -Dmaven.repo.local=...\.tools\m2repo
```

| 测试类（`ers-backend/src/test/java/com/ers/`） | 断言/用例 | 覆盖 |
|---|---|---|
| `auth/LoginSmokeTest` | 5 | 登录、401、密码错误 |
| `room/RoomReservationSmokeTest` | 11 | 切片、重复 409、取消、使用、我的预约 |
| `transfer/TransferSmokeTest` | 9 | 转交主链路、驳回、列表 |
| `transfer/D01TransferSemanticsTest` | 7 | D-01 语义（申请/自申请 422/在库 422/重复 409/越权审批 403/换人不换期/驳回后再申请） |
| `procurement/ProcurementSmokeTest` | 10 | 采购申请、审批、驳回、删除 |
| `vote/VoteSmokeTest` | 14 | 投前隐藏、投票、重复、发布/关闭 |
| `my/MyViewSmokeTest` | 3 | 借用、预约、信誉、工作台 |
| `admin/AdminSmokeTest` | 21 | 看板、用户、场地、配置 |
| `asset/AssetAdminSmokeTest` | 2 | 资产增改停用 |
| `export/ExportSmokeTest` | 4 | 导出权限与文件类型 |
| `job/CreditJobTest` | 1 | 逾期扣 5、同日幂等、下限 0 |
| `asset/AssetServiceUnitTest`（纯 Mockito） | 12 | 借出/续借/归还守卫、信誉阈值、转交互斥 |
| `transfer/TransferServiceTest`（纯 Mockito） | 11 | 申请/审批/驳回守卫与 `from=A、to=B` 落库约定 |
| `asset/AssetFilterTest` | 4 | `holderId`/`overdue`/`highValueOnly`/`sort` 筛选与排序 |
| `procurement/ProcurementAuthzTest` | 2 | 采购审批仅管理员可操作（越权 403） |
| `procurement/ProcurementSelfApprovalTest` | 4 | 自审未勾选 422 / 勾选后 `selfApproved=true` / 审批他人不置位 / 自审驳回同样需勾选 |
| `auth/TokenInvalidationTest` | 2 | 账号停用后旧令牌立即失效 |

共 56 个 `@Test`（33 个 MockMvc + 23 个纯单测），`mvn test` 全绿。

一键回归（后端全量测试 + 前端构建，自动带上本机必需的 `-Dmaven.repo.local` 与 `ESBUILD_BINARY_PATH`）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\ci.ps1
```

> 脚本退出码 0 = 两段都成功；日志写到 `.tmp\ci_backend.log` 与 `.tmp\ci_frontend.log`。`mvn` 的 `exit code` 在 PowerShell 管道下不可靠（日志 §6.13），脚本以日志中的 `BUILD SUCCESS` / `built in` 判定。

隔离机制：`ers-backend/src/test/resources/application-test.yml` 把 datasource 指向 `ers_test`，`spring.sql.init` 直接复用 `../db/schema.sql` 与 `../db/data.sql`；`BaseMockMvcTest` 每个用例前重建种子态，并先断言 `select database()` 为 `ers_test` 才继续。

### 4.8 三条典型操作路径（替代独立用户手册）

**路径 A：借用 → 续借 → 归还**

1. `#/login` 用 zhangsan/123456 登录 → `#/assets` → 选一件 `IN_STOCK` 资产（如 A-0202）→ 详情页「借出」。
2. 成功判据：资产状态变 `BORROWED`、应归还日 = 今天 + 3 天、`#/my/borrows` 出现记录。
3. 详情页或「我的借用」点「续借」→ 应归还日再 +3、续借次数 +1，满 3 次提示 422。
4. 「归还」→ 状态回 `IN_STOCK`，流转记录多一条 `RETURN`。
5. 反例：wangwu（信誉 55）借高价值资产（如 A-0201，3800 元）→ 40301「信誉分不足」。

**路径 B：申请接手（B 申请、A 审批，即 D-01 定稿语义）**

1. zhangsan 登录，打开李四持有的 A-0203（`#/assets/3`）→ 动作栏出现「申请接手」（仅"被他人借出"时出现）。
2. 填说明提交 → 提示等待持有者确认；详情页出现「张三 申请接手这件资产（现持有者 李四）」。
3. 换 lisi 登录 → `#/transfers` →「待我确认（1）」→「同意」。
4. 回到 zhangsan：A-0203 的持有人变为张三，**应归还日不变**（2026-11-14），流转记录新增 1 条 `TRANSFER`（操作人李四、目标张三）。
5. 反例：lisi 打开自己的资产没有「申请接手」按钮；同一资产在待审批期间再次提交 → 409。

**路径 C：预约场地与 409 冲突处理**

1. `#/rooms` 选场地 + 日期 → 点一个空闲切片 → 成功提示，切片变已预约。
2. 换另一个账号在同一场地/日期/时段下单 → 409「该时段已被预约，请刷新后重选」。
3. 前端自动刷新网格并**保留已选场地/日期**，直接改选其它切片即可。
4. 取消：「我的预约」里取消后切片释放，可重新预订。

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

- **文档优先（D-01 教训）**：业务语义一律以 `docs/..._PRD.md` 与 `docs/...-API接口契约` 为唯一权威。实现任何「谁发起 / 谁审批 / 谁可见」类动作前，先回读对应章节并在开发日志写一句结论，再动代码与前端文案；若文档与需求冲突，先提确认、改文档，再改代码。
- 分支：`main` 保护；功能分支 `feat/xxx`。
- 提交：`feat|fix|docs|chore: 描述`。
- 接口变更先改 `docs/...-API接口契约`，再写代码。
- 表结构变更同步 `db/schema.sql` 与《数据库设计说明书》。
- 禁止硬编码阈值（60/500/3/3），一律读 `sys_config`。
