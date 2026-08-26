<template>
  <el-card shadow="never">
    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="createTime" label="退货时间" width="170" />
      <el-table-column prop="itemId" label="产品ID" width="110" />
      <el-table-column prop="warehouseId" label="仓库ID" width="90" />
      <el-table-column prop="quantity" label="退货数量" width="120" align="right" />
      <el-table-column prop="bizNo" label="来源销售单号" min-width="160" />
      <el-table-column label="方向" width="90">
        <template #default>
          <el-tag type="success">入库</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      class="pagination"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="load"
      @current-change="load"
    />
  </el-card>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { inventoryTx } from '@/api/inventory'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const res = await inventoryTx({ ...query, bizType: 'sales_return' })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

load()
</script>

<style scoped>
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
