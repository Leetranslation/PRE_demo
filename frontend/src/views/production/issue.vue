<template>
  <DocList
    :columns="columns"
    :fetch-list="fetchList"
    :fetch-items="fetchItems"
    filename="领料单"
    print-title="领料单"
    :print-head-fn="printHeadFn"
    :print-cols="printCols"
    :print-sign-labels="['领料人', '仓管员', '审核人']"
    @detail="showDetail"
  />
  <el-dialog v-model="detailVisible" title="领料明细" width="720px">
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
defineOptions({ name: 'MaterialIssue' })
import { ref } from 'vue'
import DocList from '@/components/DocList.vue'
import { materialIssueList, materialIssueItems } from '@/api/production'

const columns = [
  { prop: 'issueNo', label: '领料单号', width: 170 },
  { prop: 'orderId', label: '生产工单ID', width: 110 },
  { prop: 'warehouseId', label: '仓库ID', width: 90 },
  { prop: 'issueDate', label: '领料日期', width: 120 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const detailVisible = ref(false)
const detailItems = ref<any[]>([])

function fetchList(params: any) {
  return materialIssueList(params)
}

async function fetchItems(id: number) {
  return await materialIssueItems(id)
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
    [{ label: '领料单号', value: row.issueNo }, { label: '领料日期', value: row.issueDate }],
    [{ label: '生产工单ID', value: row.orderId }, { label: '仓库ID', value: row.warehouseId }]
  ]
}
</script>
