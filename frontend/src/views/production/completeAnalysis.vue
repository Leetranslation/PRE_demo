<template>
  <el-card shadow="never">
    <div class="select-bar">
      <span class="label">生产工单：</span>
      <el-select v-model="workOrderId" placeholder="请选择工单" filterable style="width: 320px" @change="doAnalysis">
        <el-option v-for="o in orders" :key="o.id" :label="`${o.orderNo} / ${o.productId}`" :value="o.id" />
      </el-select>
      <el-button type="primary" :disabled="!workOrderId" @click="doAnalysis">齐套分析</el-button>
    </div>

    <el-alert
      v-if="analyzed && shortageCount === 0"
      title="该工单物料齐套，无缺料"
      type="success"
      :closable="false"
      style="margin: 12px 0"
    />
    <el-alert
      v-if="analyzed && shortageCount > 0"
      :title="`共 ${shortageCount} 项物料缺料，请及时采购/补料`"
      type="warning"
      :closable="false"
      style="margin: 12px 0"
    />

    <el-table :data="records" v-loading="loading" border stripe row-key="materialId">
      <el-table-column prop="materialCode" label="物料编码" width="140" />
      <el-table-column prop="materialName" label="物料名称" min-width="160" />
      <el-table-column prop="materialType" label="类型" width="90">
        <template #default="{ row }">{{ row.materialType === 'product' ? '产品' : '物料' }}</template>
      </el-table-column>
      <el-table-column prop="unitUsage" label="单位用量" width="100" align="right" />
      <el-table-column prop="lossRate" label="损耗率%" width="90" align="right" />
      <el-table-column prop="requiredQty" label="需求量" width="110" align="right" />
      <el-table-column prop="availableQty" label="可用库存" width="110" align="right" />
      <el-table-column label="缺料数量" width="120" align="right">
        <template #default="{ row }">
          <el-tag v-if="Number(row.shortageQty) > 0" type="danger">{{ row.shortageQty }}</el-tag>
          <span v-else>0</span>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
defineOptions({ name: 'CompleteAnalysis' })
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { completeAnalysis, productionOrderList } from '@/api/production'

const loading = ref(false)
const workOrderId = ref<number>()
const orders = ref<any[]>([])
const records = ref<any[]>([])
const analyzed = ref(false)

const shortageCount = computed(() => records.value.filter((r) => Number(r.shortageQty) > 0).length)

async function loadOrders() {
  const res = await productionOrderList({ page: 1, pageSize: 100, approveStatus: 'approved' })
  orders.value = res.records
}

async function doAnalysis() {
  if (!workOrderId.value) return
  loading.value = true
  try {
    records.value = await completeAnalysis(workOrderId.value)
    analyzed.value = true
    if (records.value.length === 0) ElMessage.info('该工单产品无 BOM，无法分析')
  } finally {
    loading.value = false
  }
}

loadOrders()
</script>

<style scoped>
.select-bar { display: flex; align-items: center; gap: 8px; }
.label { font-size: 14px; color: #333; }
</style>
