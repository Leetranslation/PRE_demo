import {
  HomeFilled,
  Files,
  Tools,
  ShoppingCart,
  Box,
  Setting,
  DataAnalysis,
  Coin,
  CircleCheck,
  User,
  Connection,
  Odometer,
  Money,
  MagicStick
} from '@element-plus/icons-vue'
// ⚠️ `Component` 是 Vue 的类型，`@element-plus/icons-vue` 不导出它
// （原来写成 `from '@element-plus/icons-vue'`，TS 报 TS2305）
import type { Component } from 'vue'

export interface MenuLeaf {
  path: string
  title: string
}

export interface MenuColumn {
  /** 列标题，如"销售单据" */
  groupTitle: string
  items: MenuLeaf[]
}

export interface TopMenu {
  /** 唯一 key，用于 ref / 状态 */
  key: string
  /** 一级菜单显示标题 */
  title: string
  /** 一级菜单图标（Element Plus icon） */
  icon: Component
  /** 若一级菜单本身就是一个页面（无子菜单），点击直接跳转 */
  path?: string
  /** 当前路由匹配该一级的高亮前缀（可选；不填则按 groups.items 匹配） */
  pathPrefix?: string[]
  /** 子菜单分组（多列浮层的内容） */
  groups?: MenuColumn[]
}

/**
 * 一级菜单 + 多列浮层 配置（替代传统 el-menu 树形折叠）
 * 分组原则：按业务子模块聚合（销售单据 / 销售出库 / 销售财务 / 设计与售后 等）
 */
export const topMenus: TopMenu[] = [
  {
    key: 'dashboard',
    title: '首页',
    icon: HomeFilled,
    path: '/dashboard'
  },
  {
    key: 'cockpit',
    title: '驾驶舱',
    icon: Odometer,
    path: '/cockpit'
  },
  {
    key: 'architecture',
    title: '架构总览',
    icon: Files,
    path: '/architecture'
  },
  {
    key: 'ai-brain',
    title: 'AI 工业大脑',
    icon: MagicStick,
    path: '/ai-brain'
  },
  {
    key: 'base',
    title: '基础资料',
    icon: Files,
    pathPrefix: ['/base', '/produceData'],
    groups: [
      {
        groupTitle: '基础档案',
        items: [
          { path: '/base/customer', title: '客户档案' },
          { path: '/base/supplier', title: '供应商档案' },
          { path: '/base/material', title: '物料档案' },
          { path: '/base/product', title: '产品档案' },
          { path: '/base/warehouse', title: '仓库' },
          { path: '/base/location', title: '库位管理' },
          { path: '/base/area', title: '库区管理' },
          { path: '/base/bom', title: 'BOM 管理' },
          { path: '/base/substitute', title: '替代料关系' },
          { path: '/base/customerPrice', title: '客户价格资料' }
        ]
      },
      {
        groupTitle: '生产数据',
        items: [
          { path: '/produceData/process', title: '工序管理' },
          { path: '/produceData/processRoute', title: '工艺路线' },
          { path: '/produceData/workshop', title: '车间管理' },
          { path: '/produceData/machine', title: '机台设备' },
          { path: '/produceData/workCalendar', title: '工作日历' },
          { path: '/produceData/unit', title: '单位管理' },
          { path: '/produceData/unitConversion', title: '多单位转换率' },
          { path: '/produceData/serialRule', title: '序列号规则' },
          { path: '/produceData/defect', title: '不良品项' }
        ]
      }
    ]
  },
  {
    key: 'sales',
    title: '销售管理',
    icon: ShoppingCart,
    groups: [
      {
        groupTitle: '销售单据',
        items: [
          { path: '/sales/order', title: '销售订单' },
          { path: '/sales/quotation', title: '报价单' },
          { path: '/sales/contract', title: '合同管理' },
          { path: '/sales/stocking', title: '销售备货单' }
        ]
      },
      {
        groupTitle: '销售出库',
        items: [
          { path: '/sales/outbound', title: '销售出库单' },
          { path: '/sales/return', title: '销售退货单' }
        ]
      },
      {
        groupTitle: '销售财务',
        items: [
          { path: '/sales/invoice', title: '销售发票' },
          { path: '/sales/verification', title: '销售核销' },
          { path: '/sales/packing', title: '装箱单' }
        ]
      },
      {
        groupTitle: '设计与售后',
        items: [
          { path: '/sales/designCenter', title: '设计中心' },
          { path: '/sales/afterSale', title: '售后管理' }
        ]
      }
    ]
  },
  {
    key: 'purchase',
    title: '采购管理',
    icon: Box,
    groups: [
      {
        groupTitle: '采购单据',
        items: [
          { path: '/purchase/order', title: '采购订单' },
          { path: '/purchase/requisition', title: '采购申请单' },
          { path: '/purchase/inquiry', title: '采购询价单' }
        ]
      },
      {
        groupTitle: '采购入库',
        items: [
          { path: '/purchase/inbound', title: '采购入库单' },
          { path: '/purchase/return', title: '采购退货单' }
        ]
      },
      {
        groupTitle: '采购财务',
        items: [
          { path: '/purchase/invoice', title: '采购发票' },
          { path: '/purchase/verification', title: '采购核销' }
        ]
      }
    ]
  },
  {
    key: 'production',
    title: '生产管理',
    icon: Setting,
    groups: [
      {
        groupTitle: '生产计划',
        items: [
          { path: '/production/order', title: '生产工单' },
          { path: '/production/semi', title: '半成品加工单' },
          { path: '/production/schedule', title: '生产排产计划' },
          { path: '/production/mps', title: '主生产计划' },
          { path: '/production/completeAnalysis', title: '齐套分析' }
        ]
      },
      {
        groupTitle: '生产执行',
        items: [
          { path: '/production/issue', title: '领料单' },
          { path: '/production/return', title: '退料单' },
          { path: '/production/inbound', title: '生产入库单' }
        ]
      },
      {
        groupTitle: 'MES 报工',
        items: [
          { path: '/production/reportWork', title: '报工管理' },
          { path: '/production/auditWork', title: '审工管理' },
          { path: '/production/progressInquiry', title: '工序进度查询' },
          { path: '/production/simpleWorkReport', title: '简易报工' }
        ]
      }
    ]
  },
  {
    key: 'mrp',
    title: 'MRP 计划',
    icon: DataAnalysis,
    path: '/mrp/index'
  },
  {
    key: 'inventory',
    title: '库存管理',
    icon: Coin,
    groups: [
      {
        groupTitle: '库存查询',
        items: [
          { path: '/inventory/stock', title: '库存查询' },
          { path: '/inventory/flow', title: '库存流水' }
        ]
      },
      {
        groupTitle: '库存精细',
        items: [
          { path: '/inventory/warning', title: '库存预警' },
          { path: '/inventory/track', title: '批号/序列号跟踪' },
          { path: '/inventory/reservation', title: '锁库管理' },
          { path: '/inventory/otherStock', title: '其他出入库' }
        ]
      },
      {
        groupTitle: '库存作业',
        items: [
          { path: '/inventory/stocktake', title: '库存盘点' },
          { path: '/inventory/transfer', title: '库存调拨' },
          { path: '/inventory/assembly', title: '组装/拆卸' },
          { path: '/inventory/beginBalance', title: '期初余额' }
        ]
      }
    ]
  },
  {
    key: 'quality',
    title: '质量管理',
    icon: CircleCheck,
    groups: [
      {
        groupTitle: '质量检验',
        items: [
          { path: '/quality/index', title: '质量检验' },
          { path: '/quality/apply', title: '质检申请单' },
          { path: '/quality/base', title: '检验方法/方案' }
        ]
      }
    ]
  },
  {
    key: 'outsource',
    title: '外协管理',
    icon: Connection,
    groups: [
      {
        groupTitle: '外协单据',
        items: [
          { path: '/outsource/order', title: '外协单' },
          { path: '/outsource/issue', title: '外协领料' },
          { path: '/outsource/inbound', title: '外协入库' }
        ]
      }
    ]
  },
  {
    key: 'finance',
    title: '财务管理',
    icon: Money,
    groups: [
      {
        groupTitle: '应收应付',
        items: [
          { path: '/finance/receivable', title: '应收台账' },
          { path: '/finance/payable', title: '应付台账' }
        ]
      },
      {
        groupTitle: '收付款',
        items: [
          { path: '/finance/receipt', title: '收款单' },
          { path: '/finance/payment', title: '付款单' }
        ]
      },
      {
        groupTitle: '成本',
        items: [
          { path: '/finance/cost', title: '成本核算' }
        ]
      }
    ]
  },
  {
    key: 'equipment',
    title: '设备管理',
    icon: Tools,
    groups: [
      {
        groupTitle: '设备台账',
        items: [
          { path: '/equipment/ledger', title: '设备台账' }
        ]
      },
      {
        groupTitle: '设备维护',
        items: [
          { path: '/equipment/check', title: '设备点检' },
          { path: '/equipment/maintenance', title: '设备保养' },
          { path: '/equipment/repair', title: '设备维修' }
        ]
      }
    ]
  },
  {
    key: 'system',
    title: '系统管理',
    icon: User,
    groups: [
      {
        groupTitle: '用户与权限',
        items: [
          { path: '/system/user', title: '用户管理' },
          { path: '/system/role', title: '角色管理' },
          { path: '/system/department', title: '部门管理' }
        ]
      }
    ]
  }
]