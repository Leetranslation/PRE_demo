<template>
  <el-card shadow="never">
    <div class="arch-title">系统架构一览</div>
    <div class="arch-tip">点击模块跳转对应功能页面；鼠标悬浮节点可查看主要接口路径</div>

    <!-- 核心业务主流程 -->
    <div class="main-flow">
      <div class="flow-label">核心主流程</div>
      <div class="flow-nodes">
        <template v-for="(n, i) in mainFlow" :key="n.title">
          <el-tooltip :content="n.api || '点击进入'" placement="top" :show-after="200">
            <div class="flow-node" @click="go(n)">{{ n.title }}</div>
          </el-tooltip>
          <span v-if="i < mainFlow.length - 1" class="flow-arrow">→</span>
        </template>
      </div>
    </div>

    <!-- 分域模块一览 -->
    <div v-for="row in rows" :key="row.domain" class="arch-row">
      <div class="arch-domain" :style="{ background: row.color }">
        <el-icon style="margin-right: 4px"><component :is="row.icon" /></el-icon>{{ row.domain }}
      </div>
      <div class="arch-nodes">
        <template v-for="(n, i) in row.nodes" :key="n.title">
          <el-tooltip :content="n.api || '点击进入'" placement="top" :show-after="200">
            <div class="arch-node" @click="go(n)">
              <span class="node-name">{{ n.title }}</span>
            </div>
          </el-tooltip>
          <span v-if="row.flow && i < row.nodes.length - 1" class="node-arrow">→</span>
        </template>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
defineOptions({ name: 'Architecture' })
import { useRouter } from 'vue-router'
import {
  Files, ShoppingCart, Box, Setting, CircleCheck, Coin,
  Connection, Money, Tools, User
} from '@element-plus/icons-vue'

const router = useRouter()

interface ArchNode { title: string; path?: string; api?: string }
interface ArchRow { domain: string; color: string; icon: any; flow?: boolean; nodes: ArchNode[] }

function go(n: ArchNode) {
  if (n.path) router.push(n.path)
}

/** 核心业务主流程：销售 → 生产 → 采购 → 执行 → 出库 → 结算 */
const mainFlow: ArchNode[] = [
  { title: '销售订单', path: '/sales/order', api: 'POST /api/sales/order/create' },
  { title: 'MRP 计划', path: '/mrp/index', api: '/api/mrp/*' },
  { title: '采购订单', path: '/purchase/order', api: 'POST /api/purchase/order/create' },
  { title: '生产工单', path: '/production/order', api: 'POST /api/production/order/create' },
  { title: '领料', path: '/production/issue', api: 'POST /api/production/issue/create' },
  { title: '报工', path: '/production/reportWork', api: 'POST /api/production/reportWork/create' },
  { title: '生产入库', path: '/production/inbound', api: 'POST /api/production/inbound/create' },
  { title: '销售出库', path: '/sales/outbound', api: 'POST /api/sales/outbound/create' },
  { title: '销售发票', path: '/sales/invoice', api: 'POST /api/sales/invoice/approve' },
  { title: '收款核销', path: '/finance/receipt', api: 'POST /api/finance/receipt/writeoff' }
]

const rows: ArchRow[] = [
  {
    domain: '基础数据', color: '#909399', icon: Files,
    nodes: [
      { title: '客户档案', path: '/base/customer', api: '/api/base/customer/list' },
      { title: '供应商档案', path: '/base/supplier', api: '/api/base/supplier/list' },
      { title: '物料档案', path: '/base/material', api: '/api/base/material/list' },
      { title: '产品档案', path: '/base/product', api: '/api/base/product/list' },
      { title: 'BOM 管理', path: '/base/bom', api: '/api/base/bom/list' },
      { title: '替代料', path: '/base/substitute', api: '/api/base/substitute/list' },
      { title: '客户价格', path: '/base/customerPrice', api: '/api/base/customerPrice/list' },
      { title: '工序管理', path: '/produceData/process', api: '/api/base/process/list' },
      { title: '工艺路线', path: '/produceData/processRoute', api: '/api/base/processRoute/list' },
      { title: '车间管理', path: '/produceData/workshop', api: '/api/base/workshop/list' },
      { title: '机台设备', path: '/produceData/machine', api: '/api/base/machine/list' },
      { title: '工作日历', path: '/produceData/workCalendar', api: '/api/base/workCalendar/list' },
      { title: '单位管理', path: '/produceData/unit', api: '/api/base/unit/list' },
      { title: '序列号规则', path: '/produceData/serialRule', api: '/api/base/serialRule/list' },
      { title: '仓库/库位/库区', path: '/base/warehouse', api: '/api/base/warehouse/list' }
    ]
  },
  {
    domain: '销售', color: '#f56c6c', icon: ShoppingCart, flow: true,
    nodes: [
      { title: '报价单', path: '/sales/quotation', api: 'POST /api/sales/quotation/convert 转单' },
      { title: '合同', path: '/sales/contract', api: 'POST /api/sales/contract/convert 转单' },
      { title: '销售订单', path: '/sales/order', api: '/api/sales/order/list|items|create' },
      { title: '销售发票', path: '/sales/invoice', api: 'POST /api/sales/invoice/approve → 生成应收' },
      { title: '销售出库', path: '/sales/outbound', api: 'POST /api/sales/outbound/create' },
      { title: '销售退货', path: '/sales/return', api: 'POST /api/sales/return/create' },
      { title: '备货单', path: '/sales/stocking', api: '/api/sales/stocking/list' },
      { title: '装箱单', path: '/sales/packing', api: '/api/sales/packing/items' },
      { title: '售后管理', path: '/sales/afterSale', api: '/api/sales/afterSale/list' },
      { title: '设计中心', path: '/sales/designCenter', api: '/api/sales/designCenter/list' }
    ]
  },
  {
    domain: '采购', color: '#e6a23c', icon: Box, flow: true,
    nodes: [
      { title: '采购申请', path: '/purchase/requisition', api: 'POST /api/purchase/requisition/create' },
      { title: '采购询价', path: '/purchase/inquiry', api: 'POST /api/purchase/inquiry/toPurchaseOrder 比价生单' },
      { title: '采购订单', path: '/purchase/order', api: '/api/purchase/order/list|items|create' },
      { title: '采购发票', path: '/purchase/invoice', api: 'POST /api/purchase/invoice/approve → 生成应付' },
      { title: '采购入库', path: '/purchase/inbound', api: 'POST /api/purchase/inbound/create' },
      { title: '采购退货', path: '/purchase/return', api: 'POST /api/purchase/return/create' }
    ]
  },
  {
    domain: '生产/MES', color: '#409eff', icon: Setting, flow: true,
    nodes: [
      { title: 'MRP 计划', path: '/mrp/index', api: '/api/mrp/*' },
      { title: '主生产计划', path: '/production/mps', api: 'POST /api/production/mps/generate' },
      { title: '排产计划', path: '/production/schedule', api: '/api/production/schedule/list' },
      { title: '生产工单', path: '/production/order', api: '/api/production/order/start|finish' },
      { title: '齐套分析', path: '/production/completeAnalysis', api: 'GET /api/production/completeAnalysis/analysis' },
      { title: '领料单', path: '/production/issue', api: 'POST /api/production/issue/create' },
      { title: '退料单', path: '/production/return', api: 'POST /api/production/return/create' },
      { title: '报工管理', path: '/production/reportWork', api: 'POST /api/production/reportWork/approve' },
      { title: '审工管理', path: '/production/auditWork', api: '/api/production/reportWork/batchApprove' },
      { title: '工序进度', path: '/production/progressInquiry', api: 'GET /api/production/reportWork/processProgress' },
      { title: '简易报工', path: '/production/simpleWorkReport', api: '/api/production/reportWork/create' },
      { title: '生产入库', path: '/production/inbound', api: 'POST /api/production/inbound/create' },
      { title: '半成品加工', path: '/production/semi', api: '/api/production/order/list?orderType=semi' }
    ]
  },
  {
    domain: '质量', color: '#67c23a', icon: CircleCheck, flow: true,
    nodes: [
      { title: '质检申请', path: '/quality/apply', api: 'POST /api/quality/apply/generate → 生成检验单' },
      { title: '质量检验', path: '/quality/index', api: 'POST /api/quality/judge 判定' },
      { title: '检验方法/方案', path: '/quality/base', api: '/api/quality/base/method|scheme' }
    ]
  },
  {
    domain: '库存', color: '#b37feb', icon: Coin,
    nodes: [
      { title: '库存查询', path: '/inventory/stock', api: '/api/inventory/list' },
      { title: '库存流水', path: '/inventory/flow', api: '/api/inventory/flow' },
      { title: '库存预警', path: '/inventory/warning', api: 'GET /api/inventory/warning' },
      { title: '批号/序列号跟踪', path: '/inventory/track', api: 'GET /api/inventory/batchTrack|serialTrack' },
      { title: '锁库管理', path: '/inventory/reservation', api: 'POST /api/inventory/reservation/lock' },
      { title: '其他出入库', path: '/inventory/otherStock', api: 'POST /api/inventory/otherStock/create' },
      { title: '库存盘点', path: '/inventory/stocktake', api: 'POST /api/inventory/stocktake/adjust' },
      { title: '库存调拨', path: '/inventory/transfer', api: 'POST /api/inventory/transfer/create' },
      { title: '组装/拆卸', path: '/inventory/assembly', api: 'POST /api/inventory/assembly/create' },
      { title: '期初余额', path: '/inventory/beginBalance', api: 'POST /api/inventory/beginBalance/save' }
    ]
  },
  {
    domain: '外协', color: '#36cfc9', icon: Connection, flow: true,
    nodes: [
      { title: '外协单', path: '/outsource/order', api: '/api/outsource/order/start|finish|void' },
      { title: '外协领料', path: '/outsource/issue', api: 'POST /api/production/issue/create (issueType=outsource)' },
      { title: '外协入库', path: '/outsource/inbound', api: 'POST /api/production/inbound/create (inboundType=outsource)' }
    ]
  },
  {
    domain: '财务', color: '#fa8c16', icon: Money, flow: true,
    nodes: [
      { title: '应收台账', path: '/finance/receivable', api: 'GET /api/finance/receivable/list' },
      { title: '收款单', path: '/finance/receipt', api: 'POST /api/finance/receipt/writeoff 核销' },
      { title: '应付台账', path: '/finance/payable', api: 'GET /api/finance/payable/list' },
      { title: '付款单', path: '/finance/payment', api: 'POST /api/finance/payment/writeoff 核销' },
      { title: '成本核算', path: '/finance/cost', api: 'POST /api/cost/calculate' }
    ]
  },
  {
    domain: '设备', color: '#722ed1', icon: Tools, flow: true,
    nodes: [
      { title: '设备台账', path: '/equipment/ledger', api: '/api/base/machine/list' },
      { title: '设备点检', path: '/equipment/check', api: 'POST /api/equipment/check/create' },
      { title: '设备保养', path: '/equipment/maintenance', api: 'POST /api/equipment/maintenance/complete' },
      { title: '设备维修', path: '/equipment/repair', api: 'POST /api/equipment/repair/status 状态流转' }
    ]
  },
  {
    domain: '系统', color: '#c0c4cc', icon: User,
    nodes: [
      { title: '用户管理', path: '/system/user', api: '/api/system/user/*' },
      { title: '角色管理', path: '/system/role', api: '/api/system/role/*' },
      { title: '部门管理', path: '/system/department', api: '/api/system/department/*' },
      { title: '经营驾驶舱', path: '/cockpit', api: 'GET /api/dashboard/cockpit' }
    ]
  }
]
</script>

<style scoped>
.arch-title { font-size: 18px; font-weight: 600; }
.arch-tip { color: #909399; font-size: 12px; margin: 4px 0 16px; }

.main-flow {
  display: flex; align-items: center; flex-wrap: wrap; gap: 6px;
  background: #f6f8fb; border: 1px dashed #c6d4e8; border-radius: 8px;
  padding: 10px 14px; margin-bottom: 16px;
}
.flow-label { font-weight: 600; color: #409eff; margin-right: 8px; font-size: 13px; }
.flow-nodes { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.flow-node {
  background: #409eff; color: #fff; padding: 4px 12px; border-radius: 14px;
  font-size: 12px; cursor: pointer; transition: opacity .15s;
}
.flow-node:hover { opacity: .8; }
.flow-arrow { color: #a8abb2; }

.arch-row { display: flex; align-items: stretch; margin-bottom: 10px; }
.arch-domain {
  width: 86px; flex-shrink: 0; color: #fff; border-radius: 6px;
  display: flex; align-items: center; justify-content: center;
  font-weight: 600; font-size: 13px; padding: 6px 4px; margin-right: 12px;
}
.arch-nodes { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; flex: 1; }
.arch-node {
  border: 1px solid #e4e7ed; border-radius: 8px; background: #fff;
  padding: 10px 6px; min-width: 96px; text-align: center; cursor: pointer;
  transition: all .15s;
}
.arch-node:hover {
  border-color: var(--el-color-primary); color: var(--el-color-primary);
  box-shadow: 0 2px 8px rgba(64, 158, 255, .15);
}
.node-name { font-size: 12px; }
.node-arrow { color: #c0c4cc; }
</style>
