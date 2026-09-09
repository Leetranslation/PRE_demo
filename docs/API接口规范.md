# Token 制造 ERP API 接口规范

> 本规范是后端接口开发的统一契约，所有新增接口必须遵守。
> 活文档：Swagger UI（http://localhost:8080/swagger-ui/index.html），随代码注解自动生成。
> 本文档为书面规范 + 新增接口登记表。

---

## 一、基础约定

| 项 | 约定 |
|---|---|
| 基础路径 | 所有接口以 `/api` 开头 |
| 协议 | HTTP + JSON |
| 字符编码 | UTF-8 |
| 鉴权 | 除 `POST /api/auth/login` 外，均需请求头 `Authorization: Bearer <token>` |
| 权限控制 | 类级 `@PreAuthorize` 控制菜单权限，方法级控制操作权限 |

---

## 二、统一返回结构

### 2.1 通用响应 `Result<T>`

```json
{
  "code": 0,          // 0=成功；401=未登录；403=无权限；400=业务校验失败；404=不存在；500=系统错误
  "message": "success",
  "data": { }          // 业务数据，可为 null
}
```

### 2.2 分页响应 `PageResult<T>`

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "page": 1,
    "pageSize": 10,
    "total": 108,
    "records": [ ]
  }
}
```

### 2.3 错误码约定

| code | 含义 |
|---|---|
| 0 | 成功 |
| 400 | 业务校验失败（参数非法、状态不允许等，抛 `BizException`） |
| 401 | 未登录或登录失效 |
| 403 | 无权限访问 |
| 404 | 资源不存在 |
| 500 | 系统异常 |

---

## 三、接口命名规范

### 3.1 URL 结构

```
/api/{模块}/{资源}/{动作}
```

| 模块 | 前缀 |
|---|---|
| 基础资料 | `/api/base/` |
| 生产数据 | `/api/base/`（工序 process / 工艺路线 processRoute 等） |
| 销售 | `/api/sales/` |
| 采购 | `/api/purchase/` |
| 生产 | `/api/production/` |
| 库存 | `/api/inventory/` |
| 质量 | `/api/quality/` |
| MRP | `/api/mrp/` |
| 系统 | `/api/system/` |
| 认证 | `/api/auth/` |

### 3.2 标准动作（REST 风格 + 业务动作）

| HTTP 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/list` | 分页列表查询 |
| GET | `/items` | 查询单据明细 |
| POST | `/create` | 新增 |
| POST | `/update` | 编辑 |
| POST | `/delete` | 删除（逻辑删除） |
| POST | `/status` | 启用/停用 |
| POST | `/submit` | 提交（进入审批流） |
| POST | `/approve` | 审批通过 |
| POST | `/unapprove` | 反审核 |
| POST | `/judge` | 质检判定 |
| POST | 其他业务动作 | 如批量设置、复核等，用动词命名 |

### 3.3 参数规范

- **分页参数**：`page`（默认 1）、`pageSize`（默认 10）
- **关键字**：`keyword`（模糊匹配名称/编号）
- **状态筛选**：`status`（基础资料：1 启用 / 0 停用）、`approveStatus`（单据审批状态）、`bizStatus`（单据业务状态）
- **ID 参数**：`@RequestParam Long id`（单个）、`@RequestBody`（对象，用于 create/update 含明细的场景）
- **幂等参数**：涉及库存变动的操作传 `requestId`（如入库/出库/领料/退料）

---

## 四、编号规则

| 项 | 规则 |
|---|---|
| 格式 | `业务前缀 + yyyyMMdd + 4位流水` |
| 前缀 | WL物料 / CP产品 / KH客户 / GY供应商 / SO销售订单 / PO采购订单 / MO生产工单 / BOM / QC质检 / RK入库 / CK出库 / MRP计划 / **GX工序 / GY工艺路线**（V2 新增） |
| 说明 | 按日重置流水（每日从 0001 起），删除单据编号不复用 |

---

## 五、分页查询实现范式

```java
// Controller
@GetMapping("/list")
public Result<PageResult<Xxx>> list(
        @RequestParam(defaultValue = "1") long page,
        @RequestParam(defaultValue = "10") long pageSize,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Integer status) {
    return Result.success(PageResult.of(xxxService.page(page, pageSize, keyword, status)));
}

// Service
public Page<Xxx> page(long page, long pageSize, String keyword, Integer status) {
    LambdaQueryWrapper<Xxx> qw = new LambdaQueryWrapper<>();
    if (StringUtils.hasText(keyword)) {
        qw.and(w -> w.like(Xxx::getName, keyword).or().like(Xxx::getCode, keyword));
    }
    if (status != null) qw.eq(Xxx::getStatus, status);
    qw.orderByDesc(Xxx::getId);
    return xxxMapper.selectPage(new Page<>(page, pageSize), qw);
}
```

---

## 六、Swagger 注解规范（新模块必须补）

每个 Controller 必须补注解，作为 API 活文档：

```java
@Tag(name = "工序管理", description = "工序基础数据的增删改查与批量设置")
@RestController
@RequestMapping("/api/base/process")
public class ProcessController {

    @Operation(summary = "分页查询工序")
    @GetMapping("/list")
    public Result<PageResult<Process>> list(...) { ... }

    @Operation(summary = "新增工序")
    @PostMapping("/create")
    public Result<Process> create(@RequestBody ProcessRequest req) { ... }
}
```

- 类上：`@Tag(name = "模块中文名")`
- 方法上：`@Operation(summary = "接口说明")`
- 请求体字段：`@Schema(description = "字段说明")`（实体/DTO 上）
- 路径参数：`@Parameter(description = "说明")`

---

## 七、新增接口登记表

> 每开发一个新模块，在此登记接口清单，与 Swagger 注解保持一致。

### V2 新增：生产数据 - 工序管理（ProcessController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/process/list` | 分页查询工序 | base:process |
| POST | `/api/base/process/create` | 新增工序 | base:process:create |
| POST | `/api/base/process/update` | 编辑工序 | base:process:update |
| POST | `/api/base/process/status` | 启用/停用 | base:process:update |
| POST | `/api/base/process/batchPrice` | 批量设置标准单价 | base:process:update |
| POST | `/api/base/process/batchPriceType` | 批量设置计价方式 | base:process:update |

### V2 新增：生产数据 - 工艺路线（ProcessRouteController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/processRoute/list` | 分页查询工艺路线 | base:processRoute |
| GET | `/api/base/processRoute/items` | 查询工艺路线明细 | base:processRoute |
| POST | `/api/base/processRoute/create` | 新增工艺路线 | base:processRoute:create |
| POST | `/api/base/processRoute/update` | 编辑工艺路线 | base:processRoute:update |
| POST | `/api/base/processRoute/status` | 启用/停用 | base:processRoute:update |
| POST | `/api/base/processRoute/delete` | 删除工艺路线 | base:processRoute:update |

### V2 新增：生产数据 - 车间管理（WorkshopController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/workshop/list` | 分页查询车间 | base:workshop |
| POST | `/api/base/workshop/create` | 新增车间 | base:workshop:create |
| POST | `/api/base/workshop/update` | 编辑车间 | base:workshop:update |
| POST | `/api/base/workshop/status` | 启用/停用 | base:workshop:update |

### V2 新增：生产数据 - 机台设备（MachineController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/machine/list` | 分页查询机台 | base:machine |
| POST | `/api/base/machine/create` | 新增机台 | base:machine:create |
| POST | `/api/base/machine/update` | 编辑机台 | base:machine:update |
| POST | `/api/base/machine/status` | 启用/停用 | base:machine:update |

### V2 新增：生产数据 - 工作日历（WorkCalendarController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/workCalendar/list` | 分页查询工作日历 | base:workCalendar |
| POST | `/api/base/workCalendar/batchSet` | 批量设置工作日 | base:workCalendar:update |

### V2 新增：生产数据 - 单位管理（UnitController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/unit/list` | 分页查询单位 | base:unit |
| POST | `/api/base/unit/create` | 新增单位 | base:unit:create |
| POST | `/api/base/unit/update` | 编辑单位 | base:unit:update |
| POST | `/api/base/unit/status` | 启用/停用 | base:unit:update |

### V2 新增：生产数据 - 多单位转换率（UnitConversionController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/unitConversion/list` | 分页查询换算率 | base:unitConversion |
| POST | `/api/base/unitConversion/create` | 新增换算率 | base:unitConversion:create |
| POST | `/api/base/unitConversion/update` | 编辑换算率 | base:unitConversion:update |
| POST | `/api/base/unitConversion/status` | 启用/停用 | base:unitConversion:update |

### V2 新增：生产数据 - 序列号规则（SerialRuleController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/serialRule/list` | 分页查询规则 | base:serialRule |
| POST | `/api/base/serialRule/create` | 新增规则 | base:serialRule:create |
| POST | `/api/base/serialRule/update` | 编辑规则 | base:serialRule:update |
| POST | `/api/base/serialRule/status` | 启用/停用 | base:serialRule:update |

### V2 新增：基础资料 - 库区管理（AreaController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/area/list` | 分页查询库区 | base:area |
| POST | `/api/base/area/create` | 新增库区 | base:area:create |
| POST | `/api/base/area/update` | 编辑库区 | base:area:update |
| POST | `/api/base/area/status` | 启用/停用 | base:area:update |

### V2 新增：生产工单状态操作（ProductionOrderController 扩展）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| POST | `/api/production/order/start` | 工单开始（未开始→执行中） | production:order:start |
| POST | `/api/production/order/finish` | 工单完工（执行中→已完工） | production:order:start |
| POST | `/api/production/order/void` | 工单作废 | production:order:start |

### V2 新增：报工/审工（ReportWorkController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/production/reportWork/list` | 分页查询报工单 | production:reportWork |
| POST | `/api/production/reportWork/create` | 新增报工 | production:reportWork:create |
| POST | `/api/production/reportWork/approve` | 审工通过 | production:reportWork:audit |
| POST | `/api/production/reportWork/unapprove` | 审工反审核 | production:reportWork:audit |
| POST | `/api/production/reportWork/reject` | 审工驳回 | production:reportWork:audit |
| POST | `/api/production/reportWork/batchApprove` | 一键审核 | production:reportWork:audit |
| GET | `/api/production/reportWork/processProgress` | 按工单查工序进度 | production:reportWork |

### V2 新增：生产排产计划（SchedulePlanController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/production/schedule/list` | 分页查询排产计划 | production:schedule |
| POST | `/api/production/schedule/create` | 新增排产计划 | production:schedule:create |
| POST | `/api/production/schedule/status` | 更新排产状态 | production:schedule:create |

### V2 新增：主生产计划（MpsPlanController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/production/mps/list` | 分页查询主生产计划 | production:mps |
| POST | `/api/production/mps/create` | 新增主生产计划 | production:mps:create |
| POST | `/api/production/mps/generate` | 生成工单 | production:mps:create |

### V2 新增：报价单（QuotationController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/sales/quotation/list` | 分页查询报价单 | sales:quotation |
| GET | `/api/sales/quotation/items` | 查询报价单明细 | sales:quotation |
| POST | `/api/sales/quotation/create` | 新增报价单 | sales:quotation:create |
| POST | `/api/sales/quotation/submit` | 提交审批 | sales:quotation:create |
| POST | `/api/sales/quotation/approve` | 审批通过 | sales:quotation:approve |
| POST | `/api/sales/quotation/unapprove` | 反审核 | sales:quotation:approve |
| POST | `/api/sales/quotation/convert` | 转销售订单 | sales:quotation:create |

### V2 新增：采购申请单（PurchaseRequisitionController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/purchase/requisition/list` | 分页查询采购申请单 | purchase:requisition |
| GET | `/api/purchase/requisition/items` | 查询申请单明细 | purchase:requisition |
| POST | `/api/purchase/requisition/create` | 新增采购申请单 | purchase:requisition:create |
| POST | `/api/purchase/requisition/submit` | 提交审批 | purchase:requisition:create |
| POST | `/api/purchase/requisition/approve` | 审批通过 | purchase:requisition:approve |
| POST | `/api/purchase/requisition/unapprove` | 反审核 | purchase:requisition:approve |

### V2 新增：合同（ContractController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/sales/contract/list` | 分页查询合同 | sales:contract |
| GET | `/api/sales/contract/items` | 查询合同明细 | sales:contract |
| POST | `/api/sales/contract/create` | 新增合同 | sales:contract:create |
| POST | `/api/sales/contract/submit` | 提交审批 | sales:contract:create |
| POST | `/api/sales/contract/approve` | 审批通过 | sales:contract:approve |
| POST | `/api/sales/contract/unapprove` | 反审核 | sales:contract:approve |
| POST | `/api/sales/contract/convert` | 转销售订单 | sales:contract:create |

### V2 新增：采购询价单（PurchaseInquiryController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/purchase/inquiry/list` | 分页查询询价单 | purchase:inquiry |
| GET | `/api/purchase/inquiry/items` | 查询询价单明细 | purchase:inquiry |
| POST | `/api/purchase/inquiry/create` | 新增询价单 | purchase:inquiry:create |
| POST | `/api/purchase/inquiry/submit` | 提交审批 | purchase:inquiry:create |
| POST | `/api/purchase/inquiry/approve` | 审批通过 | purchase:inquiry:approve |
| POST | `/api/purchase/inquiry/unapprove` | 反审核 | purchase:inquiry:approve |

### V2 新增：销售发票（SalesInvoiceController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/sales/invoice/list` | 分页查询销售发票 | sales:invoice |
| POST | `/api/sales/invoice/create` | 新增销售发票 | sales:invoice:create |
| POST | `/api/sales/invoice/submit` | 提交审批 | sales:invoice:create |
| POST | `/api/sales/invoice/approve` | 审批通过 | sales:invoice:approve |
| POST | `/api/sales/invoice/unapprove` | 反审核 | sales:invoice:approve |

### V2 新增：采购发票（PurchaseInvoiceController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/purchase/invoice/list` | 分页查询采购发票 | purchase:invoice |
| POST | `/api/purchase/invoice/create` | 新增采购发票 | purchase:invoice:create |
| POST | `/api/purchase/invoice/submit` | 提交审批 | purchase:invoice:create |
| POST | `/api/purchase/invoice/approve` | 审批通过 | purchase:invoice:approve |
| POST | `/api/purchase/invoice/unapprove` | 反审核 | purchase:invoice:approve |

### V2 新增：销售/采购核销（VerificationController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/sales/verification/list` | 分页查询销售核销 | sales:verification |
| POST | `/api/sales/verification/create` | 新增销售核销 | sales:verification:create |
| POST | `/api/sales/verification/submit` | 提交审批 | sales:verification:create |
| POST | `/api/sales/verification/approve` | 审批通过 | sales:verification:approve |
| POST | `/api/sales/verification/unapprove` | 反审核 | sales:verification:approve |
| GET | `/api/purchase/verification/list` | 分页查询采购核销 | purchase:verification |
| POST | `/api/purchase/verification/create` | 新增采购核销 | purchase:verification:create |
| POST | `/api/purchase/verification/submit` | 提交审批 | purchase:verification:create |
| POST | `/api/purchase/verification/approve` | 审批通过 | purchase:verification:approve |
| POST | `/api/purchase/verification/unapprove` | 反审核 | purchase:verification:approve |

### V2 新增：采购退货（PurchaseReturnController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/purchase/return/list` | 分页查询采购退货 | purchase:return |
| GET | `/api/purchase/return/items` | 查询退货明细 | purchase:return |
| POST | `/api/purchase/return/create` | 新增采购退货（库存出库） | purchase:return:create |

### V2 新增：售后/备货/设计/装箱（销售独有）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET/POST | `/api/sales/afterSale/*` | 售后管理（list/create/status） |
| GET/POST | `/api/sales/stocking/*` | 销售备货单（list/create/submit/approve） |
| GET/POST | `/api/sales/designCenter/*` | 设计中心（list/create/status） |
| GET/POST | `/api/sales/packing/*` | 装箱单（list/create） |

> 销售退货：V1 已存在（SalesOutboundController /return/create），本次补充 /return/list、/return/items。

### V2 新增：库存精细（InventoryController 扩展）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/inventory/warning` | 库存预警（低于最低/安全库存） | inventory |
| GET | `/api/inventory/batchTrack` | 批号跟踪 | inventory |
| GET | `/api/inventory/serialTrack` | 序列号跟踪 | inventory |
| GET | `/api/inventory/reservation/list` | 锁库记录列表 | inventory |
| POST | `/api/inventory/reservation/lock` | 锁库（增加 reservedQty） | inventory:reserve |
| POST | `/api/inventory/reservation/release` | 解锁（减少 reservedQty） | inventory:reserve |

### V2 新增：其他出入库（OtherStockController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/inventory/otherStock/list` | 分页查询其他出入库单 | inventory:otherStock |
| GET | `/api/inventory/otherStock/items` | 查询明细 | inventory:otherStock |
| POST | `/api/inventory/otherStock/create` | 新增其他出入库（direction in/out） | inventory:otherStock:create |

### V2 新增：质检申请单（QcApplyController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/quality/apply/list` | 分页查询质检申请单 | quality |
| POST | `/api/quality/apply/create` | 新增质检申请单 | quality:create |
| POST | `/api/quality/apply/generate` | 申请单生成检验单（转待检） | quality:create |

### 第一档断环补全：库存盘点（StocktakeController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/inventory/stocktake/list` | 分页查询盘点单 | inventory:stocktake |
| GET | `/api/inventory/stocktake/items` | 查询盘点明细 | inventory:stocktake |
| POST | `/api/inventory/stocktake/create` | 创建盘点方案（自动带库存账面） | inventory:stocktake:create |
| POST | `/api/inventory/stocktake/count` | 录入实际数（算差异） | inventory:stocktake:create |
| POST | `/api/inventory/stocktake/adjust` | 差异生成调整（盘盈入库/盘亏出库） | inventory:stocktake:create |

### 第一档断环补全：库存调拨（TransferController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/inventory/transfer/list` | 分页查询调拨单 | inventory:transfer |
| GET | `/api/inventory/transfer/items` | 查询调拨明细 | inventory:transfer |
| POST | `/api/inventory/transfer/create` | 新增调拨（源出+目标入） | inventory:transfer:create |

### 第一档断环补全：组装/拆卸（AssemblyController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/inventory/assembly/list` | 分页查询组装/拆卸单 | inventory:assembly |
| GET | `/api/inventory/assembly/items` | 查询子件明细 | inventory:assembly |
| POST | `/api/inventory/assembly/create` | 新增组装/拆卸（按BOM展开子件） | inventory:assembly:create |

### 第一档断环补全：期初余额（BeginBalanceController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/inventory/beginBalance/list` | 分页查询期初余额 | inventory:beginBalance |
| POST | `/api/inventory/beginBalance/save` | 保存期初余额（upsert） | inventory:beginBalance:create |
| POST | `/api/inventory/beginBalance/delete` | 删除期初余额 | inventory:beginBalance:create |

### 第一档断环补全：物料替代料（MaterialSubstituteController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/substitute/list` | 分页查询替代料关系 | base:substitute |
| POST | `/api/base/substitute/create` | 新增替代料 | base:substitute:create |
| POST | `/api/base/substitute/update` | 编辑替代料 | base:substitute:create |
| POST | `/api/base/substitute/status` | 启用/停用 | base:substitute:create |
| POST | `/api/base/substitute/delete` | 删除替代料 | base:substitute:create |

### 第一档断环补全：不良品项（DefectItemController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/defect/list` | 分页查询不良品项 | base:defect |
| POST | `/api/base/defect/create` | 新增不良品项 | base:defect:create |
| POST | `/api/base/defect/update` | 编辑不良品项 | base:defect:create |
| POST | `/api/base/defect/status` | 启用/停用 | base:defect:create |
| POST | `/api/base/defect/delete` | 删除不良品项 | base:defect:create |

### 第一档断环补全：客户价格资料（CustomerPriceController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/base/customerPrice/list` | 分页查询客户价格 | base:customerPrice |
| POST | `/api/base/customerPrice/create` | 新增客户价格 | base:customerPrice:create |
| POST | `/api/base/customerPrice/update` | 编辑客户价格 | base:customerPrice:create |
| POST | `/api/base/customerPrice/status` | 启用/停用 | base:customerPrice:create |
| POST | `/api/base/customerPrice/delete` | 删除客户价格 | base:customerPrice:create |

### 第一档断环补全：齐套分析（CompleteAnalysisController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/production/completeAnalysis/analysis` | 工单 BOM 需求 vs 可用库存，返回缺料清单 | production:completeAnalysis |

### 第一档断环补全：检验方法/方案（QualityBaseController）

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| GET | `/api/quality/base/method/list` | 分页查询检验方法 | quality:base |
| POST | `/api/quality/base/method/create` | 新增检验方法 | quality:base:create |
| POST | `/api/quality/base/method/update` | 编辑检验方法 | quality:base:create |
| POST | `/api/quality/base/method/status` | 启用/停用 | quality:base:create |
| GET | `/api/quality/base/scheme/list` | 分页查询检验方案 | quality:base |
| POST | `/api/quality/base/scheme/create` | 新增检验方案 | quality:base:create |
| POST | `/api/quality/base/scheme/update` | 编辑检验方案 | quality:base:create |
| POST | `/api/quality/base/scheme/status` | 启用/停用 | quality:base:create |

### 第一档断环补全：已有 Controller 扩展

| 方法 | 路径 | 说明 | 权限码 |
|---|---|---|---|
| POST | `/api/purchase/inquiry/toPurchaseOrder` | 比价生单（询价转采购订单） | purchase:inquiry:create |
| POST | `/api/inventory/warning/generatePurchase` | 物料预警生成采购申请单 | inventory:reserve |
| POST | `/api/inventory/warning/generateWorkOrder` | 产品预警生成生产工单 | inventory:reserve |
| GET | `/api/production/order/list?orderType=semi` | 半成品加工单（复用生产工单） | production:semi |
