# AI 落地 · 待修复 Bug 清单（可勾选执行）

> 编制时间：2026-09-16  
> 用途：按批次自修。每条含 `文件:行号` / 现状 / 修法 / 影响哪个 AI 功能  
> 配套：`docs/AI智能体接入可行性评估.md`（完整分析）、`docs/项目全景摸底报告.md`（全量摸底）



---

## 〇 · 先看这张表：四个 AI 功能的前置依赖完全不同

**这是最重要的一张表** —— 你的四个功能不在同一个难度层级上，不要一起开工。

| AI 功能           | 依赖批次              | 阻塞程度   | 现在能不能开始                   |
| --------------- | ----------------- | ------ | ------------------------- |
| **知识库问答系统**     | 批次 6（安全）+ 批次 8    | **低**  | ✅ **现在就能做**，几乎不受现有 bug 影响 |
| **AI 交期预警**     | 批次 4（专属）+ 批次 3    | **中高** | ⚠️ 需要先补 4 个字段/设施，有真实缺数据问题 |
| **数据 AI 分析助手**  | 批次 3 + 批次 7 + 语义层 | **中**  | ⚠️ 必须先修数据正确性，否则分析的结论是错的   |
| **AI 助手（智能下单）** | 批次 1 + 2 + 5 + 6  | **最高** | ❌ 不修完批次 1/2/5 不建议动手       |

**建议开工顺序：知识库问答（练手 + 验证链路）→ 交期预警（补数据 + 补调度）→ 数据 AI 分析助手 → AI 智能下单。**  
理由：知识库问答是纯读、零风险，能先把 Agent 框架、鉴权、审计这套地基跑通，后面三个复用同一套地基。

---

## 批次 1 · 幂等契约重构【最高优先，阻断一切 AI 写操作】

### ☐ B-01 把 `IdempotencyService` 从"前置占位"改为"落库 + 唯一约束 + 成功后确认"

- **文件**：`service/IdempotencyService.java:30-38`
- **现状**：`setIfAbsent` 在业务逻辑**之前**占用 key，TTL 24h，**事务回滚不释放**。
- **为什么对 AI 致命**：Agent 第一次因"库存不足"失败后，24h 内用同一 `requestId` 重试会被判"请勿重复提交"。Agent 要么死循环、要么换个 `requestId` 再来 → **变成重复下单**。两个方向都错。
- **修法**：
  ```sql
  CREATE TABLE idempotent_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    biz_no VARCHAR(64) NULL,          -- 成功后回填业务单号，可审计
    status VARCHAR(16) NOT NULL,      -- processing / success / failed / expired
    trace_id VARCHAR(64) NULL,        -- 与 Agent 编排层打通
    create_time DATETIME NOT NULL,
    expire_time DATETIME NOT NULL,
    UNIQUE KEY uk_biz_req (biz, request_id)
  );
  ```
  流程改为：插入 `processing` → 业务执行 → 成功置 `success` + 回填 `biz_no`；**失败/异常删除记录（允许重试）**。
- **影响功能**：AI 智能下单、所有 AI 写操作

### ☐ B-02 为 12 类无幂等的写接口补齐幂等

- **现状**（全量 grep `checkAndOccupy` 只有 10 个调用点，**全是库存类单据**）：
  | 缺失幂等的接口                                                               | 位置                             | 风险                  |
  | --------------------------------------------------------------------- | ------------------------------ | ------------------- |
  | `POST /api/purchase/order/create`                                     | `PurchaseOrderService.java:93` | **重复采购下单（智能下单主路径）** |
  | `POST /api/sales/order/create`                                        | `SalesOrderService`            | 重复销售接单              |
  | `POST /api/mrp/calculate`                                             | `MrpService.java:61`           | 重复生成计划与建议           |
  | `POST /api/mrp/confirm`                                               | `MrpConfirmService.java:35`    | **重复生成采购单/工单**      |
  | `POST /api/sales/invoice/create`、`purchase/invoice/create`            | —                              | 重复开票                |
  | `POST /api/sales/verification/create`、`purchase/verification/create`  | —                              | 重复核销                |
  | `POST /api/finance/receipt/writeoff`                                  | `ReceiptService.java:79`       | **重复核销应收**          |
  | `POST /api/finance/payment/writeoff`                                  | `PaymentService.java:79`       | **重复核销应付**          |
  | `POST /api/inventory/stocktake/adjust`                                | `StocktakeService`             | 重复库存调整              |
  | `POST /api/quality/apply/create`、`quality/judge`                      | —                              | 重复质检                |
  | `POST /api/sales/stocking/create`、`packing/create`、`afterSale/create` | —                              | 重复建单                |
  | `POST /api/sales/designCenter/create`、`purchase/inquiry/create` 等     | —                              | 重复建单                |
- **修法**：统一在 Controller 层加 `requestId` 参数（或 `@RequestHeader("Idempotency-Key")`），Service 首行调用 `checkAndOccupy`。建议做成 **AOP 注解 `@Idempotent(biz="purchase_order")`**，避免逐个手改漏掉。
- **影响功能**：AI 智能下单、AI 助手所有写操作

### ☐ B-03 删掉 `PurchaseReturnService` 幂等键里的时间戳

- **文件**：`service/PurchaseReturnService.java:71`
- **现状**：`checkAndOccupy(request.getOrderId() + "_" + System.currentTimeMillis(), "purchase_return")` → **每次键都不同，幂等完全失效**。
- **修法**：改为 `checkAndOccupy(request.getRequestId(), "purchase_return")`。
- **影响功能**：AI 智能下单（退货场景）

### ☐ B-04 定义 Agent 幂等契约并让前端接入

- **现状**：后端有能力，但**前端 12 个 api 文件没有任何一处传 `requestId`**（已全量确认）→ 现有幂等能力实际未被使用。
- **修法**：
  1. 前端把 `crypto.randomUUID()` 从"点击时生成"改为"**弹窗打开时生成一次**"（现在 11 处都是在 `handleXxx()` 内部现生成，连点产生不同 key，等于没做）。
  2. 为 Agent 编排层定义：`requestId` 由编排层生成，与 `trace_id` 绑定，写入 `idempotent_request.trace_id`。
- **影响功能**：全部

---

## 批次 2 · 修通 MRP → 采购单链路【智能下单的直接前置】

### ☐ B-05 `MrpConfirmService` 生成的采购单 `totalAmount` 恒为 0 → 下游永久断链

- **文件**：`service/MrpConfirmService.java:93`（置 `BigDecimal.ZERO`）、`:101`（明细 `price=0`）
- **现状**：插入后**从不重算金额**。下游 `PurchaseInvoiceService.create` 校验 `开票额 ≤ totalAmount − 已开票 − 待审 = 0` → **MRP 生成的采购单永远无法开票** → 无法生成应付 → 采购→发票→应付链路断死。
- **修法**：生成明细后按 `Σ(qty × price)` 回写 `totalAmount`；明细 `price` 缺省时取 `material.standardCost` 或该物料最近一次采购价，而不是 0。
- **影响功能**：AI 智能下单（这是它的主路径）

### ☐ B-06 `MrpConfirmService` 生成的采购单 `supplierId = null`，绕过供应商必填校验

- **文件**：`service/MrpConfirmService.java:86`
- **现状**：直接 `insert`，**没走 `PurchaseOrderService.create`**（那里有 `supplierId == null` 校验，`:94`）→ 空供应商落库 → `Payable.supplierId` 为空、按供应商统计全错。
- **修法**：接入供应商决策（见 B-21），无合适供应商时**不要建单**，转为"待人工指定供应商"的待办。
- **影响功能**：AI 智能下单

### ☐ B-07 `MrpService` 的 `supplyType` 缺少"无 BOM → purchase"分支（死路 bug）

- **文件**：`service/MrpService.java:209-215`
- **现状**：product 分支**无条件** `supplyType = "make"`，注释写着"产品默认自制（如果无 BOM 则采购）"，但**代码里没有这个分支**。
- **后果**：无 BOM 的产品被建议"自制" → 转成无 BOM 的生产工单 → 领料时 `MaterialIssueService` 抛"该工单无 BOM，无法领料" → **死路**。Agent 拿到这种建议会执行错误动作。
- **修法**：查该产品是否存在 `status=1` 的 BOM，无则 `supplyType = "purchase"`。
- **影响功能**：AI 智能下单、数据 AI 分析

### ☐ B-08 `MrpConfirmService` 无锁 + 未校验计划状态 + 可跨计划确认

- **文件**：`service/MrpConfirmService.java:35-80`
- **现状**：
  1. `convertedStatus == "none"` 检查**无行锁、无唯一约束** → 并发重复转单；
  2. **未校验 `mrp_plan.status`** → 已 `confirmed` 的计划可重复确认；
  3. **未校验 `requirement.planId == request.planId`** → 可跨计划确认。
- **修法**：对 `mrp_requirement` 行加 `FOR UPDATE` 或改成原子条件更新 `UPDATE mrp_requirement SET converted_status=?, converted_qty=? WHERE id=? AND converted_status='none'` 并检查影响行数；补计划状态与 planId 校验。
- **影响功能**：AI 智能下单

### ☐ B-09 `MrpService.calculate` 无幂等 + O(N×M) 查询

- **文件**：`service/MrpService.java:61`（无幂等）、`:98-105`（逐单查明细）、`:278-291`（**每条需求都全量拉一次已审批采购单再逐单查明细**）、`:258-262`（逐条需求查库存）
- **现状**：Agent 会频繁触发计算，重复点会生成多份 plan；且性能随数据量迅速劣化。
- **修法**：补幂等（B-02）；`calcInTransitQty` / `calcAvailableQty` 改为按 `itemId + itemType` **批量预取一次**，在内存里按需求分配。
- **影响功能**：AI 智能下单

---

## 批次 3 · 数据正确性【影响所有 AI 分析类功能】

### ☐ B-10 `CostService` 材料成本漏乘工单数量（量纲错误）

- **文件**：`service/CostService.java:82-94`（**我已亲自复核**）
- **现状**：`calcMaterialCost` 里 `total += qty × (1+lossRate) × cost`，**没有乘工单数量**。`bomItem.quantity` 是单件用量 → 算出的是**单件材料成本**；而 `laborCost`/`overheadCost`（`:50-51`）是**整单**的。三者相加再除以数量得 `unitCost` → **材料成本被低估约"工单数量"倍**。
- **修法**：
  ```java
  // 每行修改为：
  total = total.add(qty.multiply(order.getQuantity())
                       .multiply(BigDecimal.ONE.add(lossRate)).multiply(cost));
  ```
- **影响功能**：**数据 AI 分析助手（成本类分析全部失真）**、AI 智能下单（毛利/经济批量决策）

### ☐ B-11 `loss_rate` 口径跨模块冲突 → 齐套分析虚高 100 倍

- **文件**（按小数 `1+lossRate`）：`MrpService.java:166-168`、`MaterialIssueService.java:142-146`、`CostService.java:89-91`
- **文件**（按百分比 `1+loss/100`）：`CompleteAnalysisService.java:55-58`
- **现状**：DB 定义 `loss_rate DECIMAL(10,6) DEFAULT 0 COMMENT '损耗率'`，**无单位说明**。
- **后果**：若录入 `0.05` 表示 5%，齐套分析当 500% 算 → **需求量放大 100 倍** → Agent 看到"严重缺料"疯狂下采购单。
- **修法**：**统一为小数**（改 `CompleteAnalysisService` 去掉 `/100`），并在 `schema.sql` 注释里明确"0.05 = 5%"，同时在前端损耗率输入框加提示。
- **影响功能**：**AI 智能下单（会误判缺料）**、AI 交期预警（齐套率错误 → 交期预测错误）、数据 AI 分析

### ☐ B-12【需要你决策】明确成本口径：标准成本 vs 移动加权

- **现状**：
  - `CostService` 只读 `material.standard_cost` / `product.standard_cost` → **只有标准成本法**；
  - `inventory.unit_cost` / `inventory.total_cost` 两个字段**全项目从未被写入**（死字段，注释自己写"V1 预留"）；
  - 无批次成本层、无期间成本、无成本台账表。
- **为什么必须现在定**：AI 做成本类分析（毛利、建议售价、经济批量、成本异常预警）**必须先有一个确定的成本口径**。走移动加权需要给库存加成本层，是个不小的架构分叉，**越晚定越贵**。
- **建议**：如果只做"分析/预警"（读为主）→ 先保留标准成本法，修完 B-10 就够用；如果要做到"实际成本 vs 标准成本差异分析"→ 需要引入批次成本层。
- **影响功能**：数据 AI 分析助手（决定性）、AI 智能下单

### ☐ B-13 单据明细累计字段"读-改-写"无锁 → 超发/超收/超领/超退（4 处同构）

| 位置                                           | 症状                         |
| -------------------------------------------- | -------------------------- |
| `service/SalesOutboundService.java:84-125`   | 超发（累计出库 > 订单量）             |
| `service/PurchaseInboundService.java:86-133` | 超收                         |
| `service/MaterialIssueService.java:111-153`  | 超领（`calcAlreadyIssued` 无锁） |
| `service/SalesReturnService.java:97-107`     | 超退（`sumReturnedQty` 无锁）    |

- **现状**：`selectById` 读出（**无 FOR UPDATE**）→ 校验 `本次量 ≤ quantity − 已累计` → `set(旧值 + 本次)` → `updateById`。两个并发请求都读到旧值、都通过校验。
- **关键**：库存 `change()` 只保证**不超库存台账**，**不保证不超订单**。
- **为什么 AI 会放大**：人单线操作几乎不触发；**Agent 并行工具调用必触发**。
- **修法**（推荐，无需加锁）：
  ```sql
  UPDATE sales_order_item
     SET outbound_qty = outbound_qty + #{qty}
   WHERE id = #{id} AND outbound_qty + #{qty} <= quantity;
  ```
  检查影响行数 `== 1`，为 0 则抛"出库量超出订单剩余量"。或对明细行加 `SELECT ... FOR UPDATE`。
- **影响功能**：AI 智能下单、AI 助手所有出入库操作

### ☐ B-14 `InventoryService.release()` 无锁，与 `reserve()` 保护不对称

- **文件**：`service/InventoryService.java:410-427`
- **现状**：`release()` 用 `inventoryMapper.selectById`（**无 FOR UPDATE**）读库存行后 `updateById` 覆盖写 `reservedQty`；而 `reserve()`（`:372-407`）是有 `FOR UPDATE` 的。**并发锁库 + 释放会丢更新。**
- **修法**：`release()` 补 `selectForUpdate` + 同样的 Redis 锁。
- **影响功能**：AI 智能下单（预留/释放是它的核心动作）

### ☐ B-15 Redis 库存锁：加 owner 校验 + 固定加锁顺序

- **文件**：`service/InventoryService.java:102-124`（`tryLock`）、`:473-480`（`releaseLock`）
- **现状**：
  1. TTL 硬编码 `10s`，长事务可能超租约；
  2. `releaseLock` 是**裸 `delete`，无 owner 校验** → 可能删掉别的线程刚获取的锁（经典"锁过期 + 盲删"）；
  3. 多维度加锁顺序不固定（`TransferService.create` 先源仓后目标仓）→ 双向调拨并发时互相等到超时，退化成 409 假失败。
- **修法**：`setIfAbsent(key, UUID.randomUUID()...)`，释放用 Lua 脚本比对 value 再删；TTL 提到 30-60s 并加续租；**加锁顺序按维度 key 字典序排序**。
- **影响功能**：AI 智能下单（高并发场景）

### ☐ B-16 `InventoryService` 没读 `negative_stock_strategy` 配置

- **文件**：`service/InventoryService.java:142-144`（硬编码抛"库存不足"）
- **现状**：`sys_config.negative_stock_strategy = 'forbid'` 已存在，**代码从未读取** → 策略不可切换。
- **修法**：接上配置（注意 `SysConfigService` 要先做 B-30 的 fail-closed + 缓存改造）。
- **影响功能**：AI 智能下单（策略开关）

---

## 批次 4 · AI 交期预警【专属，含真实缺数据问题】

> **这一批是我实测新发现的，不在之前的通用清单里。** 交期预警不是"调个模型"能解决的，它对数据有硬要求。

### ☐ B-17【阻断】工作日历 `work_calendar` 表存在，但**全项目没有任何代码读它**

- **实测证据**（全量 grep）：`WorkCalendarMapper` / `WorkCalendarService` 只被 `WorkCalendarController` 自己（CRUD）引用，**MRP、排产、提前期计算全部忽略工作日历**。
- **后果**：任何提前期倒推（`MrpService:232-235`）、任何交期预测都会落在周末和节假日上，**AI 算出的交期系统性偏早**。
- **修法**：抽一个 `WorkCalendarService.addWorkDays(date, days)` / `workDaysBetween(from, to)`，让 MRP 提前期倒推、排产、交期预测统一走它。
- **影响功能**：**AI 交期预警（阻断级）**

### ☐ B-18【阻断】没有任何调度器

- **实测证据**：全项目 **无 `@EnableScheduling`、无 `@Scheduled`、无 Quartz、无 xxl-job、无 `@Async`、无线程池配置**。
- **后果**：交期预警需要**定时扫描**（每天算一遍哪些订单将逾期）→ 基础设施完全没有。同时 LLM 调用耗时数秒，没有异步能力会阻塞 Tomcat 线程。
- **修法**：加 `@EnableScheduling` + `@Async` + 线程池配置（或引入 xxl-job 便于分布式与可视化管理）。注意 V1 文档曾写"不增加后台定时机制"，现在需要正式立项。
- **影响功能**：**AI 交期预警（阻断级）**、所有 AI 长任务

### ☐ B-19【缺口】`production_order` 没有实际开工/完工日期 → 无法用历史实绩校准交期

- **实测**：`entity/ProductionOrder.java` 只有 `planDate`（计划开始）、`planFinishDate`（计划完工），**没有 `actualStartDate` / `actualFinishDate`**。
- **后果**：交期预测最有效的特征就是"这个产品历史上实际做了多少天"。现在只能用 `createTime` 到 `production_inbound.inboundDate` 近似，且没有"实际开工日"，**首工序何时开始无法还原**。
- **对比（好消息）**：采购侧**可以推导** —— `purchase_order.orderDate` + `purchase_inbound.inboundDate` = 实际到货周期，`expectedDate` 对比 `inboundDate` = 是否准时。**采购侧的准时率、实际提前期是能算出来的。**
- **修法**：给 `production_order` 加 `actual_start_date`、`actual_finish_date`，在 `start` / `finish` / 入库时回填。
- **影响功能**：**AI 交期预警（精度取决于此）**


### ☐ B-20【缺口】`machine` 没有产能字段 → 无产能约束建模基础

- **实测**：`entity/Machine.java` 有 `type`/`model`/`equipmentStatus`/`maintenanceCycle`，**没有日产能、每日工时、效率系数、可用率**。
- **现有可用资产**：`process_route_item.standard_hours`（工序标准工时）、`process_route_item.is_key`（关键工序）、`process.standard_price`。**这是排产/交期测算的好基础**，缺的是"机台每天能干多少小时"。
- **修法**：给 `machine` 加 `daily_hours`（每日可用工时）、`efficiency`（效率系数）；配合 B-17 的工作日历即可算产能。
- **影响功能**：**AI 交期预警**

### ☐ B-21 提前期缺省值不一致，且无供应商实际提前期聚合

- **现状**：
  - `MrpService` 里缺省提前期是 **0**（`:201`、`:207`）；
  - `WarningActionService` 里缺省是 **7 天**（`:49`，魔法数字）；
  - `material.purchase_lead_days` / `product.production_lead_days` 字段存在但**是人工维护的计划值，没有任何地方回写实际值**。
- **修法**：
  1. 统一缺省值（建议走 `sys_config`，不写死）；
  2. 新增供应商履约聚合（**数据可推导，不用新建表**）：按 `(supplierId, materialId)` 聚合
     `AVG(purchase_inbound.inbound_date − purchase_order.order_date)` = 实际平均提前期；
     `SUM(inbound_date <= expected_date) / COUNT(*)` = 准时率；
     作为 AI 交期预测的特征，也作为"智能选供应商"的打分依据。
- **影响功能**：**AI 交期预警**、AI 智能下单（选供应商）

### ☐ B-22 `CompleteAnalysisService` 只做一级 BOM 展开 + N+1

- **文件**：`service/CompleteAnalysisService.java:53-87`
- **现状**：只按工单 BOM**一级**展开算需求，逐条调 `inventoryService.availableQty`（N+1）。多层 BOM 未展开 → 齐套率虚高。
- **修法**：复用 `MrpService.expandBom` 的多级展开逻辑（那段写得不错），改为批量查库存。
- **影响功能**：**AI 交期预警（齐套率是交期的核心输入）**

### ☐ B-23 `MpsPlanService` 名不符实，`generateWorkOrder` 状态校验过松

- **文件**：`service/MpsPlanService.java:32-70`
- **现状**：**没有任何 MPS 运算**（无预测、无 ATP、无毛净需求、无产能校验）；`generateWorkOrder` 只阻断 `status == "confirmed"`，**不要求 `status == "planned"`** → 任何其他状态都能转工单。
- **修法**：若交期预警需要"计划层"数据，先明确 MPS 的实际定位（要么补运算，要么改名为"计划登记"）。
- **影响功能**：AI 交期预警、AI 智能下单

---

## 批次 5 · 审计与回滚【放开自动化的前置条件】

### ☐ B-24 `operation_log` 建了表，但**全项目无实体、无 Mapper、从来没人写**

- **现状**：`schema.sql` 建了 `operation_log`（含 `before_state` / `after_state` / `ip`），**没有任何代码写它**。
- **为什么对 AI 是硬要求**：AI 自动化上线后必须能回答——**谁触发的？Agent 看到了什么上下文？依据什么规则？执行了什么？结果如何？能否回滚？** 没有这个，出事后既无法定责也无法复盘，业务方不会信任自动化。
- **修法**：加实体 + Mapper + AOP 切面；**所有 Agent 发起的写操作强制留痕**，至少记录 `trace_id / tool_name / 入参 / Agent 推理摘要 / 执行结果 / 影响行数`。
- **影响功能**：全部（这是 AI 落地的合规底线）

### ☐ B-25 `approval_record` 建了表，但从来没人写 → 审批链缺失

- **现状**：审批只有单据上的 `approve_status` 字符串，**没有审批痕迹表**（谁审的、什么时候、什么意见）。
- **修法**：在 11 处 `checkSelfApprove` / `approve` 落库时同步写 `approval_record`。
- **影响功能**：AI 智能下单（human-in-the-loop 需要审批留痕）

### ☐ B-26 库存类单据 `status` 默认即终态，**无草稿/无中间态/无作废红冲**

- **实测**：以下单据建表 `status DEFAULT 'completed'`，落库即完成，**无作废接口**：
  ```
  sales_outbound   purchase_inbound   production_inbound
  material_issue   material_return    transfer
  other_stock      assembly_order
  ```
- **为什么这是"敢不敢放开自动化"的开关**：Agent 一定会犯错（幻觉数量、选错物料、重复下单）。**没有红冲 = 出错后只能人工进 SQL 手改**，工业场景不可接受。
- **修法**：给这批单据加 `draft → completed → voided` 状态机 + 作废接口（作废时反向冲销库存并写 `inventory_transaction` 冲销流水）。
- **影响功能**：全部 AI 写操作

### ☐ B-27 `ProductionOrderService.voidOrder` 太宽松

- **文件**：`service/ProductionOrderService.java:214-220`
- **现状**：只阻断 `completed`；对 `producing`（已领料、可能已部分入库）的工单能直接作废，**不回滚领料、不释放库存、不联动 MPS 计划状态、不检查外协单**。
- **修法**：作废前校验未入库/未领料，或提供"作废并冲销"路径。
- **影响功能**：AI 智能下单、AI 助手

### ☐ B-28 `attachment` 建了表，但从来没人写 → **单据无法挂附件**

- **现状**：`attachment` 表存在，无实体无 Mapper。
- **为什么和你的知识库相关**：如果知识库要收录**业务单据附件**（合同扫描件、图纸、报价单、质检报告），当前**没有任何附件上传/关联能力**。
- **修法**：若知识库只收外部文档（PDF/Excel/Word），**这条不阻塞**；若要收单据附件，需要补文件上传 + `attachment` 落地。
- **影响功能**：知识库问答系统（条件性）

---

## 批次 6 · Agent 安全边界【决定 Agent 能不能上线】

### ☐ B-29【重大】29 处直接把实体当 `@RequestBody` → 转成 tool schema 会暴露全部字段

- **实测 grep 命中**（14 个 Controller，共 29 处）：

  | Controller | 行号 |
  |---|---|
  | `UserController` | `:31` `:36` |
  | `CustomerController` | `:30` `:36` |
  | `SupplierController` | `:30` `:36` |
  | `MaterialController` | `:30` `:36` |
  | `ProductController` | `:30` `:36` |
  | `WarehouseController` | `:30` `:36` |
  | `RoleController` | `:27` `:32` |
  | `DepartmentController` | `:26` `:31` |
  | `DictionaryController` | `:34` `:39` |
  | `MachineController` | `:38` `:45` |
  | `WorkshopController` | `:36` `:43` |
  | `LocationController` | `:37` `:43` |
  | `AreaController` | `:37` `:44` |
  | `SerialRuleController` | `:36` `:43` |
  | `UnitController` | `:36` `:43` |

- **具体风险**：把 OpenAPI 转成 tool schema 时，这些实体的**所有字段**都会变成可写参数。Agent（或被 prompt injection 诱导的 Agent）可以直接：
  ```
  POST /api/system/user/create
  { "username":"x", "password":"<可控hash>", "status":1, "id":<任意值>, "deleted":0 }
  ```
- **修法**（两层）：
  1. **给 Agent 的 tool 层单独建白名单 DTO**，只暴露确实需要的字段；**绝不让 Agent 直接调这些实体入参接口**；
  2. 长期：把这些接口逐步改为专用 Request DTO + `@Valid` 校验。
- **影响功能**：全部（这是 AI 安全的第一道门）

### ☐ B-30 补 `@Valid` / JSR303 参数校验

- **现状**：`pom.xml` 里 `spring-boot-starter-validation` **已引入**，但全项目 **只有 `LoginRequest` 用了校验注解**（305 个端点中仅 1 处 `@Valid`）。
- **对 AI 的影响**：tool schema 质量直接取决于 DTO 上的约束注解。没有注解，LLM 不知道哪些字段必填、取值范围是什么，**会频繁传错参数**，然后陷入重试。
- **修法**：给 tool 层 DTO 全部补 `@NotNull` / `@NotBlank` / `@DecimalMin` / `@Positive` 等；顺带覆盖现有 DTO。
- **影响功能**：全部

### ☐ B-31 引入结构化错误码（现在缺 409 / 422 分支）

- **现状**：`common/GlobalExceptionHandler.java` 只实现 **400/401/403/500**，**没有 404、没有 409、没有 422**（而 V1 需求文档里定义了 409/422）。且业务失败一律 **HTTP 200 + body 内 code**（只有 401/403 是真状态码）。
- **对 AI 的影响**：Agent 无法区分「参数错（重试无用）」「库存不足（可稍后重试）」「重复提交（不该重试）」「并发冲突（应退避）」，**只能从 `message` 字符串里猜语义**。
- **修法**：定义结构化错误码，建议：
  ```
  400xx 参数错误（不可重试）
  409xx 状态冲突 / 并发冲突（应退避重试）
  422xx 业务校验失败（修正入参后重试）
  429xx 限流（退避重试）
  500xx 系统异常
  ```
  并在 `Result` 里增加 `retryable` 布尔字段供编排层直接判断。
- **影响功能**：全部（Agent 的失败处理策略全靠它）

### ☐ B-32 实现数据权限（`data_scope` 建了但从未使用）

- **现状**：`Role.data_scope` 字段存在（all/dept/self，`init_data.sql` 里默认 `self`），但**从未参与任何查询过滤**。当前只有 `admin` 一个账号（全部权限）。
- **修法**：
  1. 建**服务账号**（service account）+ 最小权限角色 + **工具级 scope**（每个 tool 对应独立权限点）；
  2. 把 `data_scope` 落进 `LambdaQueryWrapper`（all/dept/self 三种过滤），服务层加单据归属校验。
- **影响功能**：全部（Agent 用哪个账号决定了它能看到什么、能做什么）

### ☐ B-33 `checkSelfApprove` 在 `createBy == null` 时静默放行

- **现状**：11 处调用逻辑均为 `createBy != null && createBy.equals(currentUser)` → **`createBy` 为 null 时直接放行**。
- **对 AI 的影响**：Agent 若在无用户上下文的场景建单（例如后台定时任务、系统触发），**会自动绕过全部审批风控**。
- **反向利用（正向设计）**：若让 Agent 用同一账号建单又审批，`allow_self_approve=false` 会挡住它 —— **这正是你要的 human-in-the-loop 阀门**。但要**显式设计**（Agent 账号只能 `create` + `submit`，不给 `approve` 权限），不能靠碰运气。
- **修法**：`createBy == null` 时改为 **fail-closed**（拒绝自审判断）或明确"系统触发不适用自审规则"并记录审计。
- **影响功能**：AI 智能下单（审批风控）

### ☐ B-34 `DictionaryController` / `DashboardController` 完全无权限注解

- **现状**：`DictionaryController`（4 个端点）**无 `@PreAuthorize`** → 任意登录用户可**增改字典**；`DashboardController`（2 个端点）无权限 → 任意登录用户可看**应付/已付/库存金额等财务敏感 KPI**。
- **修法**：字典写操作加权限点；驾驶舱财务 KPI 单独权限点（你可以顺便放进"数据 AI 分析助手"的权限体系）。
- **影响功能**：数据 AI 分析助手（财务数据越权）

### ☐ B-35 凭证与密钥治理

| 项 | 位置 | 修法 |
|---|---|---|
| JWT 密钥有硬编码默认值 | `application.yml:33` | 移除默认值，强制从环境变量注入 |
| DB 密码默认 `<已移除，见 .env 的 DB_PASSWORD>`、Redis 无密码 | `application.yml:11-15` | 拆出生产 profile，不设明文默认 |
| 密码经 URL query 传递 | `UserController` reset-password | 改为 `@RequestBody` |
| Swagger 无 profile 隔离 | `application.yml:36-41` | 生产环境关闭或加鉴权 |
| 登录无失败次数限制/锁定/验证码 | `AuthService.java:22-41` | 加限流与锁定（Agent 场景尤其需要防爆破） |
| `JwtAuthFilter` 里 `catch (Exception ignored) {}` | `JwtAuthFilter` | 改为记日志，便于观测异常尝试 |

- **影响功能**：全部

---

## 批次 7 · 性能【数据 AI 分析助手会放大】

### ☐ B-36 `DashboardService.cockpit()` 全表加载进 JVM 内存聚合

- **文件**：`service/DashboardService.java:90`、`:107`、`:119`、`:129`、`:140`
- **现状**：连续 5 次 `selectList(null)` 把 `sales_order` / `production_order` / `purchase_order` / `inventory` / `inventory_transaction` **全表**拉进 JVM 聚合。`stats()`（`:61-79` + `:211-224`）还有逐物料查库存的 N+1。
- **后果**：上万行变慢，十万级 **OOM**。AI 分析助手会高频调用这类聚合接口。
- **修法**：改为 `GROUP BY` 聚合 SQL。
- **影响功能**：数据 AI 分析助手（决定性）

### ☐ B-37 其余 N+1 清单（AI 会高频触发）

| 位置 | 形态 |
|---|---|
| `MrpService.calcInTransitQty:278-291` | 每条需求 × 全量已审批采购单（**最严重**） |
| `MaterialIssueService.calcAlreadyIssued:155-166` | 循环领料单 → 每单查明细 |
| `SalesReturnService.sumReturnedQty:130-142` | 循环退货单 → 每单查明细 |
| `WorkCalendarService.batchSet:52-53` | 按天 `selectOne`（单月 30 次） |
| `CompleteAnalysisService:53-87` | 每个 BOM 子件 2 次查询 |
| `StocktakeService.create:95-106` | 整仓库存逐条 insert |
| `RoleService.assignPermissions:63-68` | 循环单条 insert |
| `BomService.saveItems:155-164` / `ProcessRouteService.saveItems:119-133` | 循环单条 insert |
| `SecurityUtils.currentUserId:30-39` | 每次调用回查 user 表（token 里已有 userId） |

- **修法**：批量预取 + `saveBatch`。**`NameFillUtil` 已经做了正确的批量范式，照它写。**

### ☐ B-38 关闭 SQL 全量打印

- **文件**：`application.yml:24` → `log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`
- **后果**：每条 SQL 打进标准输出，生产必刷屏（`backend.log` 已 5.3MB）。
- **修法**：改为 `Slf4jImpl` 并按 profile 控制日志级别。

### ☐ B-39 `ProcessRouteItem` / `WorkCalendar` 缺 `@TableId` → 雪花 ID 与自增混用

- **现状**：两个实体既不继承 `BaseEntity`，也没有 `@TableId`；`application.yml` 未配 `id-type` → MyBatis-Plus 默认 `ASSIGN_ID`，插入时**显式写雪花 ID**，与其余 99 张表的 `AUTO_INCREMENT` 混用。
- **修法**：补 `@TableId(type = IdType.AUTO)`，或全局配置 `id-type: auto`。
- **影响功能**：数据 AI 分析（主键数量级不一致会让 AI 困惑，也会污染时序分析）

---

## 批次 8 · 前端断链【后端有、界面没有，2026-10-04 实测】

### ☑ B-40 生产工单页缺「工单开始」/「完工」按钮 → **成品工单无法报工**（2026-10-04 已修复）

- **现状**：
  - 后端 `ProductionOrderController` 有 `/start`、`/finish`；前端 `api/production.ts:20/23` 也导出了 `productionOrderStart` / `productionOrderFinish`
  - 但**只有 `views/production/semiFinished.vue:134-135`（半成品加工单）调用了它们**。`views/production/order.vue:47-57` 操作列只有 提交/审批通过/领料/退料/入库/反审核/打印
- **后果（完整链条）**：`ReportWorkService.create:69-71` 硬性要求 `bizStatus == "producing"` 才能报工 → 成品工单永远停在 `pending` → **成品工单在界面上无法报工、无法审工、无法查工序进度**
- **修法**：在 `order.vue` 操作列补两个按钮：`bizStatus === 'pending'` 时显示「工单开始」，`'producing'` 时显示「完工」
- **✅ 2026-10-04 已修**：`order.vue` 新增 `handleStart()` / `handleFinish()`（带二次确认）+ 两个按钮（操作列宽 400→500）；import 补 `productionOrderStart` / `productionOrderFinish`；顺带补全 `bizText()` 缺失的 `finished` / `voided` 映射
- **影响功能**：报工链路（成品）、AI 交期预警（拿不到真实生产进度）

### ☑ B-41 询价单「比价生单」无前端入口 → **询价单转不了采购订单**（2026-10-04 已修复）

- **现状**：后端 `PurchaseInquiryController:75-80` 有 `/toPurchaseOrder`（`supplierId` 为空时自动选最低总价）；但前端 `api/purchase.ts:53-68` 的 inquiry 系列**没有 `toPurchaseOrder` 封装**，`views/purchase/inquiry.vue` 操作列只有 提交/审批/反审核/明细
- **后果**：询价单审批后界面上没有任何入口转采购订单，采购订单只能手工新建——「采购询价」这条闭环在界面上是断的
- **修法**：补 `api/purchase.ts` 的 `purchaseInquiryToOrder` 封装 + `inquiry.vue` 操作列按钮
- **✅ 2026-10-04 已修**：`api/purchase.ts` 加 `purchaseInquiryToOrder(inquiryId, supplierId?)`；`inquiry.vue` 操作列加「比价生单」按钮（仅已审批且未比价时显示）+ `toOrder()` 函数 + 新增「比价状态」列（操作列宽 200→290）
- **影响功能**：采购询价闭环、AI 智能下单（询价→订单自动化的基础）

---

## 批次 8 · 知识库问答系统专属

> **好消息：这一批几乎不受现有 bug 影响。** 这是你的四个功能里唯一可以**立刻开工**的。

### ☐ B-40 依赖项清单（多数是新增，不是修 bug）

| 依赖 | 现状 | 要做的事 |
|---|---|---|
| 文档存储 | **绿地**，`attachment` 表未使用 | 若只收外部文档（PDF/Word/Excel）→ 独立文档服务即可，**不影响 ERP** |
| 向量库 | 无 | 新增（Milvus / pgvector / ES / 云服务） |
| 只读业务数据源 | **无只读账号**，现在是 `root / <见 .env 的 DB_PASSWORD>` | **必做**：建只读账号 + 白名单视图 |
| Auth 集成 | 有 JWT，但无数据权限 | 走批次 6 的 B-32 |
| 单据附件关联 | `attachment` 表无实体无 Mapper | **仅当**知识库要收录单据附件时才做（见 B-28） |

### ☐ B-41【红线】绝不让 LLM 直连数据库或自由写 SQL

- **必须的隔离层**：
  ```
  LLM
   ↓ 只能调用预定义参数的「语义查询工具」（不是自由 SQL）
  只读数据源（独立只读账号，只授予白名单视图）
   + SQL AST 校验（禁 DML/DDL/多语句/UNION 越权）
   + 强制 LIMIT + 语句超时 + 结果行数上限
   + 敏感字段脱敏（成本、毛利、银行账号、客户联系方式）
  ```
- **为什么现在特别危险**：当前 DB 账号是 `root`，一旦注入即全库沦陷。

---

## 附 · 建议的执行顺序（按依赖关系）

```
第 1 周  批次 1（幂等）        ← 所有 AI 写操作的地基，先做
第 2 周  批次 2（MRP 链路）    ← 智能下单的直接前置
并行可做 批次 8（知识库）      ← 纯读，零风险，先把 Agent 框架/鉴权/审计跑通
第 3 周  批次 6（安全边界）    ← Agent 上线前的门
第 4 周  批次 5（审计+红冲）   ← 放开自动化的开关
第 5 周  批次 3（数据正确性）  ← 数据分析助手前提
第 6 周  批次 4（交期预警专属）← 需要补字段和调度器，工期最长
持续    批次 7（性能）        ← 随数据量增长逐步做
```

**判断标准**：
- 只要做**只读问答** → 只需批次 6 + 8
- 要做到**Agent 建单 + 人审批** → 还需要批次 1 + 2 + 5
- 要做到**数据 AI 分析** → 还需要批次 3 + 7
- 要做到**交期预警** → 还需要批次 4

---

## 附 · 已实测确认的有利条件（修 bug 时顺手复用）

| 资产 | 位置 | 用途 |
|---|---|---|
| **审批状态机已全单据统一** | `pending → approving → approved` | **天然的 human-in-the-loop**，不用自造停机确认 |
| **库存唯一入口** | `InventoryService.change()`（已验证无绕写） | **护栏有地方注入**：限额、白名单、二次校验、审计钩子 |
| **采购侧交期实绩可推导** | `purchase_order.orderDate` + `purchase_inbound.inboundDate` + `expectedDate` | 直接算供应商平均提前期与准时率，**不用新建表** |
| **比价逻辑已存在** | `PurchaseInquiryService.bestSupplier`（按总价取最低）+ `purchase_inquiry_item` 存多供应商报价 | AI 选供应商的现成数据基础 |
| **工序标准工时已存在** | `process_route_item.standard_hours` / `is_key` | 排产与交期测算的基础 |
| **批量取名的正确范式** | `NameFillUtil`（16 个 `xxxMap()` 方法） | 修 N+1 时照它写 |
| **统一返回包装** | `Result<T>` / `PageResult<T>`（305/305） | Agent 工具适配器可统一封装 |
| **OpenAPI 已配置** | springdoc 2.6.0 + `OpenApiConfig` | tool schema 可自动生成（但按 B-29 必须裁剪） |
| **`pom.xml` 无任何 AI 依赖** | — | 完全绿地，无历史包袱 |

---

*本清单基于 2026-09-16 代码快照。凡标注「实测」的条目均已通过代码读取或 SQL 实探确认；凡标注「需你决策」的条目为架构分叉，不是 bug。*
