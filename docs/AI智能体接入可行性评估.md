# AI 智能体接入可行性评估与落地路线

> 编制时间：2026-09-16
> 目标：在 Token 制造 ERP 内建设 ①AI 智能问答 ②AI 工业智能体（智能下单等）
> 结论口径：所有结论均基于代码实读；涉及行号处可直接定位

---

## 0. 总体判断

**没有"AI 不能接"的死结，但有 4 个"一接就出事"的结构性问题。**

核心矛盾：**系统当前的责任边界是给"人"设计的。** 人一次点一个按钮、不会 7×24 重试、出错可以找主管走流程。Agent 会并行调用工具、会重试、会幻觉、会被 prompt injection 诱导。因此下列事项不是"优化项"，而是**准入条件**。

接入 AI 的前置条件分三类：
- **A 类（阻断）**：不修则"智能下单"做不出来
- **B 类（决策质量）**：会让 Agent 系统性做出错误决策
- **C 类（安全）**：接 Agent 后会放大成事故

---

## A 类 · 阻断"智能下单"的问题

### A1 · 写接口没有幂等契约【最高优先级】

**实测事实**（全量 grep `checkAndOccupy`）：全项目只有 **10 个调用点**，全部是库存类单据：

```
TransferService:84          transfer
ProductionInboundService:76 production_inbound
PurchaseReturnService:71    purchase_return    ← 键含时间戳，等于零幂等
MaterialReturnService:68    material_return
PurchaseInboundService:65   purchase_inbound
OtherStockService:74        other_stock
MaterialIssueService:73     material_issue
AssemblyService:89          assembly
SalesReturnService:75       sales_return
SalesOutboundService:67     sales_outbound
```

**完全没有任何幂等的写路径**（业务影响从高到低）：

| 接口 | 位置 | 后果 |
|---|---|---|
| `POST /api/purchase/order/create` | `PurchaseOrderService:93` | **重复采购下单**（智能下单的主路径） |
| `POST /api/sales/order/create` | `SalesOrderService` | 重复销售接单 |
| `POST /api/mrp/calculate` | `MrpService:61` | 重复生成 MRP 计划与建议 |
| `POST /api/mrp/confirm` | `MrpConfirmService:35` | **重复生成采购单/工单** |
| `POST /api/sales/invoice/create`、`purchase/invoice/create` | — | 重复开票 |
| `POST /api/sales/verification/create`、`purchase/verification/create` | — | 重复核销 |
| `POST /api/finance/receipt/writeoff`、`payment/writeoff` | `ReceiptService:79`、`PaymentService:79` | **重复核销应收应付** |
| `POST /api/inventory/stocktake/adjust` | `StocktakeService` | 重复库存调整 |
| `POST /api/quality/apply/create`、`quality/judge` | — | 重复质检判定 |
| `POST /api/sales/stocking/create`、`packing/create`、`afterSale/create` | — | 重复建单 |

**为什么 Agent 必然踩中**：LLM 在超时、5xx、工具调用解析失败时会重试；多 Agent / 并行工具调用会同时发出请求。重复下单不是概率问题，是必然。

**现有幂等实现是反向的**（`IdempotencyService.java:30`）：

```java
public void checkAndOccupy(String requestId, String biz) {
    if (requestId == null || requestId.isBlank())
        throw new BizException(400, "缺少请求标识 requestId...");
    String key = "idempotent:" + biz + ":" + requestId;
    Boolean success = redisTemplate.opsForValue().setIfAbsent(key, "1", TTL_24H);
    if (Boolean.FALSE.equals(success)) throw new BizException(409, "请勿重复提交");
}
```

- **键在业务逻辑之前占用，事务回滚不释放。** Agent 第一次因"库存不足"失败后，24h 内用同一 requestId 重试 → 被判"请勿重复提交" → Agent 要么死循环、要么换 requestId 再来 → **又变成重复下单**。两个方向都错。
- 键与业务单据之间**无落库关联**，无法审计"哪个 requestId 对应哪张单"。
- **前端 12 个 api 文件没有任何一处传 `requestId`** → 现有幂等能力实际未被使用。Agent 将是第一个真正的调用方，所以契约必须现在定对。

### A2 · MRP → 采购单链路本身是断的

这是"智能下单"最可能复用的路径，但当前跑不通。

**`MrpConfirmService.generatePurchaseOrders`（L83-115）**：

| 问题 | 位置 | 后果 |
|---|---|---|
| `totalAmount = ZERO` 且插入后**从不重算** | `:93` | 下游 `PurchaseInvoiceService.create` 校验"开票额 ≤ totalAmount − 已开票 − 待审 = 0" → **MRP 采购单永远无法开票** → 无法生成应付 → 采购→发票→应付链路断裂 |
| 明细 `price = 0` | `:101` | 需人工补价 |
| `supplierId = null` 直接落库 | `:86` | 绕过 `PurchaseOrderService.create` 的"供应商不能为空"（走的是直接 insert）；后续按供应商统计、`Payable.supplierId` 全为空 |
| `taxRate` 硬编码 `new BigDecimal("13")` | `:102` | `sys_config.default_tax_rate` 已存在但从未被读 |
| 无锁，`convertedStatus=="none"` 检查无行锁 | `:40` | 并发重复转单 |
| 未校验 `mrp_plan.status` | — | 已 confirmed 的计划可重复确认 |
| 未校验 `requirement.planId == request.planId` | — | 可跨计划确认 |

**`MrpService.calculate` 的逻辑缺陷**：

| 问题 | 位置 | 后果 |
|---|---|---|
| `supplyType` 产品分支**无条件** `"make"`，注释写"产品默认自制（如果无 BOM 则采购）"但**代码里没有该分支** | `:209-215` | 无 BOM 的产品被建议"自制" → 转成无 BOM 的生产工单 → 领料时 `MaterialIssueService` 抛"该工单无 BOM，无法领料"（死路）。**Agent 拿到这种建议会执行错误动作** |
| 无批量策略（Lot Sizing）：无最小订货量、包装倍数、固定批量、EOQ | 全文 | 净需求量直接落为采购量 → 采购量非整数倍、不符合供应商起订量 |
| 无时间分桶（Time Phasing）/ 需求分配（Pegging）：所有需求聚成一个数量、取最早日期 | 全文 | 同一批可用库存在多条需求间没有先后分配，只是整体抵减一次 |
| `calcInTransitQty` 对**每条需求**都全量拉一次已审批采购单再逐单查明细 | `:278-291` | O(N×M) 查询，且 Agent 会频繁触发 |
| 无幂等无锁 | `:61` | 重复点"计算"生成多份 plan |
| `MrpRequest` 只有 `startDate/endDate` | DTO | 无产品/仓库范围、无计划员、无"是否考虑预留"参数，Agent 无法精细化调用 |

**`MpsPlanService` 名不符实**：只有"登记 + 转工单"两个方法，**没有任何 MPS 运算**（无预测、无 ATP、无毛净需求、无产能校验）。`generateWorkOrder` 只阻断 `confirmed`，不要求 `planned`。

> **结论：AI 接入前，MRP→采购单链路必须先能人工走通。否则是在给一根断掉的管道加智能大脑。**

### A3 · "查库存 → 决定下单"是典型 TOCTOU

**实测**：`availableQty()` / `stockQty()` / `stockQtyTotal()` / `warningList()` 全部是**无锁快照读**。

Agent 的工作模式就是"观察 → 决策 → 行动"三步分离，天然踩 TOCTOU。而唯一有原子语义的 `reserve()`（锁库）**没有任何地方把它作为下单前置条件来用**。

**正确姿势**：
```
❌ Agent: 查 availableQty → 判断够 → 下采购单
✅ Agent: 尝试 reserve（原子，失败即拒绝）→ 成功则下单 → 订单取消时 release
```

现有 `InventoryService.reserve/release` 能力已具备，缺的是**把它接入下单流程**，以及修 `release()` 无锁的问题（`InventoryService:410-427` 用 `selectById` 读后覆盖写，与有 `FOR UPDATE` 的 `reserve()` 保护不对称，并发会丢更新）。

### A4 · 库存类单据没有作废/红冲能力

**实测**：以下单据的 `status` **建表默认值就是终态 `completed`**，落库即完成：

```
sales_outbound    purchase_inbound    production_inbound
material_issue    material_return     transfer
other_stock       assembly_order
```

**没有草稿态、没有已审核中间态、没有作废接口。** 全项目只有 `production_order` 有 `voidOrder`（且它也仅阻断 `completed`，对 `producing` 状态能直接作废，不回滚领料、不释放库存）。

**对 AI 自动化的意义**：Agent 一定会犯错（幻觉数量、选错物料、重复下单）。**没有红冲 = 出错后只能人工进 SQL 手改**。这在工业场景不可接受，直接决定"敢不敢放开自动执行"。

**这是放开自动化的前置条件，优先级高于任何 AI 功能本身。**

---

## B 类 · 会让 Agent 做出错误决策的问题

| # | 问题 | 位置 | 对 AI 的影响 |
|---|---|---|---|
| **B1** | `calcMaterialCost` **漏乘工单数量**——算出来是单件材料成本，而人工/制造费用是整单的，量纲混在一起 | `CostService.java:82-94`（已复核） | 总成本被低估约"工单数量"倍 → **任何基于成本的 AI 决策全部失真**：毛利测算、建议售价、经济批量、成本异常预警 |
| **B2** | `loss_rate` 口径跨模块冲突：MRP/领料/成本按**小数**（`1+lossRate`），齐套分析按**百分比**（`1+loss/100`） | `MrpService:166`、`MaterialIssueService:142`、`CostService:89` vs `CompleteAnalysisService:55` | DB 定义 `DECIMAL(10,6)` 无单位说明。若录 0.05 表示 5%，齐套分析当 500% 算 → **需求量放大 100 倍** → Agent 看到"缺料"疯狂下采购单 |
| **B3** | 明细累计字段读-改-写无锁（4 处同构） | `SalesOutboundService:84-125`、`PurchaseInboundService:86-133`、`MaterialIssueService:111-153`、`SalesReturnService:97-107` | 人单线操作几乎不触发；**Agent 并行调用必触发** → 超发/超收/超领/超退。且库存 `change()` 只保证不超台账，**不保证不超订单** |
| **B4** | `operation_log` / `approval_record` / `attachment` **建了表但全项目无实体、无 Mapper、从来没人写** | `schema.sql` | **AI 自动化的合规底线是"可审计"**：谁触发、Agent 看到什么上下文、依据什么规则、执行了什么、结果如何、能否回滚。现在完全没有 → 这是 AI 落地的**硬缺口**，不是可选项 |
| **B5** | 质检不合格处理**只记录不执行**：`handleType`(return/scrap) 只写 `quality_result`，不做退货出库或报废出库 | `QualityService.judge:117-143` | 库存进了 `unqualified` 维度后**永远无人处理**，成为黑洞。Agent 若基于库存做决策会算错可用量 |
| **B6** | 无面向 Agent 的语义查询 API。唯一聚合接口 `cockpit()` 是**把 5 张表全量加载进 JVM 内存**做聚合 | `DashboardService:90/107/119/129/140` | Agent 无法高效获取"业务全景"用于推理；且数据量上万即显著变慢，十万级 OOM |
| **B7** | 状态字段全是字符串魔法值，无枚举、无 CHECK 约束 | 全项目 30+ 种字面量 | Agent 从描述里推断状态值极易出错。典型：`production_order` 的 `qc_status` 用 `"pending"`=待质检，同行 `approve_status` 的 `"pending"`=待提交 |
| **B8** | `sys_config.negative_stock_strategy='forbid'` 已存在但**代码从未读取**，是死配置 | `InventoryService.doChange:142` | Agent 无法通过配置调整库存策略；`allow_self_approve`/`default_tax_rate`/`amount_scale` 等同样是死配置 |

---

## C 类 · 安全（接 Agent 会放大）

### C1 · 14 个 Controller 直接把实体当 `@RequestBody`【重大风险】

实测 grep 命中 **29 处**：

```
UserController:31/36         CustomerController:30/36      SupplierController:30/36
MaterialController:30/36     ProductController:30/36       WarehouseController:30/36
RoleController:27/32         DepartmentController:26/31    DictionaryController:34/39
MachineController:38/45      WorkshopController:36/43      LocationController:37/43
AreaController:37/44         SerialRuleController:36/43    UnitController:36/43
```

**对 Agent 的具体风险**：把 OpenAPI 转成 tool schema 时，这些实体的**所有字段**都会被暴露成可写参数。Agent（或被 prompt injection 诱导的 Agent）可以直接：

```
POST /api/system/user/create
{ "username":"x", "password":"<可控hash>", "status":1, "id":<任意值>, "deleted":0, ... }
```

**给 Agent 的 tool 绝对不能直接挂这些接口，必须包一层白名单 DTO（只暴露确实需要的字段）。**

同时，`params: any` / DTO 无 JSR303 注解（`pom.xml` 里 `spring-boot-starter-validation` 已引入，但全项目只有 `LoginRequest` 用了）→ 自动生成的 tool schema 质量会很差，LLM 容易传错参数。

### C2 · 没有数据权限，且只有一个超级账号

- `Role.dataScope` 字段存在（all/dept/self）但**从未用于任何查询过滤**。
- 当前只有 `admin` / `admin123`（全部权限）。

**必须做**：服务账号（service account）+ 最小权限角色 + **工具级 scope**（每个 tool 对应独立权限点）。否则 Agent 要么寸步难行，要么权限过大。

### C3 · `checkSelfApprove` 在 `createBy == null` 时静默放行

11 处调用逻辑均为 `createBy != null && createBy.equals(currentUser)`。**Agent 若在无用户上下文的场景建单，会自动绕过全部审批风控。**

反向注意：若 Agent 用同一账号既建单又审批，`allow_self_approve=false` 会挡住它——**这正是你想要的 human-in-the-loop 阀门**，但要显式设计，不能靠碰运气。

### C4 · 错误码不可用，Agent 无法分类失败

- 401/403 是**真实 HTTP 状态码**（`SecurityConfig` 配了 entryPoint/accessDeniedHandler，这点做得好）。
- 但**业务失败一律 HTTP 200 + body 内 code**，且只实现了 **400/401/403/500**，`GlobalExceptionHandler` **没有 409 和 422 分支**（而 V1 需求文档里定义了这两个）。

**Agent 的困境**：无法区分「参数错（重试无用）」「库存不足（可稍后重试）」「重复提交（不该重试）」「并发冲突（应退避）」，只能从 `message` 字符串里猜语义。**强烈建议为 Agent 引入结构化错误码**（建议：`400xx` 参数、`409xx` 状态冲突、`422xx` 业务校验、`429xx` 限流、`500xx` 系统）。

### C5 · 凭证与密钥

| 问题 | 位置 |
|---|---|
| JWT 密钥有硬编码默认值 `token-erp-secret-key-please-change-in-production-...` | `application.yml:33` |
| DB 密码默认 `<已移除，见 .env 的 DB_PASSWORD>`、Redis 无密码、无生产 profile | `application.yml:11-15` |
| 密码经 URL query 传递：`POST /api/system/user/reset-password?id=..&newPassword=明文` | `UserController` |
| Swagger 无 profile 隔离，生产环境公开全部 305 个接口契约 | `application.yml:36-41` |
| `AuthService.login` 无失败次数限制、无锁定、无验证码 | `AuthService:22-41` |
| `JwtAuthFilter` 里 `catch (Exception ignored) {}` 静默吞异常，攻击尝试不可观测 | `JwtAuthFilter` |

### C6 · NL2SQL 的红线

**绝对不能让 LLM 直连数据库。** 当前是 `root / <见 .env 的 DB_PASSWORD>`，且没有只读账号。

必须的隔离层：
```
LLM
 ↓ 只能调用「语义查询工具」（预定义参数，非自由 SQL）
只读数据源（独立只读账号，只授予白名单视图）
 + SQL AST 校验（禁 DML/DDL/多语句/UNION 越权）
 + 强制 LIMIT + 语句超时 + 结果行数上限
 + 敏感字段脱敏（成本、毛利、银行账号）
```

另注：V1 需求文档明确冻结"不做 AI 智能下单/交期预测/NL2SQL"，且项目规则是「**文档没说做的，代码不能做**」→ **第 0 步必须先在 `docs/开发总进度.md` 立项 AI 模块。**

---

## D 类 · 可以复用的有利条件（别浪费）

| 现有资产 | 如何用于 AI |
|---|---|
| **审批状态机 `pending → approving → approved` 已全单据统一** | **这就是天然的 human-in-the-loop。** 最稳的落地路径：**Agent 只做 `create + submit`，审批留给人。** 不需要自造停机确认机制 |
| **库存有唯一入口 `InventoryService.change()`**（已验证无绕写） | **护栏有地方注入**：限额、物料白名单、二次校验、审计钩子都能挂在这一处。很多项目没有这个条件 |
| **305 个端点命名规范 + springdoc OpenAPI 已配置** | tool schema 可自动生成（但按 C1 必须裁剪） |
| **`Result<T>` / `PageResult<T>` 全量统一（305/305）** | Agent 工具适配器可统一封装，解析成本低 |
| **`PurchaseInquiryService` 已有比价逻辑**：按 `supplierId` 归集 `price×qty` 总价取最低；`purchase_inquiry_item` 存了多供应商报价 | "AI 智能选供应商"**有现成数据基础**，可在此之上加 AI 打分（交期、质量历史、价格趋势），不必从零建表 |
| **`WarningActionService` 已有"预警 → 生成采购申请/工单"联动** | 智能补货的现成触发链（但需补幂等，当前重复点击会重复建单） |
| **`sys_config` 已有开关位** | 可放 Agent 总开关、限额、白名单（需先让代码真正读这些配置） |
| **`pom.xml` 无任何 AI 依赖** | 完全绿地，无历史包袱。可选 Spring AI / LangChain4j / 自研编排 |

---

## E 类 · 落地路线（建议顺序）

> 排序原则：**先让"管道通"和"可回滚"，再上智能。**

### 第 0 步 · 立项与隔离（半天）
1. 更新 `docs/开发总进度.md`，正式立项 AI 模块（否则违反项目规则）。
2. 新建 MySQL **只读账号**，为问答场景建白名单视图。
3. `application.yml` 拆出生产 profile，移除 JWT 默认密钥。

### 第 1 步 · 幂等契约重构（最先做，否则不能开始）
4. 把 `IdempotencyService` 从"前置占位"改为「**请求表落库 + 唯一约束 + 业务成功后确认**」；失败/回滚时释放键。
5. 为**创建订单类**接口补齐幂等：`purchase/order/create`、`sales/order/create`、`mrp/calculate`、`mrp/confirm`、发票/核销/收付款 writeoff、`stocktake/adjust`、`quality/*`。
6. 删掉 `PurchaseReturnService:71` 幂等键里的 `System.currentTimeMillis()`。
7. 定义 Agent 幂等契约：`requestId` 由编排层生成并与 `trace_id` 绑定。

### 第 2 步 · 修通 MRP → 采购单链路
8. `MrpConfirmService` 重算 `totalAmount` 与明细金额；校验供应商必填；补 `mrp_plan.status` 校验与并发锁。
9. 修 `MrpService` 的 `supplyType`：补"无 BOM → purchase"分支。
10. 统一 `loss_rate` 口径（建议统一为小数，改 `CompleteAnalysisService`）。
11. 修 `CostService.calcMaterialCost` 补乘工单数量；决定成本口径（标准成本 vs 移动加权）并统一。

### 第 3 步 · Agent 安全边界
12. 建服务账号 + 最小权限角色 + **工具级 scope**；实现 `dataScope` 查询过滤。
13. 为 Agent 单独建 **tool 层 DTO 白名单**（**不复用实体入参接口**）；补 JSR303 校验注解。
14. 引入 **结构化错误码**（含 409/422/429），让 Agent 能分类失败并决定是否重试。
15. `SysConfigService` 改 fail-closed + 加缓存；让 `negative_stock_strategy`/`allow_self_approve` 真正生效并作为 Agent 策略开关。

### 第 4 步 · 审计与回滚（放开自动化的前置条件）
16. 补 `operation_log` / `approval_record` 的实体与写入点，**所有 Agent 发起的写操作强制留痕**（记录 trace_id、工具名、入参、Agent 推理摘要、执行结果）。
17. 给库存类单据补「草稿 + 提交 + 作废/红冲」，让误操作可回滚。

### 第 5 步 · 并发与数据正确性
18. 明细累计字段改为原子条件更新：`UPDATE ..._item SET outbound_qty = outbound_qty + ? WHERE id = ? AND outbound_qty + ? <= quantity`，检查影响行数（4 处同构）。
19. `InventoryService.release()` 补同等锁保护；Redis 锁加 owner 校验（UUID + Lua）；加锁顺序固定化。
20. 把 `reserve()` 接入下单流程，替代"查询后下单"。

### 第 6 步 · 面向 Agent 的数据层
21. 建**语义查询 API**（按业务实体聚合，如"某物料未来 30 天供需""某供应商履约表现"），替代让 Agent 拼 20 个分页请求。
22. `DashboardService` 的全表加载改 `GROUP BY` 聚合 SQL。

### 第 7 步 · 接入 AI
23. 选型 Spring AI / LangChain4j；建 Agent 编排层（建议显式状态机，不要全靠 ReAct）。
24. tool schema 从裁剪后的 tool 层 DTO 生成。
25. 先上**只读问答**（低风险验证）→ 再上**"Agent 建单 + 人审批"**（human-in-the-loop）→ 最后才评估**有限自主执行**（限定金额阈值、限定物料范围、必须可回滚）。

---

## F 类 · 必须避开的坑

| 坑 | 说明 |
|---|---|
| 让 LLM 直连 DB 或自由写 SQL | 现在是 root 账号，一旦注入即全库沦陷 |
| 把 305 个端点全部暴露成 tool | 包含用户/角色/权限/字典的写接口，等于给 Agent 开管理后台 |
| 让 Agent 拥有审批权限 | 等于取消 human-in-the-loop。**务必让 Agent 停在 `submit`** |
| 复用现有幂等实现 | 它是"占位后不释放"，会让 Agent 陷入"重试被拒 → 换 key → 重复下单"的循环 |
| 先做 AI 再修可回滚 | 没有红冲能力时，Agent 的错误无法修正，只能人工改库 |
| 跳过审计表 | 没有 `operation_log` 的 AI 自动化，出事后无法定责与复盘 |
| 用 `availableQty` 做决策 | 无锁快照，必然 TOCTOU |
| 相信 `sys_config` 里的配置已生效 | 至少 4 个 key 代码从未读取，是死配置 |

---

*本评估基于 2026-09-16 代码快照，所有结论可定位到文件与方法。配套阅读：`docs/项目全景摸底报告.md`。*
