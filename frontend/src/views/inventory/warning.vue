<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" @click="load">刷新</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="itemId">
      <el-table-column prop="code" label="编码" width="140" />
      <el-table-column prop="name" label="名称" min-width="140" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">{{ row.itemType === 'material' ? '物料' : '产品' }}</template>
      </el-table-column>
      <el-table-column prop="availableQty" label="可用库存" width="110" align="right" />
      <el-table-column prop="minStock" label="最低库存" width="110" align="right" />
      <el-table-column prop="safetyStock" label="安全库存" width="110" align="right" />
      <el-table-column label="预警级别" width="110">
        <template #default="{ row }">
          <el-tag :type="row.warningLevel === 'critical' ? 'danger' : 'warning'" size="small">
            {{ row.warningLevel === 'critical' ? '严重缺货' : '库存偏低' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.itemType === 'material'" link type="primary" @click="genPurchase(row)">生成采购申请</el-button>
          <el-button v-else link type="warning" @click="genWorkOrder(row)">生成生产工单</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
defineOptions({ name: 'InventoryWarning' })
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { inventoryWarning, warningGeneratePurchase, warningGenerateWorkOrder } from '@/api/inventory'

const loading = ref(false)
const records = ref<any[]>([])

async function load() {
  loading.value = true
  try {
    records.value = await inventoryWarning()
  } finally {
    loading.value = false
  }
}

async function genPurchase(row: any) {
  await ElMessageBox.confirm(`确认为「${row.name}」生成补货采购申请单吗？`, '提示', { type: 'warning' })
  const res: any = await warningGeneratePurchase(row.itemId)
  ElMessage.success(`已生成采购申请单：${res.requisitionNo}`)
  load()
}

async function genWorkOrder(row: any) {
  await ElMessageBox.confirm(`确认为「${row.name}」生成补货生产工单吗？`, '提示', { type: 'warning' })
  const res: any = await warningGenerateWorkOrder(row.itemId)
  ElMessage.success(`已生成生产工单：${res.orderNo}`)
  load()
}

load()
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
