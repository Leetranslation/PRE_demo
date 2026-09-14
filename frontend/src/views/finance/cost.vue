<template>
  <el-card shadow="never">
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="生产工单">
        <el-select v-model="orderId" filterable placeholder="选择工单" style="width: 340px" clearable>
          <el-option v-for="o in orders" :key="o.id" :label="`${o.orderNo} ${o.productName || ''}`" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleCalculate">核算成本</el-button>
      </el-form-item>
    </el-form>

    <el-descriptions v-if="result" title="成本核算结果" :column="3" border>
      <el-descriptions-item label="工单号">{{ result.orderNo }}</el-descriptions-item>
      <el-descriptions-item label="完工数量">{{ result.completedQty }}</el-descriptions-item>
      <el-descriptions-item label="单位成本">
        <span style="color: #e6a23c; font-weight: bold">{{ result.unitCost }}</span>
      </el-descriptions-item>
      <el-descriptions-item label="材料成本">{{ result.materialCost }}</el-descriptions-item>
      <el-descriptions-item label="人工成本">{{ result.laborCost }}</el-descriptions-item>
      <el-descriptions-item label="制造费用">{{ result.overheadCost }}</el-descriptions-item>
      <el-descriptions-item label="总成本" :span="3">
        <span style="color: #f56c6c; font-weight: bold">{{ result.totalCost }}</span>
      </el-descriptions-item>
    </el-descriptions>
    <el-empty v-else description="选择工单后点击「核算成本」" />
  </el-card>
</template>

<script setup lang="ts">
defineOptions({ name: 'Cost' })
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { costCalculate } from '@/api/finance'
import { productionOrderList } from '@/api/production'

const orderId = ref<number>()
const orders = ref<any[]>([])
const result = ref<any>(null)

async function loadOrders() {
  const res = await productionOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}

async function handleCalculate() {
  if (!orderId.value) { ElMessage.warning('请选择工单'); return }
  result.value = await costCalculate(orderId.value)
}

loadOrders()
</script>

<style scoped>
.filter-bar { margin-bottom: 12px; }
</style>
