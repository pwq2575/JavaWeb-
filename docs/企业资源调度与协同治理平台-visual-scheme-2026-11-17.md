# 视觉方案 (Visual Scheme) — 企业资源调度与协同治理平台 (ERS)

## 0. 元数据

| 字段 | 值 |
|---|---|
| 文档类型 | Visual Scheme（Design Pipeline Contract v1） |
| 版本 | v1.0 — **Frozen**（2026-11-17 直接敲定，不再逐轮确认） |
| 日期 | 2026-11-17 |
| 作者 | Visual Explorer (Agent) |
| source_docs | `docs/企业资源调度与协同治理平台_PRD.md`（v1.3）<br>`docs/企业资源调度与协同治理平台-ia-2026-11-17.md`（v1.3）<br>PRD §5 Benchmark References（reference-led mode 输入） |
| 技术前提 | 前后端分离：Vue 3 + Vite 5 + **Element Plus** + Pinia + Vue Router 4 + Axios；ECharts 5 |
| recommended_direction | **D1「Signal Ledger 信号台账」** |
| selected_direction | **D1「Signal Ledger 信号台账」**（用户 m00742「继续给出并敲定视觉方案」授权直接冻结） |
| 下游消费者 | `motion-concept`、`resource-inventory`、`figma-design-bridge`、`frontend-design`、开发 sprint |
| 目录假设 | `docs/{project}/` 不存在，沿用 IA 的回退策略保存为 `docs/企业资源调度与协同治理平台-visual-scheme-2026-11-17.md` |

---

## 1. Art Direction

### 1.1 概念：Signal Ledger —「信号台账」

这不是一个"再普通不过的 SaaS 后台"。ERS 的产品本质是 **责任的可追溯**：谁借走了什么、几点该还、谁把东西转给了谁、哪次预约爽约了。因此视觉母题取「**台账**」而非「仪表盘」——纸面清单的秩序感 + 状态信号的即时性。

- **纸**：内容区是接近纸白的 `#FFFFFF` 与极浅冷灰 `#F5F7FA`，信息靠 **1px 发丝线**和留白分区，不靠一层层卡片阴影堆叠。
- **墨**：左侧导航是深墨蓝 `#101A2E`，像一本合上的硬壳台账；它让"工作区"与"导航"在视觉上截然分层，也让 7 项主导航不需要任何图标以外的装饰。
- **信号**：状态是本产品最核心的信息（在库 / 借出 / 逾期 / 爽约 / 已入库）。所有状态统一走**「信号胶囊」**：4px 圆角、1px 同色描边、8% 同色底、正文级深色字，在列表、详情、看板、时间线里同一套语法。
- **数字**：编号、金额、日期、计数、分值一律 `JetBrains Mono` 等宽数字——台账的可信感来自数字对齐。

### 1.2 Visual Manifesto

> 我们不做"数据可视化的大屏"，我们做**一张可以签字的工作台账**。
> 每一行有责任人，每一格有状态，每一次变更留下痕迹。
> 界面不喧哗：去掉渐变、去掉光球、去掉玻璃拟态、去掉满屏圆角卡片；
> 留下的是精确的列宽、对齐的数字、克制的色块，和一条能把责任链讲清楚的时间线。

### 1.3 Signature Moment（记忆钩子）

**「状态点亮」**：进入资产详情页时，顶部状态胶囊与右侧流转时间线的第一个事件点，在 **320ms** 内以 `cubic-bezier(0.16, 1, 0.3, 1)` 同步做一次极轻的"点亮"（描边由 24% 不透明度 → 100%，节点外环 1 次 12px 扩散后消失）。用户记住的不是动画本身，而是"这台设备现在处于什么状态、上一秒发生了什么"被一次性说清。

### 1.4 Memorable Differentiator

**等宽数字 + 发丝线台账**：全站所有可变数字（`#A-0217`、`¥1,280.00`、`2026-11-20`、`剩余 2 天`、信誉分 `85`）使用同一等宽字体与 `tabular-nums`；表格行与详情字段之间只用 1px `#EDF1F6` 分隔，不使用斑马纹。它的可替代性极低——一旦换成常规无衬线数字与卡片流，产品立刻退回"模板后台"。

### 1.5 审美复杂度匹配

| 维度 | 判断 |
|---|---|
| 视觉野心 | 中高：不靠动效与素材，靠**排版精度、密度、状态语法**建立高级感 |
| 实现复杂度 | 低—中：CSS-only 令牌覆盖 Element Plus + 1 个 ECharts 主题 + 3 个自绘 SVG 空态 |
| 素材成本 | 极低：0 图片、0 视频、0 WebGL；仅 Logo 字标、图标（Element Plus Icons）、3 张空态线稿 |
| 性能预算 | 首屏 JS ≤ 250KB gzip（Vue+EP 按需）；无大图；动画全部 transform/opacity，主线程零阻塞 |

---

## 2. Reference Translation（reference-led）

### 2.1 重新打开 URL 的结果（2026-11-17）

| URL | 本次访问结果 | 视觉证据可得性 |
|---|---|---|
| https://demo.snipeitapp.com | HTTP 200，落在 `/login`（页面标题 `Snipe-IT Asset Management Demo`，表单为 Bootstrap 风格登录卡，公开账号 `admin/password`） | **仅登录页**；进入后台仍需会话，`/hardware` 等 302→`/login`。后台视觉细节标 `inferred` |
| https://itam.ciyocloud.cn/ | HTTP 200，仅返回 SPA 壳（`CIYO 资产管理系统`，1730 B），无内联样式与路由内容 | 仅能确认「Vue 3 + Element Plus SPA」；配色/排版标 `inferred` |
| Element Plus 官方 token 源 `theme-chalk/src/common/var.scss` | HTTP 200，取得**真实默认值**（见 2.2） | 高：本方案的组件基线直接由此推导 |

> 因两站均在登录墙后，本方案按 visual-explorer 的 fallback 规则处理：**不虚构截图**，凡未实证的视觉特征一律标 `inferred`，并只用"策略级"结论。

### 2.2 Element Plus 默认令牌（实证，本方案的基线）

| 类别 | 默认值（实证） | 本方案处置 |
|---|---|---|
| 主色 | `#409EFF` | **替换**为 `#2B5CE6`（默认蓝过于"组件库"，缺乏台账气质） |
| 成功/警告/危险/信息 | `#67C23A` / `#E6A23C` / `#F56C6C` / `#909399` | **全部替换**为更深的语义色（12px 标签需 AA） |
| 文字 | `#303133` / `#606266` / `#909399` / `#A8ABB2` | 替换为冷调墨色阶 `#141B2D → #A7B0C0` |
| 边框 | `#DCDFE6` / `#E4E7ED` / `#EBEEF5` | 替换为 `#E2E7EF` / `#EDF1F6` |
| 页面底色 | `#F2F3F5` | 替换为 `#F5F7FA` |
| 圆角 | `4px`（base） | 保留 4px 用于按钮/输入；卡片 6px、弹层 8px |
| 字号 | base 14 / small 13 / xs 12 | 保留（中文在 12px 以下不可读） |
| 组件高度 | 32px（default） | 保留 32；表格行 40px；筛选栏控件 32px |
| 表格 | cell padding `0 12px`，row `8px 0`，header 字色 `#909399` | 保留骨架，header 改 `#6B7688` + 12px/500 + 0.02em |
| 菜单 | item height `56px`，active 用 primary | 侧栏自定义（深底浅字，active 为品牌色 12% 底 + 2px 左侧竖条） |
| 过渡 | `0.3s cubic-bezier(0.645,0.045,0.355,1)`；fast `0.2s cubic-bezier(0.23,1,0.32,1)` | **替换**为 4 档自有曲线（见 §6） |
| 阴影 | 多层大阴影（0 12px 32px 4px rgba(0,0,0,.04)…） | 替换为 3 档克制阴影 |

### 2.3 从两个参考站转译什么

| 参考站 | 观察（置信度） | 转译到哪里 | 禁止复制 |
|---|---|---|---|
| Snipe-IT | 状态是导航的一部分：侧栏下挂各状态计数（inferred）；资产详情以「状态 / 上次签出 / 预计归还」三卡开场（inferred，依据 GitHub `resources/views/hardware/view.blade.php`）；动作按钮只在满足条件时出现 | §3 列表页工具条与状态切片、§8 详情页首屏三信息块、按钮条件渲染规则 | 其 Bootstrap 皮肤、蓝色系、Logo、文案、图标风格 |
| Snipe-IT | 流转历史为倒序事件表（时间/动作/操作人/目标） | 详情页「流转时间线」的信息结构（事件点 + 元信息） | 其表格样式与色带 |
| 西柚 ITAM | 一级导航 10 项平铺、分组与单页混排；双首页（`/home` 个人工作台 + `/itam/dashboard` 组织看板）；列表四段式 | 导航收敛为「7 + 1」的层级策略、工作台与看板的信息密度分工、列表四段式（筛选→工具条→表格→分页） | 其 10 项导航本身、菜单图标、品牌绿、页面文案 |
| 西柚 ITAM | 前端为 Vue 3 + Element Plus（实证：SPA 壳 + bundle） | 说明本方案选择 Element Plus 作为组件底座是**与参考站同生态**，令牌替换成本可控 | 其主题定制值、布局间距、图表配色 |
| 共同 | 状态用中文直出、状态胶囊在列表与详情重复出现 | 状态字典驱动的「信号胶囊」语法 | 其硬编码色值与确切文案 |

### 2.4 转译代价

- 覆盖 Element Plus 共 **19 个 CSS 变量**（颜色/圆角/字体/表格），不改组件源码，升级安全。
- ECharts 主题需自建（约 80 行 JSON），否则图表会带默认蓝绿，破坏台账气质。
- 自绘 3 张空态线稿（资产箱 / 日历网格 / 票箱），约 0.5 人日。

---

## 3. Visual Direction Matrix Spec

> 三个方向都**来自参考站推荐方向**（Element Plus 后台 + 状态驱动信息结构），彼此是受控变体而非无关风格。三块 P0 切片：**P-HOME-01 工作台**、**P-ASSET-01 资产台账列表**、**P-ASSET-02 资产详情**。

### D1 — Signal Ledger「信号台账」（recommended & selected）

- **art direction**：深墨蓝导航 + 纸白内容；发丝线分区；信号胶囊状态语汇；等宽数字台账。克制的编辑部式秩序。
- **tokens 摘要**：primary `#2B5CE6`；ink `#141B2D`；page `#F5F7FA`；sidebar `#101A2E`；success `#0A7A57`；warning `#A85B00`；danger `#B3261E`；radius 4/6/8；motion 120/180/240ms。
- **layout mood**：240px 深色侧栏 + 顶栏 56px + 内容 24px 内边距；内容首屏是"标题行 + 一行 KPI 数字 + 主表"，不做卡片瀑布。
- **component atmosphere**：按钮方而窄（32px 高，8/14 内边距）；表格 header 浅、行发丝线、hover 仅底色 `#F7F9FC`；状态胶囊 4px 圆角带描边；弹层 8px 圆角 + 1px 边框 + 单层阴影。
- **asset implications**：0 图片；Element Plus Icons；3 张空态线稿；Logo 字标。
- **restraint notes**：全站不使用渐变背景、不使用玻璃拟态、不使用彩色大色块；同一屏最多 3 种语义色。
- **frontend_signature**：等宽数字 + 发丝线表格 + 状态胶囊点亮（CSS-only + WAAPI 计数器，无第三方动效库）
- **implementation_complexity**：**CSS-only**（+ 1 个 ECharts 主题、3 张 SVG）

### D2 — Quiet Grid「静默网格」（受控变体）

- **art direction**：全浅色、无深色侧栏；瑞士网格；以基线网格线与字重对比分区，几乎无阴影与圆角（2px）；单色蓝 + 语义色极小面积点缀。更接近"咨询报告"而非"后台"。
- **tokens 摘要**：primary `#1F4FD8`；surface 全白；border `#E6E8EC`；radius 2px；字号对比更强（12/14/18/32）；无阴影。
- **layout mood**：侧栏白底 + 1px 右边线；内容用 12 列网格与 32px 基线；表格行高 44px 更松。
- **component atmosphere**：按钮纯文字或 1px 描边；标签无底色只有文字与左侧 3px 竖条；图表只用 2 色。
- **asset implications**：同 D1，但空态改用纯排版（大号数字 + 一句话）。
- **restraint notes**：禁止任何底色块超过屏宽 20%；禁止阴影。
- **frontend_signature**：网格线可见（1px 参考线贯穿页面结构与表格列）
- **implementation_complexity**：CSS-only

### D3 — Daylight Warehouse「仓储白昼」（受控变体）

- **art direction**：暖白 `#FAF8F4` 纸面 + 赭陶 `#B4593A` 主色 + 墨绿状态；圆角 8px，柔和投影，更"人"、更贴近企业文化投票场景。
- **tokens 摘要**：primary `#B4593A`；ink `#221E1A`；surface `#FAF8F4`；success `#2F6B4F`；radius 8/10；阴影更软。
- **layout mood**：导航浅米色，内容区留白更大（32px），KPI 用大号数字 + 一句人类可读的说明。
- **component atmosphere**：按钮 999px 胶囊；标签无描边仅 12% 底；图表暖色系。
- **restraint notes**：暖色仅用于强调与投票相关页面；资产/审批仍以中性色为主，避免整站变"咖啡厅"。
- **frontend_signature**：大号衬线数字（如 `Fraunces`）+ 暖纸底
- **implementation_complexity**：CSS-only + 额外字体（+1 字体请求）

### 切片对照矩阵（3 方向 × 3 个 P0 切片）

| 切片 | D1 Signal Ledger | D2 Quiet Grid | D3 Daylight Warehouse |
|---|---|---|---|
| **P-HOME-01 工作台** | 深墨侧栏 + 纸白内容；4 个 KPI 用 mono 28px 数字 + 发丝线分隔；今日待办/临期资产两张 6px 圆角卡；状态胶囊行内 | 白侧栏 + 1px 右边线；KPI 数字改 Sora 600 32px；无卡片边框，靠 32px 基线与参考线分区；状态以文字 + 3px 竖条表达 | 米白底 + 赭陶强调；KPI 大号数字配一句人类说明（"本周归还 12 件"）；卡片 8px 圆角 + 柔影；投票入口用赭陶实心按钮 |
| **P-ASSET-01 资产台账列表** | 四段式；header `#F7F9FC`；行 40px 发丝线；状态切片 Tab 带计数徽标；操作列右侧固定文本按钮；逾期行左 2px 红条 | 行 44px、无斑马线无底色，列间 1px 竖参考线；工具条按钮纯文字；状态为 12px 字 + 左侧 3px 竖条 | 行 44px、暖白底；状态胶囊 999px；工具条主按钮赭陶实心、次按钮描边；表格圆角外框 8px |
| **P-ASSET-02 资产详情** | 左 8 列「三信息块 + Tabs（详情/流转/借用规则）」+ 右 4 列 sticky 动作栈与时间线；状态点亮 320ms；mono 编号/日期 | 左右 6:6 对称；Tabs 改为顶部下划线式；时间线仅文字节点，无节点色；几乎无按钮底色 | 上部大号标题 + 一句话状态叙述；信息用两列定义列表；时间线为暖色点线；按钮胶囊化 |

**结论**：D2 缺乏状态信号的表现力（状态标签被压成文字竖条，扫描效率下降）；D3 的暖色与企业治理语义（责任、审计）不匹配，且引入第二套字体成本。**选 D1**，并在 D1 内吸收 D2 的"网格克制"与 D3 的"投票页人文感"作为局部变奏（投票活动页允许 1 处大号衬线标题，仅此一处）。

---

## 4. Design Tokens

### 4.1 颜色（浅色为主态）

| Token | Hex | 用途 | 对比度（白底） |
|---|---|---|---|
| `--color-ink-900` | `#141B2D` | 主文本、表格主值 | 15.8:1（对 `#F5F7FA`）✅ AAA |
| `--color-ink-700` | `#3C4659` | 正文、表单标签 | 9.6:1 ✅ AAA |
| `--color-ink-500` | `#6B7688` | 次要文本、表头、说明 | 4.7:1 ✅ AA |
| `--color-ink-300` | `#A7B0C0` | placeholder、禁用文本 | 2.4:1（仅限禁用态，不承载信息） |
| `--color-surface` | `#FFFFFF` | 卡片、表格、弹层 | — |
| `--color-surface-page` | `#F5F7FA` | 页面底 | — |
| `--color-surface-subtle` | `#F0F3F8` | 表头底、筛选条、只读区块 | — |
| `--color-sidebar` | `#101A2E` | 左侧导航底 | — |
| `--color-sidebar-hover` | `#18243C` | 导航 hover | — |
| `--color-sidebar-active` | `rgba(66,106,255,0.14)` | 导航选中底 | — |
| `--color-border` | `#E2E7EF` | 主边框 | — |
| `--color-border-light` | `#EDF1F6` | 表格行分隔、卡片内分隔 | — |
| `--color-primary` | `#2B5CE6` | 主按钮、链接、选中 | 5.6:1 ✅ AA |
| `--color-primary-hover` | `#1F49C7` | hover | — |
| `--color-primary-active` | `#183BA3` | active | — |
| `--color-primary-tint` | `#EEF3FE` | 选中行、次级按钮底 | — |
| `--color-success` | `#0A7A57` | 在库、成功、已归还 | 5.3:1 ✅ AA |
| `--color-success-tint` | `#E8F4EF` | 状态胶囊底 | — |
| `--color-warning` | `#A85B00` | 借出、待处理、临近到期 | 5.0:1 ✅ AA |
| `--color-warning-tint` | `#FCF2E5` | 状态胶囊底 | — |
| `--color-danger` | `#B3261E` | 逾期、爽约、驳回 | 6.5:1 ✅ AA |
| `--color-danger-tint` | `#FBEDEC` | 状态胶囊底 | — |
| `--color-info` | `#4A5A72` | 中性状态、元信息 | 7.4:1 ✅ AAA |
| `--color-info-tint` | `#EEF1F6` | 状态胶囊底 | — |
| `--color-focus-ring` | `rgba(43,92,230,0.35)` | 焦点环（2px 外扩） | — |

**语义状态字典（与 IA §6.0 S1、PRD 4.2 对齐）**

| 状态 | 文本 | 色 token | 胶囊样式 |
|---|---|---|---|
| 在库 | 在库 | success | 绿底绿描边绿字 |
| 借出 | 借出 · 剩余 N 天 | warning | 琥珀底琥珀描边琥珀字；≤1 天时数字加粗 |
| 逾期 | 逾期 N 天 | danger | 红底红描边红字；行内左侧 2px 红竖条 |
| 待审批 | 待审批 | info | 中性灰蓝胶囊 |
| 已驳回 | 已驳回 | danger（20% 底） | 描边红、文字红、底极浅 |
| 停用 / 维修 | 停用 | info | 仅描边，无底色 |
| 可约 / 已约 | 可约 / 已约 | success / info | 场地网格用实心 4px 圆角方块，不追加文字（网格内以角标标识） |

### 4.2 深色模式（扩展定义，本期不启用）

> IA §7.9 已明确「不做暗色主题」。为满足契约「深/浅极端定义」要求，同时避免前端误用，以下 token 作为 `[data-theme="dark"]` 预留，**本期不实现、不随主题切换器暴露**（仅深色侧栏与登录页画布常驻使用 `--color-sidebar`）。

| Token | Dark Hex |
|---|---|
| `--color-ink-900` | `#E8EDF7` |
| `--color-ink-500` | `#98A3B8` |
| `--color-surface` | `#121B2C` |
| `--color-surface-page` | `#0B1220` |
| `--color-border` | `#22304A` |
| `--color-primary` | `#6F8DFF` |
| `--color-success` | `#3FBF8F` |
| `--color-warning` | `#E0A050` |
| `--color-danger` | `#F0776C` |
| `--color-tint-*` | 各语义色 16% 叠加于 `#121B2C` |

### 4.3 CSS 变量（可直接落地）

```css
:root {
  /* color */
  --color-ink-900: #141B2D;
  --color-ink-700: #3C4659;
  --color-ink-500: #6B7688;
  --color-ink-300: #A7B0C0;
  --color-surface: #FFFFFF;
  --color-surface-page: #F5F7FA;
  --color-surface-subtle: #F0F3F8;
  --color-sidebar: #101A2E;
  --color-sidebar-hover: #18243C;
  --color-sidebar-active: rgba(66, 106, 255, 0.14);
  --color-border: #E2E7EF;
  --color-border-light: #EDF1F6;
  --color-primary: #2B5CE6;
  --color-primary-hover: #1F49C7;
  --color-primary-active: #183BA3;
  --color-primary-tint: #EEF3FE;
  --color-success: #0A7A57;
  --color-success-tint: #E8F4EF;
  --color-warning: #A85B00;
  --color-warning-tint: #FCF2E5;
  --color-danger: #B3261E;
  --color-danger-tint: #FBEDEC;
  --color-info: #4A5A72;
  --color-info-tint: #EEF1F6;
  --color-focus-ring: rgba(43, 92, 230, 0.35);

  /* typography */
  --font-sans: "Noto Sans SC", "PingFang SC", "Microsoft YaHei", sans-serif;
  --font-display: "Sora", "Noto Sans SC", "PingFang SC", sans-serif;
  --font-mono: "JetBrains Mono", "SFMono-Regular", Consolas, monospace;
  --text-12: 12px; --text-13: 13px; --text-14: 14px;
  --text-16: 16px; --text-20: 20px; --text-28: 28px; --text-40: 40px;
  --leading-tight: 1.25; --leading-normal: 1.5; --leading-relaxed: 1.6;

  /* space (4px base) */
  --space-1: 4px;  --space-2: 8px;  --space-3: 12px; --space-4: 16px;
  --space-5: 20px; --space-6: 24px; --space-8: 32px; --space-10: 40px;
  --space-section: 32px;

  /* shape */
  --radius-sm: 2px; --radius-base: 4px; --radius-card: 6px;
  --radius-overlay: 8px; --radius-pill: 999px;
  --border-width: 1px;
  --shadow-card: 0 1px 2px rgba(16, 24, 40, 0.04);
  --shadow-raised: 0 6px 16px rgba(16, 24, 40, 0.08);
  --shadow-overlay: 0 16px 40px rgba(16, 24, 40, 0.12);

  /* motion */
  --motion-fast: 120ms;
  --motion-standard: 180ms;
  --motion-enter: 240ms;
  --motion-emphasis: 320ms;
  --ease-out: cubic-bezier(0.2, 0.8, 0.2, 1);
  --ease-enter: cubic-bezier(0.16, 1, 0.3, 1);
  --ease-exit: cubic-bezier(0.4, 0, 1, 1);

  /* z-index */
  --z-base: 1; --z-sticky: 100; --z-dropdown: 1000;
  --z-drawer: 2000; --z-dialog: 2100; --z-toast: 3000;
}
```

**Element Plus 覆盖（19 个变量，写在一个 `:root` 块内，禁止改组件源码）**

```css
:root {
  --el-color-primary: var(--color-primary);
  --el-color-success: var(--color-success);
  --el-color-warning: var(--color-warning);
  --el-color-danger: var(--color-danger);
  --el-color-error: var(--color-danger);
  --el-color-info: var(--color-info);
  --el-text-color-primary: var(--color-ink-900);
  --el-text-color-regular: var(--color-ink-700);
  --el-text-color-secondary: var(--color-ink-500);
  --el-text-color-placeholder: var(--color-ink-300);
  --el-border-color: var(--color-border);
  --el-border-color-light: var(--color-border-light);
  --el-border-color-lighter: var(--color-border-light);
  --el-bg-color-page: var(--color-surface-page);
  --el-border-radius-base: var(--radius-base);
  --el-font-family: var(--font-sans);
  --el-font-size-base: 14px;
  --el-transition-duration: 0.18s;
  --el-transition-duration-fast: 0.12s;
}
```

- **可被主题覆盖的 token**：`--color-primary*`、`--color-sidebar*`（若未来做多租户/品牌换肤）。
- **不可被业务页覆盖**：语义色（success/warning/danger）、边框、字号、间距——防止页面各自为政。

---

## 5. Micro-Typography

| 角色 | 字体 | 字号/行高 | 字重 | 字距 | 用法 |
|---|---|---|---|---|---|
| 页面标题 H1 | Sora + Noto Sans SC | 20/28 | 600 | −0.01em（仅拉丁） | 每个页面唯一一个，固定在内容区顶部 |
| 区块标题 H2 | Noto Sans SC | 16/24 | 600 | 0 | 卡片/区块标题 |
| 正文 | Noto Sans SC | 14/22 | 400 | 0 | 表单、说明、段落 |
| 表格正文 | Noto Sans SC | 13/20 | 400 | 0 | 列表主值 |
| 表头 | Noto Sans SC | 12/18 | 500 | 0.02em | `#6B7688`，全站统一 |
| 辅助/元信息 | Noto Sans SC | 12/18 | 400 | 0 | 时间、操作人、ID 外的说明 |
| 数字/编号 | JetBrains Mono | 13/20 | 500 | 0.01em | 资产编号、单号、金额、日期、分值；`font-variant-numeric: tabular-nums` |
| KPI 数字 | JetBrains Mono | 28/34（看板 40/46） | 500 | 0 | 工作台/看板计数，单位用 12px 常规字排在数字右上或后接 |
| 空态标题 | Sora + Noto Sans SC | 16/24 | 600 | 0 | 仅空态使用 |

**排版规则**

- 中文字距恒为 0（不加 letter-spacing）；仅 Sora 标题允许 −0.01em，`display only`，禁止用于正文。
- 数字与中文混排时，数字前后各留 `0.125em` 视觉间隔（用 `font-feature-settings` + 空格字符不实现，改由组件模板插入窄空格 `&#8201;`）。
- 金额统一 `¥` + 千分位 + 两位小数（`¥1,280.00`）；日期统一 `YYYY-MM-DD`；相对时间（`剩余 2 天`）只在状态胶囊内出现，表格主值仍用绝对日期。
- 任何长度文本：表格单元格单行省略 + `title` 悬浮全文；详情页字段允许 2 行后省略；禁止用 `font-size` 缩小来塞内容。

**字体来源**：Sora（Google Fonts, SIL OFL）、Noto Sans SC（Google Fonts, OFL）、JetBrains Mono（OFL）。全部自托管 `woff2` 子集（中文按需分片），**不使用 Inter / Roboto / Arial / 系统默认字体作为主字体**（系统字体仅作 CJK fallback）。若字体加载失败：直接落到 `PingFang SC` / `Microsoft YaHei`，布局不变（字号与行高已按 CJK 标定）。

---

## 6. Motion Math

| 档位 | 时长 | 曲线 | 用途 |
|---|---|---|---|
| fast | 120ms | `cubic-bezier(0.2, 0.8, 0.2, 1)` | hover 颜色/底色、焦点环、图标旋转 |
| standard | 180ms | `cubic-bezier(0.2, 0.8, 0.2, 1)` | 展开/收起、Tabs 指示条、筛选面板 |
| enter | 240ms | `cubic-bezier(0.16, 1, 0.3, 1)` | 抽屉/弹层入场、路由切换 |
| emphasis | 320ms | `cubic-bezier(0.16, 1, 0.3, 1)` | 状态点亮（§1.3）、KPI 数字滚动、图表首次绘制 |
| exit | 140ms | `cubic-bezier(0.4, 0, 1, 1)` | 弹层关闭、Toast 退场 |

**运动规则**

- 只动 `transform` / `opacity` / `background-color`；禁止动画 `width/height/top/left`（布局抖动）。
- 路由切换：`opacity 0→1` + `translateY(4px)→0`，180ms；不做横向滑动（台账式界面忌"翻页"感）。
- 抽屉（审批详情）从右侧滑入 320px 宽，enter 240ms；遮罩 `rgba(16,24,40,0.35)` 同步淡入。
- 表格行 hover 仅背景变 `#F7F9FC`，120ms；不做位移、不做缩放。
- 状态胶囊变化（如归还后 借出→在库）：底色/文字色 180ms 过渡 + 一次 1px 描边闪光，不弹跳。
- 列表加载：骨架屏（`el-skeleton` 定制为 1px 灰线 + 浅灰块），禁止 spinner 居中占屏。
- `prefers-reduced-motion: reduce` 时：所有 enter/emphasis 动画降为 0ms，仅保留颜色过渡 120ms 内的瞬时切换；KPI 直接显示终值。

**Motion Hooks（交给 `motion-concept` 展开）**

| Hook | 位置 | 描述 |
|---|---|---|
| H1 | P-ASSET-02 详情 | 「状态点亮」：胶囊 + 时间线首点同步 320ms |
| H2 | P-HOME-01 工作台 | KPI 数字 count-up（WAAPI，600ms，ease-out） |
| H3 | P-ASSET-01 列表 | 状态切片 Tab 切换时表格行 stagger 24ms × 最多 8 行 |
| H4 | P-ROOM-01 场地网格 | 选中切片：外环 12px 扩散一次 + 描边收束 180ms |
| H5 | P-ASSET-04 转交审批 | 抽屉入场 + 审批通过后行状态迁移（借出→借出·新持有者） |
| H6 | P-ADM-01 管理看板 | ECharts 首次绘制：柱/饼 600ms `ease-out-quart`，坐标轴先出 |
| H7 | 全局 | Toast 进入 240ms 上移 8px，停留 3s，退出 140ms |
| H8 | P-VOTE-02 投票页 | 投票提交成功：饼图从 0 扫到实际值 600ms（仅一次，不重复） |

---

## 7. Layout System

### 7.1 应用壳（工具型界面，扫描效率优先）

| 区域 | 尺寸 | 规则 |
|---|---|---|
| 左侧导航 | 240px 固定（≤1280px 折叠为 64px 图标态） | 深墨蓝；顶部 Logo 字标 40px 高；分组标题 11px `#7C879C`（仅"管理"分组一处） |
| 顶栏 | 56px | 白底 + 1px 底线；左侧面包屑；右侧用户菜单（头像 28px + 姓名 + 角色标签） |
| 内容区 | 最大 1440px，居中；左右内边距 24px（≥1440px 时 32px）；上下 24px | 首屏结构：`H1 + 一行操作/说明` → `KPI 行（可选）` → `主内容` |
| 表格页 | 筛选条 48px → 工具条 48px → 表格 → 分页 56px | 四段式；筛选条默认收起到 1 行，超出进"更多筛选"面板 |
| 详情页 | 左 8 列内容 + 右 4 列动作/时间线（≥1280px） | 右栏 sticky，顶距 24px；≤1280px 右栏落到内容下方 |
| 看板 | 3 × KPI 卡（高 96px）+ 2 × 图表卡（高 320px） | 卡片间距 16px；不做卡片套卡片 |
| 登录页 | 左侧 60% 深墨蓝画布（几何发丝网格），右侧 40% 表单卡 | 无插图、无渐变；表单卡 400px 宽，8px 圆角 |

### 7.2 网格与不变量

- 栅格：12 列，gutter 16px，`max-width: 1440px`。
- 间距只用 4 的倍数（4/8/12/16/20/24/32/40/48/64）。
- 表格列宽策略：固定列（编号 120 / 名称 240 / 状态 120 / 持有者 140 / 到期 140 / 操作 160），其余弹性；操作列右侧固定。
- 断点：`≥1440`（舒适）/ `1280–1439`（默认）/ `1024–1279`（侧栏折叠，右栏下沉）/ `768–1023`（筛选折叠、表格横向滚动）/ `<768`（仅可滚动可点击，PRD Q11）。
- 不变量：所有固定格式元素（状态胶囊 24px 高、按钮 32px、输入 32px、头像 28/32px、表格行 40/44px）在任意断点**尺寸不变**，只换排布；正文 `min-width: 0` + 省略号，禁止文字溢出与重叠。

### 7.3 密度策略

| 页面类型 | 密度 | 依据 |
|---|---|---|
| 工具型（列表/审批/台账） | 高密度：行高 40px、字号 13px、间距 8/12 | 扫描效率优先 |
| 半内容型（详情/工作台） | 中密度：行高 44px、间距 16/24 | 兼顾阅读与操作 |
| 内容型（投票活动页/登录页） | 低密度：间距 32/40、允许 1 处 40px 大数字或大标题 | 唯一允许"媒介化表达"的地方，仅限投票页标题与结果图 |

---

## 8. Component Atmosphere

| 组件 | 默认 | hover | focus | active | disabled / empty / loading / error |
|---|---|---|---|---|---|
| 主按钮 | 32px 高，`#2B5CE6` 底，白字，4px 圆角，字 13/500 | `#1F49C7` | 2px `--color-focus-ring` 外环 | `#183BA3` + 内阴影 1px | 禁用：`#F0F3F8` 底 + `#A7B0C0` 字，无阴影；loading：文字改 3 点脉冲 + 宽度锁定不跳动 |
| 次按钮 | 白底 + 1px `#E2E7EF`，`#3C4659` 字 | 边 `#2B5CE6`、字 `#2B5CE6`、底 `#EEF3FE` | 同上 | 底 `#EEF3FE` | 禁用同上 |
| 文本/链接按钮 | 无边框，`#2B5CE6` 字 | 下划线 1px（offset 3px） | 焦点环 | `#183BA3` | 禁用 `#A7B0C0` |
| 危险按钮（驳回/删除） | 1px `#B3261E` 描边 + 白底 | 底 `#FBEDEC` | 焦点环 | 底 `#FBEDEC` + 字 `#8E1B15` | 二次确认弹层必须写明影响范围 |
| 侧栏导航 | 深底；项高 44px，图标 18px + 字 13，`#9AA6BC` | 底 `#18243C`，字白 | 焦点环（浅色） | 选中：底 `rgba(66,106,255,.14)` + 左侧 2px `#4C77FF` 竖条 + 字白 | 无权限项不渲染（不做灰色可见） |
| 表单 | label 13/500 `#3C4659`；输入 32px，1px `#E2E7EF` | 边 `#C9D2E0` | 边 `#2B5CE6` + 2px tint 环 | — | 错误：边 `#B3261E` + 下方 12px 红字（含原因）；必填星号仅红点 4px；禁用：`#F0F3F8` 底 |
| 表格 | header 40px `#F7F9FC` 底 + 12px 字；行 40px，行间 1px `#EDF1F6` | 行底 `#F7F9FC` | 行内焦点环 | 选中行底 `#EEF3FE` + 左 2px `#2B5CE6` | 空：`el-empty` 线稿 + 一句原因 + 主 CTA；loading：8 行骨架；错误：表体替换为 1 行错误条 + 重试 |
| 状态胶囊 | 24px 高，4px 圆角，1px 描边，12/500 字，底为语义 tint | 不响应 hover（非交互） | — | — | 无 |
| KPI 卡 | 白底，1px 边框，6px 圆角，无阴影；标题 12/500 `#6B7688`，数字 28/500 mono，单位 12px | 边框 `#C9D2E0`（可点击卡） | 焦点环（可点击时） | — | loading：数字位骨架条；空：显示 `—` 而非 0 |
| 图表卡 | 高 320px；标题行 40px；绘图区 padding 16px | — | — | — | 无数据：居中一行说明 + 灰线底座；加载：浅灰柱状骨架 |
| 弹层 Dialog | 8px 圆角，1px 边框，`--shadow-overlay`；宽 480（表单）/ 640（详情） | — | 内部首个可聚焦元素自动聚焦 | — | 关闭按钮 32×32 命中区；ESC 关闭；遮罩点击可关（危险确认除外） |
| 抽屉 Drawer | 右滑入，宽 480（审批）/ 560（详情）；内容分「头部信息 / 主体 / 底部动作」 | — | 同上 | — | 底部动作条 64px，sticky，主按钮右对齐 |
| 流转时间线 | 竖向 1px `#EDF1F6` 轴 + 10px 节点；节点色=事件语义色 | 事件行底 `#F7F9FC` | — | — | 空：一行"暂无流转记录"；不画假节点 |
| 场地切片网格 | 单元格 72×40，4px 圆角，2px 间隙 | 边 `#2B5CE6` | 焦点环 | 选中实心 `#2B5CE6` 白字 | 已约：`#EEF1F6` 底 + 斜线 45°（1px，6px 间隔）+ 不可点；过期：同已约但文字 `#A7B0C0` |
| 分页 | 高 32px，当前页 `#EEF3FE` 底 + `#2B5CE6` 字 | 底 `#F7F9FC` | 焦点环 | — | 总数为 0 时整行隐藏，不显示"共 0 条" |

---

## 9. Aesthetic Complexity Match（汇总）

| 方向 | 视觉野心 | 复杂度 | 素材成本 | 性能预算 | 结论 |
|---|---|---|---|---|---|
| D1 | 高（排版精度 + 状态语法） | 低—中 | 极低 | 轻 | ✅ 采用 |
| D2 | 中高（网格严谨） | 低 | 极低 | 极轻 | 作为 D1 的"网格克制"来源 |
| D3 | 中（暖色人文） | 中（第二套字体） | 低—中 | 中（额外字体） | 仅投票页局部借用 1 处大号标题 |

---

## 10. Asset Implications（供 `resource-inventory`）

| 资产 | 需求 | 规格 | 备注 |
|---|---|---|---|
| Logo 字标 | "ERS" 字标 + 中文名横排 | SVG，24/32/40 三档；深底反白版 | 由字体（Sora 600）转曲，无图形 Logo |
| 图标 | Element Plus Icons 全量按需引入 | 18px（导航/按钮）、16px（表格操作）、14px（状态） | 描边 1.5 视觉统一；不引入第二套图标库 |
| 空态线稿 | ① 资产箱（无资产）② 日历网格（无预约）③ 票箱（无投票） | SVG 200×140，1.5px 线，`#A7B0C0` | 3 张，自绘；不使用人物插画 |
| 图表主题 | ECharts 主题 JSON | 6 色分类 + 轴/网格/提示框样式 | 见 §4.1 分类色：`#2B5CE6 #0E7A8A #A85B00 #5A7D2A #B3261E #6B7688` |
| 字体文件 | Sora / Noto Sans SC / JetBrains Mono | woff2，子集化；中文按 unicode-range 分片 | 首屏只加载 400/500 两档 |
| 纹理 / 背景 | 登录页深底几何网格 | 纯 CSS `repeating-linear-gradient` 1px 线，间距 32px，透明度 6% | 不使用图片 |
| 图片 / 视频 / WebGL | **无** | — | 明确需求：0 张照片、0 视频、0 Canvas 背景 |

---

## 11. Accessibility And Performance Notes

**可访问性**

- 正文与次要文本对比度：`#141B2D`/`#F5F7FA` 15.8:1；`#3C4659`/白 9.6:1；`#6B7688`/白 4.7:1；语义色均 ≥5.0:1（见 §4.1 表），满足 WCAG 2.1 AA；KPI 大数字额外达到 AAA。
- 状态**不只依赖颜色**：胶囊内含文字（在库/借出/逾期），表格逾期行同时有左侧 2px 红竖条 + 文字"逾期 N 天"。
- 焦点可见：所有可交互元素 `:focus-visible` 显示 2px `--color-focus-ring` 外环（offset 2px），禁止 `outline: none` 而不给替代。
- 键盘：弹层焦点陷阱 + ESC 关闭；表格行可用 `Tab` 进入操作按钮；跳转到主内容的 skip link。
- 触控目标 ≥ 32×32（桌面）；移动降级时按钮最小 40×40。
- 表单错误同时给颜色、图标、文字三重信号；`aria-describedby` 关联错误文案。

**性能**

- 字体：3 个 family，仅 400/500 两档，woff2 + 子集化；`font-display: swap`；预计字体总量 ≤ 180KB（CJK 分片按页加载）。
- Element Plus 按需引入（`unplugin-vue-components`），首屏 JS 目标 ≤ 250KB gzip；图表页 ECharts 按需注册（饼/柱/线）。
- 动画仅 `transform/opacity`；`will-change` 只在抽屉打开期间临时加。
- 列表虚拟滚动阈值：单页 > 50 行时启用 `el-table-v2`；默认分页 20 行。
- `prefers-reduced-motion: reduce` 时禁用 enter/emphasis（见 §6）。

---

## 12. Frontend Constraints

1. **令牌唯一来源**：颜色/间距/圆角/动效只能来自 §4.3；业务组件不得写死 Hex（ESLint `stylelint-declaration-strict-value` 可强制）。
2. **Element Plus 覆盖只写变量，不改源码**；如需新变体，新建 `ers-*` 组件包一层。
3. **数字必用 `--font-mono`**；表格主值、金额、日期、编号、分值不得使用默认 sans。
4. **不使用**：渐变背景、玻璃拟态、彩色光球、阴影叠加超过 2 层、卡片嵌套卡片、紫蓝渐变 hero。
5. **状态渲染统一走 `<StatusTag>` 组件**，输入为状态枚举，输出文案+色+图标；禁止页面内手写颜色。
6. 表格行高 40px（紧凑页）/44px（详情内嵌表），不得用 padding 压缩内容。
7. 所有 `el-dialog` / `el-drawer` 必须提供明确标题与关闭语义；危险操作二次确认。
8. 路由切换保持应用壳不重挂载；内容区滚动位置按 query/路由 key 恢复。
9. 所有异步区块具备 loading/empty/error 三态；错误态给出"重试"而非空白。
10. 响应式只在断点改变排布，不改变固定格式控件尺寸（§7.2）。

---

## 13. Handoff Checklist

- [x] `Art Direction`：概念、宣言、记忆钩子、差异化点、复杂度匹配（§1）
- [x] `Reference Translation`：URL 重开结果、Element Plus 实证令牌、转译表、禁止复制项、代价（§2）
- [x] `Design Tokens`：颜色（含 dark 扩展）、CSS 变量、EP 覆盖 18 项、排版、动效、层级（§4–§6）
- [x] `Layout System`：应用壳尺寸、12 列网格、密度策略、响应式不变量（§7）
- [x] `Component Atmosphere`：按钮/导航/表单/表格/胶囊/KPI/图表/弹层/抽屉/时间线/场地网格/分页，含全部状态（§8）
- [x] `Motion Hooks`：H1–H8，可直接交 `motion-concept`（§6）
- [x] `Asset Implications`：可交 `resource-inventory` 提取（§10）
- [x] `Accessibility And Performance Notes`：对比度、reduced-motion、字体与 JS 预算（§11）
- [x] `Visual Direction Matrix Spec`：3 方向 × 3 P0 切片 + `frontend_signature` + `implementation_complexity`（§3）
- [x] `recommended_direction` = D1；`selected_direction` = D1（已冻结）
- [ ] Logo 字标与 3 张空态线稿尚未出图 → 交 `resource-inventory` / `figma-design-bridge` 产出
- [ ] ECharts 主题 JSON 尚未落文件 → 开发 sprint 首日产出（本文件 §4.1/§10 已给色值）

## 14. Validation Checklist（自检）

| 检查项 | 结果 |
|---|---|
| 是否读取 IA 并以其页面 ID / 模块为依据 | ✅ 三块切片、状态字典、时间线结构均引用 IA v1.3（P-HOME-01 / P-ASSET-01/02/04 / P-ROOM-01 / P-VOTE-02） |
| PRD 含 Benchmark References 时是否重开 URL 并输出 Translation | ✅ §2.1 记录重开结果（两站登录墙），§2.2 取得 Element Plus 实证令牌，§2.3 逐站转译 + 禁止复制 |
| 颜色是否全部 Hex、字体是否具体名 + fallback | ✅ §4.1/§5；无"品牌蓝"之类的空描述 |
| 是否避免通用 AI 审美（紫蓝渐变、白底圆角卡流、无意义光球） | ✅ §12 第 4 条明令禁止；方案主体为发丝线 + 等宽数字 + 深墨导航 |
| 是否避免 Inter/Roboto/Arial/system 作主字体 | ✅ 主字体 Sora / Noto Sans SC / JetBrains Mono；系统字体仅作 CJK fallback |
| 是否有 reduced-motion、对比度、媒体性能约束 | ✅ §11 |
| 文字溢出 / 元素重叠 / 固定格式尺寸漂移是否有规则 | ✅ §7.2 不变量 + §5 省略规则 |
| 是否卡片套卡片 / 装饰层堆叠 | ✅ 明确禁止（§12.4）；卡片仅单层，内部分隔用发丝线 |
| Motion Hooks 是否可被 `motion-concept` 直接展开 | ✅ H1–H8 均含位置、触发条件、时长与曲线 |
| Asset Implications 是否可被 `resource-inventory` 提取 | ✅ §10 逐项给规格、格式、数量 |
| 是否只转译策略而未复制参考站资产 | ✅ 未使用任何参考站 Logo/图片/文案/色值；Element Plus 值仅作替换基线并逐项说明 |
| 三方向是否都来自参考推荐方向（受控变体） | ✅ D2/D3 分别是 D1 的"网格化"与"暖色化"变体，非无关风格 |

## 15. Open Questions（非阻塞）

| # | 问题 | 默认处理 |
|---|---|---|
| V1 | 品牌 Logo 字标最终字形（Sora 600 转曲 vs 定制） | 先用 Sora 600 转曲，视觉评审后再决定是否定制 |
| V2 | 是否需要为投票活动页单独引入衬线体（D3 借用） | 本期用 Sora 600 放大号代替，不引入第二套西文字体 |
| V3 | 深色模式是否进入下一期 | 本期不做（IA §7.9），令牌已预留 |

---

### 交接

视觉基调已冻结为 **D1 Signal Ledger**。接下来建议使用 `motion-concept` 展开 §6 的 Motion Hooks（H1–H8）；如需生成美术素材提示词与资产清单，使用 `resource-inventory`（输入 §10）；如需把 IA、视觉方向与资产编译为 FigJam/Figma 设计交付物，使用 `figma-design-bridge`（输入 IA §8 + 本文档 §3/§8）。前端实现时以 §4.3 的 CSS 变量为唯一令牌来源，Element Plus 只做变量覆盖。
