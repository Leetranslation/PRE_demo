<template>
  <DocList :columns="columns" :fetch-list="fetchList" :fetch-items="fetchItems" filename="销售出库单" @detail="showDetail" />
  <el-dialog v-model="detailVisible" title="出库明细" width="720px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="productName" label="产品" width="140" />
      <el-table-column prop="quantity" label="数量" width="120" align="right" />
      <el-table-column prop="batchNo" label="批次号" width="140" />
      <el-table-column prop="serialNo" label="序列号" width="140" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'SalesOutbound' })
import { ref } from 'vue'
import DocList from '@/components/DocList.vue'
import { salesOutboundList } from '@/api/sales'
import request from '@/utils/request'

const columns = [
  { prop: 'outboundNo', label: '出库单号', width: 170 },
  { prop: 'orderId', label: '销售订单ID', width: 110 },
  { prop: 'warehouseId', label: '仓库ID', width: 90 },
  { prop: 'outboundDate', label: '出库日期', width: 120 },
  { prop: 'createTime', label: '创建时间', width: 170 }
]

const detailVisible = ref(false)
const detailItems = ref<any[]>([])

function fetchList(params: any) {
  return salesOutboundList(params)
}

async function fetchItems(id: number) {
  const res: any = await request.get('/sales/outbound/items', { params: { outboundId: id } })
  return res || []
}

function showDetail(items: any[]) {
  detailItems.value = items
  detailVisible.value = true
}
</script>
