<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="外协类型">
        <el-select v-model="query.issueType" style="width: 150px" @change="handleSearch">
          <el-option label="整单外协领料" value="outsource_whole" />
          <el-option label="工序外协领料" value="outsource_process" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openForm">新增外协领料</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="issueNo" label="领料单号" width="170" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ row.issueType === 'outsource_whole' ? '整单外协领料' : '工序外协领料' }}</template>
      </el-table-column>
      <el-table-column label="外协单" width="160">
        <template #default="{ row }">{{ outsourceNo(row.outsourceId) }}</template>
      </el-table-column>
      <el-table-column label="生产工单" width="150">
        <template #default="{ row }">{{ orderNo(row.orderId) }}</template>
      </el-table-column>
      <el-table-column label="仓库" width="120">
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
      <el-table-column prop="issueDate" label="领料日期" width="110" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewItems(row)">明细</el-button>
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

  <!-- 新增外协领料：整页单据表单（无审批流） -->
  <DocForm
    v-else
    title="新建外协领料"
    :model-value="form"
    :items="form.items"
    :fields="docFields"
    :columns="docColumns"
    :pick-labels="pickLabels"
    @pick="onPick"
    hide-add
    hide-submit
    @update:items="(v) => (form.items = v)"
    @cancel="closeForm"
    @save="handleSave"
  >
    <template #field-outsourceId />
    <template #tools>
      <el-button type="primary" size="small" @click="pickerVisible = true">添加物料</el-button>
    </template>
    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
    </template>
  </DocForm>

  <DocPicker
    v-model="outsourcePickerVisible"
    title="选择外协单"
    keyword-placeholder="外协单号 / 供应商"
    :columns="OUTSOURCE_ORDER_COLS"
    :fetcher="(p) => outsourceOrderList(p as any)"
    @select="applyOutsource"
  />

  <el-dialog v-model="detailVisible" title="领料明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialName" label="物料" width="140" />
      <el-table-column prop="quantity" label="数量" width="110" align="right" />
      <el-table-column prop="batchNo" label="批次号" width="130" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'OutsourceIssue' })
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { materialIssueList, materialIssueCreate, materialIssueItems, productionOrderList } from '@/api/production'
import { outsourceOrderList } from '@/api/outsource'
import { warehouseList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'
import DocForm from '@/components/DocForm.vue'
import DocPicker from '@/components/DocPicker.vue'
import { OUTSOURCE_ORDER_COLS } from '@/components/docPickerPresets'
import type { DocColumn, DocField } from '@/components/docForm'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const outsourceOrders = ref<any[]>([])
const warehouses = ref<any[]>([])
const orders = ref<any[]>([])
const formVisible = ref(false)
const outsourcePickerVisible = ref(false)
const pickedOutsource = ref<any>(null)
const detailVisible = ref(false)
const pickerVisible = ref(false)
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, issueType: 'outsource_whole' })
const form = reactive<any>({ items: [] })

function outsourceNo(id: number) { return outsourceOrders.value.find((o) => o.id === id)?.orderNo || `#${id}` }
function orderNo(id: number) { return orders.value.find((o) => o.id === id)?.orderNo || `#${id}` }
function warehouseName(id: number) { return warehouses.value.find((w) => w.id === id)?.name || `#${id}` }

function onSelect(item: any) {
  form.items.push({ materialId: item.id, materialName: `${item.code} ${item.name}`, quantity: 1, batchNo: '' })
}

function onOutsourceChange(id: number) {
  const o = outsourceOrders.value.find((x) => x.id === id)
  if (o) form.orderId = o.orderId
}

async function loadBase() {
  const [o, w, ord] = await Promise.all([
    outsourceOrderList({ page: 1, pageSize: 200, approveStatus: 'approved' }),
    warehouseList({ page: 1, pageSize: 100, status: 1 }),
    productionOrderList({ page: 1, pageSize: 200 })
  ])
  outsourceOrders.value = o.records
  warehouses.value = w.records
  orders.value = ord.records
}

async function load() {
  loading.value = true
  try {
    const res = await materialIssueList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  pickedOutsource.value = null
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

const pickLabels = computed<Record<string, string>>(() => ({
  outsourceId: pickedOutsource.value
    ? `${pickedOutsource.value.orderNo}  ${pickedOutsource.value.type === 'whole' ? '整单外协' : '工序外协'}  ${pickedOutsource.value.supplierName || ''}`
    : ''
}))

function onPick(key: string) {
  if (key === 'outsourceId') outsourcePickerVisible.value = true
}

function applyOutsource(o: any) {
  pickedOutsource.value = o
  form.outsourceId = o.id
  onOutsourceChange(o.id)
}

/** 基本信息字段 */
const docFields = computed<DocField[]>(() => [
  { key: 'outsourceId', label: '外协单', type: 'pick', required: true },
  {
    key: 'warehouseId',
    label: '仓库',
    type: 'select',
    required: true,
    options: warehouses.value.map((w: any) => ({ label: w.name, value: w.id }))
  },
  { key: 'remark', label: '备注', span: 2 }
])

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'materialName', label: '物料', width: '260' },
  { key: 'quantity', label: '数量', width: '110', type: 'input', align: 'right' },
  { key: 'batchNo', label: '批次号', width: '140', type: 'input' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)

async function handleSave() {
  if (!form.outsourceId) { ElMessage.warning('请选择外协单'); return }
  if (!form.warehouseId) { ElMessage.warning('请选择仓库'); return }
  if (!form.items.length) { ElMessage.warning('请添加领料明细'); return }
  const o = outsourceOrders.value.find((x) => x.id === form.outsourceId)
  const issueType = o && o.type === 'process' ? 'outsource_process' : 'outsource_whole'
  await materialIssueCreate({
    requestId: crypto.randomUUID(),
    issueType,
    orderId: form.orderId,
    outsourceId: form.outsourceId,
    warehouseId: form.warehouseId,
    remark: form.remark,
    items: form.items.map((i: any) => ({ materialId: i.materialId, quantity: Number(i.quantity), batchNo: i.batchNo }))
  })
  ElMessage.success('外协领料成功')
  formVisible.value = false
  load()
}

async function viewItems(row: any) {
  detailItems.value = await materialIssueItems(row.id)
  detailVisible.value = true
}

loadBase()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.tip { font-size: 12px; color: #909399; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
