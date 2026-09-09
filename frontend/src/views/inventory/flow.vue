<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="物料/产品ID">
        <el-input v-model="query.itemId" placeholder="按ID查询" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="业务类型">
        <el-select v-model="query.bizType" placeholder="全部" clearable style="width: 200px">
          <el-option label="采购入库" value="purchase_inbound" />
          <el-option label="生产入库" value="production_inbound" />
          <el-option label="销售出库" value="sales_outbound" />
          <el-option label="生产领料" value="material_issue" />
          <el-option label="生产退料" value="return" />
          <el-option label="调拨" value="transfer" />
          <el-option label="盘点" value="stocktake" />
          <el-option label="调整" value="adjustment" />
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
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ row.createTime }}</template>
      </el-table-column>
      <el-table-column label="方向" width="80">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'in' ? 'success' : 'warning'">
            {{ row.direction === 'in' ? '入库' : '出库' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="itemType" label="类型" width="80">
        <template #default="{ row }">{{ row.itemType === 'material' ? '物料' : '产品' }}</template>
      </el-table-column>
      <el-table-column prop="itemId" label="物料/产品ID" width="120" />
      <el-table-column prop="warehouseId" label="仓库ID" width="100" />
      <el-table-column prop="batchNo" label="批次号" width="130" />
      <el-table-column prop="serialNo" label="序列号" width="130" />
      <el-table-column label="库存类型" width="100">
        <template #default="{ row }">
          {{ STOCK_TYPE_TEXT[row.stockType] || row.stockType }}
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="变动数量" width="110" align="right" />
      <el-table-column prop="balanceQty" label="结存数量" width="110" align="right" />
      <el-table-column prop="bizType" label="业务类型" width="130" />
      <el-table-column prop="bizNo" label="单据编号" min-width="150" />
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
defineOptions({ name: 'InventoryFlow' })
import { reactive, ref } from 'vue'
import { inventoryTx } from '@/api/inventory'
import { useExport } from '@/composables/useExport'

const STOCK_TYPE_TEXT: Record<string, string> = {
  available: '可用',
  pending_qc: '待检',
  unqualified: '不合格'
}

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({
  page: 1, pageSize: 10, itemId: undefined as number | undefined, bizType: undefined as string | undefined
})

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'createTime', label: '时间' },
    { prop: 'direction', label: '方向', formatter: (row: any) => (row.direction === 'in' ? '入库' : '出库') },
    { prop: 'itemType', label: '类型', formatter: (row: any) => (row.itemType === 'material' ? '物料' : '产品') },
    { prop: 'itemId', label: '物料/产品ID' },
    { prop: 'warehouseId', label: '仓库ID' },
    { prop: 'batchNo', label: '批次号' },
    { prop: 'quantity', label: '变动数量' },
    { prop: 'balanceQty', label: '结存数量' },
    { prop: 'bizType', label: '业务类型' },
    { prop: 'bizNo', label: '单据编号' }
  ],
  '库存流水'
)
function doExport() { handleExport(records.value) }

async function load() {
  loading.value = true
  try {
    const params: any = { ...query, itemId: query.itemId ? Number(query.itemId) : undefined }
    const res = await inventoryTx(params)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.itemId = undefined; query.bizType = undefined; query.page = 1; load() }

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
