<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="物料/产品ID">
        <el-input v-model="query.itemId" placeholder="按ID查询" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.itemType" placeholder="全部" clearable style="width: 120px">
          <el-option label="物料" value="material" />
          <el-option label="产品" value="product" />
        </el-select>
      </el-form-item>
      <el-form-item label="库存类型">
        <el-select v-model="query.stockType" placeholder="全部" clearable style="width: 140px">
          <el-option label="可用" value="available" />
          <el-option label="待检" value="pending_qc" />
          <el-option label="不合格" value="unqualified" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button @click="doExport">导出</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="itemType" label="类型" width="80">
        <template #default="{ row }">{{ row.itemType === 'material' ? '物料' : '产品' }}</template>
      </el-table-column>
      <el-table-column prop="itemName" label="物料/产品" width="160" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="batchNo" label="批次号" width="130" />
      <el-table-column prop="serialNo" label="序列号" min-width="160" />
      <el-table-column label="库存类型" width="100">
        <template #default="{ row }">
          <el-tag :type="stockTagType(row.stockType)">{{ stockText(row.stockType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="实际数量" width="120" align="right" />
      <el-table-column prop="reservedQty" label="预留数量" width="120" align="right" />
      <el-table-column label="可用数量" width="120" align="right">
        <template #default="{ row }">
          {{ (Number(row.quantity) - Number(row.reservedQty)).toFixed(6) }}
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
defineOptions({ name: 'InventoryStock' })
import { reactive, ref } from 'vue'
import { inventoryList } from '@/api/inventory'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({
  page: 1, pageSize: 10, itemId: undefined as number | undefined,
  itemType: undefined as string | undefined, stockType: undefined as string | undefined
})

function stockText(v: string) {
  return { available: '可用', pending_qc: '待检', unqualified: '不合格' }[v] || v
}
function stockTagType(v: string) {
  return { available: 'success', pending_qc: 'warning', unqualified: 'danger' }[v] || 'info'
}

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'itemType', label: '类型', formatter: (row: any) => (row.itemType === 'material' ? '物料' : '产品') },
    { prop: 'itemId', label: '物料/产品ID' },
    { prop: 'warehouseId', label: '仓库ID' },
    { prop: 'batchNo', label: '批次号' },
    { prop: 'serialNo', label: '序列号' },
    { prop: 'stockType', label: '库存类型', formatter: (row: any) => stockText(row.stockType) },
    { prop: 'quantity', label: '实际数量' },
    { prop: 'reservedQty', label: '预留数量' }
  ],
  '库存查询'
)
function doExport() { handleExport(records.value) }

async function load() {
  loading.value = true
  try {
    const params: any = { ...query, itemId: query.itemId ? Number(query.itemId) : undefined }
    const res = await inventoryList(params)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() {
  query.itemId = undefined
  query.itemType = undefined
  query.stockType = undefined
  query.page = 1
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
