<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="仓库">
        <el-select v-model="query.warehouseId" placeholder="全部" clearable filterable style="width: 160px">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="已创建" value="created" />
          <el-option label="已盘点" value="counted" />
          <el-option label="已调整" value="adjusted" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新建盘点</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="stocktakeNo" label="盘点单号" width="180" />
      <el-table-column label="仓库" width="140">
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
      <el-table-column prop="stocktakeDate" label="盘点日期" width="110" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCount(row, true)">录数</el-button>
          <el-button link type="warning" :disabled="row.status === 'adjusted'" @click="doAdjust(row)">生成调整</el-button>
          <el-button link type="primary" @click="openCount(row, false)">明细</el-button>
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

  <el-dialog v-model="createVisible" title="新建盘点" width="480px">
    <el-form label-width="80px">
      <el-form-item label="仓库" required>
        <el-select v-model="createWarehouseId" placeholder="请选择" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="createRemark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="handleCreate">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="countVisible" :title="countEditable ? '录入实际数' : '盘点明细'" width="760px">
    <el-table :data="items" border size="small">
      <el-table-column prop="itemName" label="物料/产品" width="160" />
      <el-table-column prop="itemType" label="类型" width="90" />
      <el-table-column prop="bookQty" label="账面数量" width="120" align="right" />
      <el-table-column label="实际数量" width="150">
        <template #default="{ row }">
          <el-input v-if="countEditable" v-model="row.actualQty" type="number" size="small" />
          <span v-else>{{ row.actualQty }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="diffQty" label="差异" width="110" align="right" />
    </el-table>
    <template #footer>
      <el-button @click="countVisible = false">关闭</el-button>
      <el-button v-if="countEditable" type="primary" @click="handleCount">保存实际数</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'Stocktake' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stocktakeList, stocktakeItems, stocktakeCreate, stocktakeCount, stocktakeAdjust } from '@/api/inventory'
import { warehouseList } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const warehouses = ref<any[]>([])
const createVisible = ref(false)
const countVisible = ref(false)
const countEditable = ref(false)
const createWarehouseId = ref<number>()
const createRemark = ref('')
const items = ref<any[]>([])
const currentId = ref<number>()
const query = reactive({ page: 1, pageSize: 10, warehouseId: undefined as number | undefined, status: undefined as string | undefined })

function warehouseName(id: number) { return warehouses.value.find((w) => w.id === id)?.name || id }
function statusText(s: string) { return { created: '已创建', counted: '已盘点', adjusted: '已调整' }[s] || s }
function statusType(s: string) { return { created: 'info', counted: 'warning', adjusted: 'success' }[s] || 'info' }

async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await stocktakeList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.warehouseId = undefined; query.status = undefined; query.page = 1; load() }

function openCreate() { createWarehouseId = undefined; createRemark = ''; createVisible.value = true }

async function handleCreate() {
  if (!createWarehouseId.value) { ElMessage.warning('请选择仓库'); return }
  await stocktakeCreate(createWarehouseId.value, createRemark.value)
  ElMessage.success('盘点单已创建，已带入账面库存')
  createVisible.value = false
  load()
}

async function openCount(row: any, editable: boolean) {
  currentId.value = row.id
  countEditable.value = editable
  items.value = await stocktakeItems(row.id)
  countVisible.value = true
}

async function handleCount() {
  if (!currentId.value) return
  await stocktakeCount(items.value.map((i) => ({ id: i.id, actualQty: Number(i.actualQty) })))
  ElMessage.success('实际数已保存')
  countVisible.value = false
  items.value = await stocktakeItems(currentId.value)
  load()
}

async function doAdjust(row: any) {
  await ElMessageBox.confirm('确认按差异生成调整（盘盈入库/盘亏出库）？', '提示', { type: 'warning' })
  await stocktakeAdjust(row.id)
  ElMessage.success('调整完成')
  load()
}

loadWarehouses()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
