<template>
  <DocList :columns="columns" :fetch-list="fetchList" :fetch-items="fetchItems" filename="退料单" @detail="showDetail" />
  <el-dialog v-model="detailVisible" title="退料明细" width="620px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialId" label="物料ID" width="110" />
      <el-table-column prop="quantity" label="数量" width="120" align="right" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import DocList from '@/components/DocList.vue'
import { materialReturnList, materialReturnItems } from '@/api/production'

const columns = [
  { prop: 'returnNo', label: '退料单号', width: 170 },
  { prop: 'orderId', label: '生产工单ID', width: 110 },
  { prop: 'warehouseId', label: '仓库ID', width: 90 },
  { prop: 'returnDate', label: '退料日期', width: 120 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const detailVisible = ref(false)
const detailItems = ref<any[]>([])

function fetchList(params: any) {
  return materialReturnList(params)
}

async function fetchItems(id: number) {
  return await materialReturnItems(id)
}

function showDetail(items: any[]) {
  detailItems.value = items
  detailVisible.value = true
}
</script>
