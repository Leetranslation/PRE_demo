<template>
  <el-card shadow="never">
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="生产工单">
        <el-select v-model="orderId" placeholder="请选择工单" filterable clearable style="width: 240px" @change="loadProgress">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadProgress">查询进度</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="progress" v-loading="loading" border stripe row-key="processId">
      <el-table-column prop="seqNo" label="顺序" width="70" />
      <el-table-column prop="processName" label="工序名称" min-width="140" />
      <el-table-column label="关键工序" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isKey === 1" type="warning" size="small">关键</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="检验工序" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isQc === 1" type="danger" size="small">检验</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="planQty" label="计划数量" width="110" align="right" />
      <el-table-column prop="reportedQty" label="已报良品" width="110" align="right" />
      <el-table-column prop="reportCount" label="报工次数" width="100" align="right" />
      <el-table-column label="完成进度" min-width="180">
        <template #default="{ row }">
          <el-progress :percentage="progressPercent(row)" :status="progressStatus(row)" />
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
defineOptions({ name: 'ProgressInquiry' })
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { productionOrderList, reportWorkProcessProgress } from '@/api/production'

const loading = ref(false)
const orders = ref<any[]>([])
const orderId = ref<number>()
const progress = ref<any[]>([])

function progressPercent(row: any) {
  if (!row.planQty || Number(row.planQty) === 0) return 0
  return Math.min(100, Math.round((Number(row.reportedQty) / Number(row.planQty)) * 100))
}
function progressStatus(row: any) {
  const p = progressPercent(row)
  return p >= 100 ? 'success' : undefined
}

async function loadOrders() {
  const res = await productionOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}

async function loadProgress() {
  if (!orderId.value) {
    ElMessage.warning('请先选择工单')
    return
  }
  loading.value = true
  try {
    progress.value = await reportWorkProcessProgress(orderId.value)
    if (progress.value.length === 0) {
      ElMessage.info('该工单暂无工艺路线或工序')
    }
  } finally {
    loading.value = false
  }
}

loadOrders()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
</style>
