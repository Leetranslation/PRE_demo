<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="方向">
        <el-select v-model="query.direction" placeholder="全部" clearable style="width: 120px">
          <el-option label="入库" value="in" />
          <el-option label="出库" value="out" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.type" placeholder="全部" clearable style="width: 120px">
          <el-option label="期初" value="期初" />
          <el-option label="盘盈" value="盘盈" />
          <el-option label="盘亏" value="盘亏" />
          <el-option label="领用" value="领用" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openForm()">新增其他出入库</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="stockNo" label="单号" width="160" />
      <el-table-column label="方向" width="80">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'in' ? 'success' : 'warning'" size="small">
            {{ row.direction === 'in' ? '入库' : '出库' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="stockDate" label="日期" width="110" />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewItems(row)">明细</el-button>
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

  <!-- 新增其他出入库：整页单据表单（无审批流） -->
  <DocForm
    v-else
    title="新建其他出入库"
    :model-value="form"
    :items="form.items"
    :fields="docFields"
    :columns="docColumns"
    hide-add
    hide-submit
    @update:items="(v) => (form.items = v)"
    @cancel="closeForm"
    @save="handleSave"
  >
    <template #field-direction>
      <el-radio-group v-model="form.direction">
        <el-radio-button value="in">入库</el-radio-button>
        <el-radio-button value="out">出库</el-radio-button>
      </el-radio-group>
    </template>
    <template #tools>
      <el-button type="primary" size="small" @click="pickerVisible = true">添加物料</el-button>
    </template>
    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
    </template>
  </DocForm>

  <el-dialog v-model="detailVisible" title="明细" width="660px">
    <el-table :data="detailItems" border size="small">
      <el-table-column type="index" label="序号" width="56" align="center" />
      <el-table-column prop="itemCode" label="编号" width="140" />
      <el-table-column prop="itemName" label="物料/产品名称" min-width="160" />
      <el-table-column prop="itemType" label="类型" width="90" />
      <el-table-column prop="quantity" label="数量" width="110" align="right" />
      <el-table-column prop="unit" label="单位" width="80" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />

  <PrintDialog
    v-model="printVisible"
    title="其他出入库单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['制单人', '仓管员', '审核人']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'OtherStock' })
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { otherStockList, otherStockCreate, otherStockItems } from '@/api/inventory'
import { warehouseList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'
import DocForm from '@/components/DocForm.vue'
import type { DocColumn, DocField } from '@/components/docForm'
import request from '@/utils/request'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const detailVisible = ref(false)
const pickerVisible = ref(false)
const warehouses = ref<any[]>([])
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, direction: undefined as string | undefined, type: undefined as string | undefined })
const form = reactive<any>({ items: [] })

function onSelect(item: any) {
  form.items.push({ itemId: item.id, itemType: 'material', itemName: `${item.code} ${item.name}`, quantity: 1 })
}

async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await otherStockList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.direction = undefined; query.type = undefined; query.page = 1; load() }

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  form.direction = 'in'
  loadWarehouses()
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

/** 基本信息字段 */
const docFields = computed<DocField[]>(() => [
  { key: 'direction', label: '方向', type: 'slot', required: true },
  {
    key: 'type',
    label: '类型',
    type: 'select',
    options: [
      { label: '期初', value: '期初' },
      { label: '盘盈', value: '盘盈' },
      { label: '盘亏', value: '盘亏' },
      { label: '领用', value: '领用' },
      { label: '其他', value: '其他' }
    ]
  },
  {
    key: 'warehouseId',
    label: '仓库',
    type: 'select',
    options: warehouses.value.map((w: any) => ({ label: w.name, value: w.id }))
  },
  { key: 'remark', label: '备注', span: 2 }
])

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'itemName', label: '物料', width: '260' },
  { key: 'quantity', label: '数量', width: '120', type: 'input', align: 'right' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)

async function handleSave() {
  if (!form.direction || !form.items || form.items.length === 0) { ElMessage.warning('请选择方向并添加明细'); return }
  await otherStockCreate({
    requestId: crypto.randomUUID(),
    direction: form.direction, type: form.type, warehouseId: form.warehouseId, remark: form.remark,
    items: form.items.map((i: any) => ({ itemId: i.itemId, itemType: i.itemType, quantity: Number(i.quantity) }))
  })
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

async function viewItems(row: any) {
  detailItems.value = await otherStockItems(row.id)
  detailVisible.value = true
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printCols = [
  { label: '序号', prop: '_idx', width: '40px', align: 'center', formatter: (_r: any, i: number) => i + 1 },
  { label: '物料/产品', prop: 'itemName' },
  { label: '数量', prop: 'quantity', align: 'right' }
]

async function handlePrint(row: any) {
  const res: any = await request.get('/inventory/otherStock/items', { params: { stockId: row.id } })
  printRows.value = res || []
  printHead.value = [
    [{ label: '单号', value: row.stockNo }, { label: '日期', value: row.stockDate }],
    [{ label: '方向', value: row.direction === 'in' ? '入库' : '出库' }, { label: '类型', value: row.type }]
  ]
  printRemark.value = row.remark
  printVisible.value = true
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
