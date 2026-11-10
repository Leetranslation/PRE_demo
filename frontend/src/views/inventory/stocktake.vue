<template>
  <el-card v-if="!formVisible" shadow="never">
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
      <el-button type="primary" @click="openForm">新建盘点</el-button>
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
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openCount(row, true)">录数</el-button>
          <el-button link type="warning" :disabled="row.status === 'adjusted'" @click="doAdjust(row)">生成调整</el-button>
          <el-button link type="primary" @click="openCount(row, false)">明细</el-button>
          <el-button link type="primary" @click="handlePrint(row)">打印</el-button>
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

  <!-- 新建盘点：整页单据表单（明细由系统按账面库存带入） -->
  <DocForm
    v-else
    title="新建盘点单"
    :model-value="form"
    :items="[]"
    :fields="docFields"
    :columns="[]"
    hide-submit
    @update:items="() => {}"
    @cancel="closeForm"
    @save="handleCreate"
  />

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

  <PrintDialog
    v-model="printVisible"
    title="库存盘点单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['盘点人', '复核人', '仓管员']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Stocktake' })
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { stocktakeList, stocktakeItems, stocktakeCreate, stocktakeCount, stocktakeAdjust } from '@/api/inventory'
import { warehouseList } from '@/api/base'
import PrintDialog from '@/components/PrintDialog.vue'
import DocForm from '@/components/DocForm.vue'
import type { DocField } from '@/components/docForm'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const warehouses = ref<any[]>([])
const formVisible = ref(false)
const form = reactive<any>({ warehouseId: undefined, remark: '' })
const countVisible = ref(false)
const countEditable = ref(false)
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

function openForm() {
  form.warehouseId = undefined
  form.remark = ''
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

/** 基本信息字段 */
const docFields = computed<DocField[]>(() => [
  {
    key: 'warehouseId',
    label: '仓库',
    type: 'select',
    required: true,
    options: warehouses.value.map((w: any) => ({ label: w.name, value: w.id }))
  },
  { key: 'remark', label: '备注', span: 2 }
])

async function handleCreate() {
  if (!form.warehouseId) { ElMessage.warning('请选择仓库'); return }
  await stocktakeCreate(form.warehouseId, form.remark)
  ElMessage.success('盘点单已创建，已带入账面库存')
  formVisible.value = false
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

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printCols = [
  { label: '序号', prop: '_idx', width: '40px', align: 'center', formatter: (_r: any, i: number) => i + 1 },
  { label: '物料/产品', prop: 'itemName' },
  { label: '类型', prop: 'itemType', align: 'center' },
  { label: '账面数量', prop: 'bookQty', align: 'right' },
  { label: '实际数量', prop: 'actualQty', align: 'right' },
  { label: '差异', prop: 'diffQty', align: 'right' }
]

async function handlePrint(row: any) {
  printRows.value = await stocktakeItems(row.id)
  printHead.value = [
    [{ label: '盘点单号', value: row.stocktakeNo }, { label: '盘点日期', value: row.stocktakeDate }],
    [{ label: '仓库', value: warehouseName(row.warehouseId) }, { label: '状态', value: statusText(row.status) }]
  ]
  printRemark.value = row.remark
  printVisible.value = true
}

loadWarehouses()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
