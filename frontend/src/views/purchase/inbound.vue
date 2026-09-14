<template>
  <DocList
    :columns="columns"
    :fetch-list="fetchList"
    :fetch-items="fetchItems"
    filename="采购入库单"
    print-title="采购入库单"
    :print-head-fn="printHeadFn"
    :print-cols="printCols"
    :print-sign-labels="['制单人', '仓管员', '供应商']"
    @detail="showDetail"
  />
  <el-dialog v-model="detailVisible" title="入库明细" width="720px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialName" label="物料" width="140" />
      <el-table-column prop="quantity" label="数量" width="120" align="right" />
      <el-table-column prop="batchNo" label="批次号" width="140" />
      <el-table-column prop="serialNo" label="序列号" width="140" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseInbound' })
import { ref } from 'vue'
import DocList from '@/components/DocList.vue'
import { purchaseInboundList } from '@/api/purchase'
import request from '@/utils/request'

const columns = [
  { prop: 'inboundNo', label: '入库单号', width: 170 },
  { prop: 'orderId', label: '采购订单ID', width: 110 },
  { prop: 'warehouseId', label: '仓库ID', width: 90 },
  { prop: 'inboundDate', label: '入库日期', width: 120 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const detailVisible = ref(false)
const detailItems = ref<any[]>([])

function fetchList(params: any) {
  return purchaseInboundList(params)
}

async function fetchItems(id: number) {
  const res: any = await request.get('/purchase/inbound/items', { params: { inboundId: id } })
  return res || []
}

function showDetail(items: any[]) {
  detailItems.value = items
  detailVisible.value = true
}

const printCols = [
  { label: '序号', prop: '_idx', width: '40px', align: 'center', formatter: (_r: any, i: number) => i + 1 },
  { label: '物料', prop: 'materialName' },
  { label: '数量', prop: 'quantity', align: 'right' },
  { label: '批次号', prop: 'batchNo' },
  { label: '序列号', prop: 'serialNo' },
  { label: '单位', prop: 'unit', align: 'center' }
]
function printHeadFn(row: any) {
  return [
    [{ label: '入库单号', value: row.inboundNo }, { label: '入库日期', value: row.inboundDate }],
    [{ label: '采购订单ID', value: row.orderId }, { label: '仓库ID', value: row.warehouseId }]
  ]
}
</script>
