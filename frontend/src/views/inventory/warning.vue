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
    </el-table>
  </el-card>
</template>

<script setup lang="ts">
defineOptions({ name: 'InventoryWarning' })
import { ref } from 'vue'
import { inventoryWarning } from '@/api/inventory'

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

load()
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
