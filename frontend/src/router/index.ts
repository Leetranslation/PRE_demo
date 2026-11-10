import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useTabsStore } from '@/stores/tabs'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      {
        path: 'cockpit',
        name: 'Cockpit',
        component: () => import('@/views/dashboard/cockpit.vue'),
        meta: { title: '经营驾驶舱' }
      },
      {
        path: 'architecture',
        name: 'Architecture',
        component: () => import('@/views/architecture/index.vue'),
        meta: { title: '系统架构一览' }
      },
      // ===== AI 工业大脑 =====
      {
        path: 'ai-brain',
        name: 'AiBrain',
        component: () => import('@/views/ai/index.vue'),
        meta: { title: 'AI 工业大脑' }
      },
      {
        path: 'ai-brain/purchase',
        name: 'AiBrainPurchase',
        component: () => import('@/views/ai/purchase.vue'),
        meta: { title: '智能下单' }
      },
      {
        path: 'ai-brain/sales',
        name: 'AiBrainSales',
        component: () => import('@/views/ai/sales.vue'),
        meta: { title: '智能销售订单' }
      },
      // ===== 基础资料 =====
      {
        path: 'base/customer',
        name: 'Customer',
        component: () => import('@/views/base/customer.vue'),
        meta: { title: '客户档案' }
      },
      {
        path: 'base/supplier',
        name: 'Supplier',
        component: () => import('@/views/base/supplier.vue'),
        meta: { title: '供应商档案' }
      },
      {
        path: 'base/material',
        name: 'Material',
        component: () => import('@/views/base/material.vue'),
        meta: { title: '物料档案' }
      },
      {
        path: 'base/product',
        name: 'Product',
        component: () => import('@/views/base/product.vue'),
        meta: { title: '产品档案' }
      },
      {
        path: 'base/warehouse',
        name: 'Warehouse',
        component: () => import('@/views/base/warehouse.vue'),
        meta: { title: '仓库' }
      },
      {
        path: 'base/bom',
        name: 'Bom',
        component: () => import('@/views/base/bom.vue'),
        meta: { title: 'BOM 管理' }
      },
      {
        path: 'base/substitute',
        name: 'MaterialSubstitute',
        component: () => import('@/views/base/substitute.vue'),
        meta: { title: '替代料关系' }
      },
      {
        path: 'base/customerPrice',
        name: 'CustomerPrice',
        component: () => import('@/views/base/customerPrice.vue'),
        meta: { title: '客户价格资料' }
      },
      {
        path: 'base/location',
        name: 'Location',
        component: () => import('@/views/base/location.vue'),
        meta: { title: '库位管理' }
      },
      {
        path: 'produceData/process',
        name: 'Process',
        component: () => import('@/views/base/process.vue'),
        meta: { title: '工序管理' }
      },
      {
        path: 'produceData/processRoute',
        name: 'ProcessRoute',
        component: () => import('@/views/base/processRoute.vue'),
        meta: { title: '工艺路线' }
      },
      {
        path: 'produceData/workshop',
        name: 'Workshop',
        component: () => import('@/views/base/workshop.vue'),
        meta: { title: '车间管理' }
      },
      {
        path: 'produceData/machine',
        name: 'Machine',
        component: () => import('@/views/base/machine.vue'),
        meta: { title: '机台设备' }
      },
      {
        path: 'produceData/workCalendar',
        name: 'WorkCalendar',
        component: () => import('@/views/base/workCalendar.vue'),
        meta: { title: '工作日历' }
      },
      {
        path: 'produceData/unit',
        name: 'Unit',
        component: () => import('@/views/base/unit.vue'),
        meta: { title: '单位管理' }
      },
      {
        path: 'produceData/unitConversion',
        name: 'UnitConversion',
        component: () => import('@/views/base/unitConversion.vue'),
        meta: { title: '多单位转换率' }
      },
      {
        path: 'produceData/serialRule',
        name: 'SerialRule',
        component: () => import('@/views/base/serialRule.vue'),
        meta: { title: '序列号规则' }
      },
      {
        path: 'produceData/defect',
        name: 'DefectItem',
        component: () => import('@/views/base/defect.vue'),
        meta: { title: '不良品项' }
      },
      {
        path: 'base/area',
        name: 'Area',
        component: () => import('@/views/base/area.vue'),
        meta: { title: '库区管理' }
      },
      // ===== 库存 =====
      {
        path: 'inventory/stock',
        name: 'InventoryStock',
        component: () => import('@/views/inventory/stock.vue'),
        meta: { title: '库存查询' }
      },
      {
        path: 'inventory/flow',
        name: 'InventoryFlow',
        component: () => import('@/views/inventory/flow.vue'),
        meta: { title: '库存流水' }
      },
      {
        path: 'inventory/warning',
        name: 'InventoryWarning',
        component: () => import('@/views/inventory/warning.vue'),
        meta: { title: '库存预警' }
      },
      {
        path: 'inventory/track',
        name: 'InventoryTrack',
        component: () => import('@/views/inventory/track.vue'),
        meta: { title: '批号/序列号跟踪' }
      },
      {
        path: 'inventory/reservation',
        name: 'InventoryReservation',
        component: () => import('@/views/inventory/reservation.vue'),
        meta: { title: '锁库管理' }
      },
      {
        path: 'inventory/otherStock',
        name: 'OtherStock',
        component: () => import('@/views/inventory/otherStock.vue'),
        meta: { title: '其他出入库' }
      },
      {
        path: 'inventory/stocktake',
        name: 'Stocktake',
        component: () => import('@/views/inventory/stocktake.vue'),
        meta: { title: '库存盘点' }
      },
      {
        path: 'inventory/transfer',
        name: 'Transfer',
        component: () => import('@/views/inventory/transfer.vue'),
        meta: { title: '库存调拨' }
      },
      {
        path: 'inventory/assembly',
        name: 'Assembly',
        component: () => import('@/views/inventory/assembly.vue'),
        meta: { title: '组装/拆卸' }
      },
      {
        path: 'inventory/beginBalance',
        name: 'BeginBalance',
        component: () => import('@/views/inventory/beginBalance.vue'),
        meta: { title: '期初余额' }
      },
      // ===== MRP =====
      {
        path: 'mrp/index',
        name: 'Mrp',
        component: () => import('@/views/mrp/index.vue'),
        meta: { title: 'MRP 计划' }
      },
      // ===== 销售 =====
      {
        path: 'sales/order',
        name: 'SalesOrder',
        component: () => import('@/views/sales/order.vue'),
        meta: { title: '销售订单' }
      },
      {
        path: 'sales/quotation',
        name: 'Quotation',
        component: () => import('@/views/sales/quotation.vue'),
        meta: { title: '报价单' }
      },
      {
        path: 'sales/contract',
        name: 'Contract',
        component: () => import('@/views/sales/contract.vue'),
        meta: { title: '合同管理' }
      },
      {
        path: 'sales/invoice',
        name: 'SalesInvoice',
        component: () => import('@/views/sales/invoice.vue'),
        meta: { title: '销售发票' }
      },
      {
        path: 'sales/verification',
        name: 'SalesVerification',
        component: () => import('@/views/sales/verification.vue'),
        meta: { title: '销售核销' }
      },
      {
        path: 'sales/afterSale',
        name: 'AfterSale',
        component: () => import('@/views/sales/afterSale.vue'),
        meta: { title: '售后管理' }
      },
      {
        path: 'sales/stocking',
        name: 'SalesStocking',
        component: () => import('@/views/sales/stocking.vue'),
        meta: { title: '销售备货单' }
      },
      {
        path: 'sales/designCenter',
        name: 'DesignCenter',
        component: () => import('@/views/sales/designCenter.vue'),
        meta: { title: '设计中心' }
      },
      {
        path: 'sales/packing',
        name: 'Packing',
        component: () => import('@/views/sales/packing.vue'),
        meta: { title: '装箱单' }
      },
      {
        path: 'sales/outbound',
        name: 'SalesOutbound',
        component: () => import('@/views/sales/outbound.vue'),
        meta: { title: '销售出库单' }
      },
      {
        path: 'sales/return',
        name: 'SalesReturn',
        component: () => import('@/views/sales/return.vue'),
        meta: { title: '销售退货单' }
      },
      // ===== 采购 =====
      {
        path: 'purchase/order',
        name: 'PurchaseOrder',
        component: () => import('@/views/purchase/order.vue'),
        meta: { title: '采购订单' }
      },
      {
        path: 'purchase/requisition',
        name: 'PurchaseRequisition',
        component: () => import('@/views/purchase/requisition.vue'),
        meta: { title: '采购申请单' }
      },
      {
        path: 'purchase/inquiry',
        name: 'PurchaseInquiry',
        component: () => import('@/views/purchase/inquiry.vue'),
        meta: { title: '采购询价单' }
      },
      {
        path: 'purchase/invoice',
        name: 'PurchaseInvoice',
        component: () => import('@/views/purchase/invoice.vue'),
        meta: { title: '采购发票' }
      },
      {
        path: 'purchase/verification',
        name: 'PurchaseVerification',
        component: () => import('@/views/purchase/verification.vue'),
        meta: { title: '采购核销' }
      },
      {
        path: 'purchase/return',
        name: 'PurchaseReturn',
        component: () => import('@/views/purchase/return.vue'),
        meta: { title: '采购退货单' }
      },
      {
        path: 'purchase/inbound',
        name: 'PurchaseInbound',
        component: () => import('@/views/purchase/inbound.vue'),
        meta: { title: '采购入库单' }
      },
      // ===== 生产 =====
      {
        path: 'production/order',
        name: 'ProductionOrder',
        component: () => import('@/views/production/order.vue'),
        meta: { title: '生产工单' }
      },
      {
        path: 'production/issue',
        name: 'MaterialIssue',
        component: () => import('@/views/production/issue.vue'),
        meta: { title: '领料单' }
      },
      {
        path: 'production/return',
        name: 'MaterialReturn',
        component: () => import('@/views/production/return.vue'),
        meta: { title: '退料单' }
      },
      {
        path: 'production/inbound',
        name: 'ProductionInbound',
        component: () => import('@/views/production/inbound.vue'),
        meta: { title: '生产入库单' }
      },
      {
        path: 'production/reportWork',
        name: 'ReportWork',
        component: () => import('@/views/production/reportWork.vue'),
        meta: { title: '报工管理' }
      },
      {
        path: 'production/auditWork',
        name: 'AuditWork',
        component: () => import('@/views/production/auditWork.vue'),
        meta: { title: '审工管理' }
      },
      {
        path: 'production/progressInquiry',
        name: 'ProgressInquiry',
        component: () => import('@/views/production/progressInquiry.vue'),
        meta: { title: '工序进度查询' }
      },
      {
        path: 'production/simpleWorkReport',
        name: 'SimpleWorkReport',
        component: () => import('@/views/production/simpleWorkReport.vue'),
        meta: { title: '简易报工' }
      },
      {
        path: 'production/schedule',
        name: 'Schedule',
        component: () => import('@/views/production/schedule.vue'),
        meta: { title: '生产排产计划' }
      },
      {
        path: 'production/mps',
        name: 'Mps',
        component: () => import('@/views/production/mps.vue'),
        meta: { title: '主生产计划' }
      },
      {
        path: 'production/completeAnalysis',
        name: 'CompleteAnalysis',
        component: () => import('@/views/production/completeAnalysis.vue'),
        meta: { title: '齐套分析' }
      },
      {
        path: 'production/semi',
        name: 'SemiFinished',
        component: () => import('@/views/production/semiFinished.vue'),
        meta: { title: '半成品加工单' }
      },
      // ===== 质量 =====
      {
        path: 'quality/index',
        name: 'Quality',
        component: () => import('@/views/quality/index.vue'),
        meta: { title: '质量检验' }
      },
      {
        path: 'quality/apply',
        name: 'QcApply',
        component: () => import('@/views/quality/apply.vue'),
        meta: { title: '质检申请单' }
      },
      {
        path: 'quality/base',
        name: 'QualityBase',
        component: () => import('@/views/quality/base.vue'),
        meta: { title: '检验方法/方案' }
      },
      // ===== 外协 =====
      {
        path: 'outsource/order',
        name: 'OutsourceOrder',
        component: () => import('@/views/outsource/order.vue'),
        meta: { title: '外协单' }
      },
      {
        path: 'outsource/issue',
        name: 'OutsourceIssue',
        component: () => import('@/views/outsource/issue.vue'),
        meta: { title: '外协领料' }
      },
      {
        path: 'outsource/inbound',
        name: 'OutsourceInbound',
        component: () => import('@/views/outsource/inbound.vue'),
        meta: { title: '外协入库' }
      },
      // ===== 财务 =====
      {
        path: 'finance/receivable',
        name: 'Receivable',
        component: () => import('@/views/finance/receivable.vue'),
        meta: { title: '应收台账' }
      },
      {
        path: 'finance/payable',
        name: 'Payable',
        component: () => import('@/views/finance/payable.vue'),
        meta: { title: '应付台账' }
      },
      {
        path: 'finance/receipt',
        name: 'Receipt',
        component: () => import('@/views/finance/receipt.vue'),
        meta: { title: '收款单' }
      },
      {
        path: 'finance/payment',
        name: 'Payment',
        component: () => import('@/views/finance/payment.vue'),
        meta: { title: '付款单' }
      },
      {
        path: 'finance/cost',
        name: 'Cost',
        component: () => import('@/views/finance/cost.vue'),
        meta: { title: '成本核算' }
      },
      // ===== 设备管理 =====
      {
        path: 'equipment/ledger',
        name: 'EquipmentLedger',
        component: () => import('@/views/equipment/ledger.vue'),
        meta: { title: '设备台账' }
      },
      {
        path: 'equipment/check',
        name: 'EquipmentCheck',
        component: () => import('@/views/equipment/check.vue'),
        meta: { title: '设备点检' }
      },
      {
        path: 'equipment/maintenance',
        name: 'EquipmentMaintenance',
        component: () => import('@/views/equipment/maintenance.vue'),
        meta: { title: '设备保养' }
      },
      {
        path: 'equipment/repair',
        name: 'EquipmentRepair',
        component: () => import('@/views/equipment/repair.vue'),
        meta: { title: '设备维修' }
      },
      // ===== 系统管理 =====
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/department',
        name: 'SystemDepartment',
        component: () => import('@/views/system/department.vue'),
        meta: { title: '部门管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫 + 标签上限拦截
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  // 标签上限拦截：达到上限且目标为新标签时，阻止跳转
  const tabsStore = useTabsStore()
  tabsStore.initAffix()
  if (!tabsStore.hasTab(to.path) && tabsStore.isFull()) {
    next(false)
    return
  }
  next()
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? ''} - Token 制造`
  // 非登录页：自动登记/复用标签（首页固定）
  if (to.path !== '/login') {
    const tabsStore = useTabsStore()
    tabsStore.initAffix()
    tabsStore.addTab(to)
  }
})

export default router
