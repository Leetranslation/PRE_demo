<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="类型">
        <el-select v-model="query.type" placeholder="全部" clearable style="width: 130px">
          <el-option label="整单外协" value="whole" />
          <el-option label="工序外协" value="process" />
        </el-select>
      </el-form-item>
      <el-form-item label="审批状态">
        <el-select v-model="query.approveStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="待提交" value="pending" />
          <el-option label="审批中" value="approving" />
          <el-option label="已审批" value="approved" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openForm()">新增外协单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="orderNo" label="外协单号" width="160" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'whole' ? 'primary' : 'warning'" size="small">
            {{ row.type === 'whole' ? '整单外协' : '工序外协' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生产工单" width="150">
        <template #default="{ row }">{{ orderNo(row.orderId) }}</template>
      </el-table-column>
      <el-table-column label="外协供应商" min-width="140">
        <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="100" align="right" />
      <el-table-column prop="deliveryDate" label="交货日期" width="110" />
      <el-table-column label="审批状态" width="90">
        <template #default="{ row }">{{ approveText(row.approveStatus) }}</template>
      </el-table-column>
      <el-table-column label="业务状态" width="90">
        <template #default="{ row }">
          <el-tag :type="bizType(row.bizStatus)" size="small">{{ bizText(row.bizStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="doSubmit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="doApprove(row)">审批</el-button>
          <el-button v-if="row.approveStatus === 'approved' && row.bizStatus === 'pending'" link type="warning" @click="doStart(row)">开始外协</el-button>
          <el-button v-if="row.bizStatus === 'outsourcing'" link type="success" @click="doFinish(row)">完工</el-button>
          <el-button v-if="row.bizStatus !== 'finished' && row.bizStatus !== 'voided'" link type="danger" @click="doVoid(row)">作废</el-button>
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

  <!-- 新增/编辑外协单：整页单据表单 -->
  <DocForm
    v-else
    :title="formTitle"
    :model-value="form"
    :items="form.items"
    :fields="docFields"
    :columns="docColumns"
    hide-add
    :hide-submit="!!form.id"
    @update:items="(v) => (form.items = v)"
    @cancel="closeForm"
    @save="handleSave"
  >
    <template #field-type>
      <el-radio-group v-model="form.type" :disabled="!!form.id">
        <el-radio-button value="whole">整单外协</el-radio-button>
        <el-radio-button value="process">工序外协</el-radio-button>
      </el-radio-group>
    </template>
    <template #field-orderId>
      <div class="pick-field">
        <el-input
          :model-value="pickedOrder ? `${pickedOrder.orderNo} ${pickedOrder.productName || ''}` : ''"
          readonly
          size="small"
          placeholder="点右侧选择工单"
        />
        <el-button size="small" @click="orderPickerVisible = true">选择</el-button>
      </div>
    </template>
    <template #field-processId>
      <el-select v-model="form.processId" placeholder="请选择工序" filterable size="small" style="width: 100%">
        <el-option v-for="p in processes" :key="p.id" :label="p.name" :value="p.id" />
      </el-select>
    </template>
    <template #field-supplierId>
      <el-select v-model="form.supplierId" placeholder="请选择供应商" filterable size="small" style="width: 100%">
        <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
    </template>
    <template #tools>
      <el-button type="primary" size="small" @click="pickerVisible = true">添加物料</el-button>
    </template>
    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
    </template>
  </DocForm>

  <DocPicker
    v-model="orderPickerVisible"
    title="选择生产工单"
    keyword-placeholder="工单号 / 产品编号 / 产品名称"
    :columns="orderPickCols"
    :fetcher="fetchOrders"
    @select="applyOrder"
  />

  <el-dialog v-model="detailVisible" title="外协明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="itemName" label="物料/产品" width="160" />
      <el-table-column prop="itemType" label="类型" width="100" />
      <el-table-column prop="quantity" label="数量" width="110" align="right" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />

  <PrintDialog
    v-model="printVisible"
    title="外协单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['制单人', '审核人', '供应商']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'OutsourceOrder' })
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  outsourceOrderList, outsourceOrderItems, outsourceOrderCreate, outsourceOrderUpdate,
  outsourceOrderSubmit, outsourceOrderApprove, outsourceOrderStart, outsourceOrderFinish, outsourceOrderVoid
} from '@/api/outsource'
import { productionOrderList } from '@/api/production'
import { supplierList, processList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'
import DocForm from '@/components/DocForm.vue'
import DocPicker from '@/components/DocPicker.vue'
import type { DocColumn, DocField } from '@/components/docForm'
import type { DocPickColumn } from '@/components/DocPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const orders = ref<any[]>([])
const suppliers = ref<any[]>([])
const processes = ref<any[]>([])
const formVisible = ref(false)
const orderPickerVisible = ref(false)
const pickedOrder = ref<any>(null)
const detailVisible = ref(false)
const pickerVisible = ref(false)
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, type: undefined as string | undefined, approveStatus: undefined as string | undefined })
const form = reactive<any>({ items: [] })

function orderNo(id: number) { return orders.value.find((o) => o.id === id)?.orderNo || `#${id}` }
function supplierName(id: number) { return suppliers.value.find((s) => s.id === id)?.name || `#${id}` }
function approveText(s: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[s] || s }
function bizText(s: string) { return { pending: '未开始', outsourcing: '外协中', finished: '已完工', voided: '已作废' }[s] || s }
function bizType(s: string) { return { pending: 'info', outsourcing: 'primary', finished: 'success', voided: 'danger' }[s] || 'info' }

function onSelect(item: any) {
  form.items.push({ itemId: item.id, itemType: 'material', itemName: `${item.code} ${item.name}`, quantity: 1 })
}

async function loadBase() {
  const [o, s, p] = await Promise.all([
    productionOrderList({ page: 1, pageSize: 200 }),
    supplierList({ page: 1, pageSize: 200, status: 1 }),
    processList({ page: 1, pageSize: 200 })
  ])
  orders.value = o.records
  suppliers.value = s.records
  processes.value = p.records
}

async function load() {
  loading.value = true
  try {
    const res = await outsourceOrderList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.type = undefined; query.approveStatus = undefined; query.page = 1; load() }

function openForm(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  if (row) {
    Object.assign(form, row)
    detailItems.value = []
    pickedOrder.value = orders.value.find((o: any) => o.id === row.orderId) || { orderNo: '#' + row.orderId }
  } else {
    form.type = 'whole'
    pickedOrder.value = null
  }
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

/** 工单选择器的列 */
const orderPickCols: DocPickColumn[] = [
  { prop: 'orderNo', label: '工单号', width: '170' },
  { prop: 'productCode', label: '产品编号', width: '140' },
  { prop: 'productName', label: '产品名称', minWidth: '150' },
  { prop: 'quantity', label: '生产数量', width: '100', align: 'right' },
  { prop: 'completedQty', label: '已完成', width: '90', align: 'right' },
  {
    prop: 'approveStatus',
    label: '审批状态',
    width: '100',
    format: (r: any) => ({ pending: '待提交', approving: '审批中', approved: '已审批' }[r.approveStatus as string] || r.approveStatus || '')
  }
]

function fetchOrders(p: { page: number; pageSize: number; keyword: string }) {
  return productionOrderList(p as any)
}

function applyOrder(order: any) {
  pickedOrder.value = order
  form.orderId = order.id
}

const formTitle = computed(() => (form.id ? '编辑外协单' : '新建外协单'))

/** 基本信息字段（工序外协才显示「外协工序」） */
const docFields = computed<DocField[]>(() => {
  const fs: DocField[] = [
    { key: 'type', label: '类型', type: 'slot', required: true },
    { key: 'orderId', label: '生产工单', type: 'slot', required: true }
  ]
  if (form.type === 'process') {
    fs.push({ key: 'processId', label: '外协工序', type: 'slot', required: true })
  }
  fs.push(
    { key: 'supplierId', label: '外协供应商', type: 'slot', required: true },
    { key: 'quantity', label: '外协数量' },
    { key: 'deliveryDate', label: '交货日期', type: 'date' },
    { key: 'remark', label: '备注', span: 2 }
  )
  return fs
})

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'itemName', label: '物料/产品', width: '260' },
  { key: 'quantity', label: '数量', width: '120', type: 'input', align: 'right' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)

async function handleSave() {
  if (!form.orderId) { ElMessage.warning('请选择生产工单'); return }
  if (!form.supplierId) { ElMessage.warning('请选择外协供应商'); return }
  if (form.type === 'process' && !form.processId) { ElMessage.warning('工序外协必须选择工序'); return }
  const data = {
    id: form.id, type: form.type, orderId: form.orderId, processId: form.processId,
    supplierId: form.supplierId, quantity: form.quantity ? Number(form.quantity) : undefined,
    deliveryDate: form.deliveryDate, remark: form.remark,
    items: form.items.map((i: any) => ({ itemId: i.itemId, itemType: i.itemType, quantity: Number(i.quantity), unit: i.unit }))
  }
  if (form.id) await outsourceOrderUpdate(data)
  else await outsourceOrderCreate(data)
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

async function doSubmit(row: any) { await outsourceOrderSubmit(row.id); ElMessage.success('已提交'); load() }
async function doApprove(row: any) { await outsourceOrderApprove(row.id); ElMessage.success('已审批'); load() }
async function doStart(row: any) { await outsourceOrderStart(row.id); ElMessage.success('开始外协'); load() }
async function doFinish(row: any) { await outsourceOrderFinish(row.id); ElMessage.success('外协完工'); load() }
async function doVoid(row: any) {
  await ElMessageBox.confirm('确定作废该外协单吗？', '提示', { type: 'warning' })
  await outsourceOrderVoid(row.id); ElMessage.success('已作废'); load()
}
async function viewItems(row: any) {
  detailItems.value = await outsourceOrderItems(row.id)
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
  { label: '类型', prop: 'itemType', align: 'center' },
  { label: '数量', prop: 'quantity', align: 'right' },
  { label: '单位', prop: 'unit', align: 'center' }
]

async function handlePrint(row: any) {
  printRows.value = await outsourceOrderItems(row.id)
  printHead.value = [
    [{ label: '外协单号', value: row.orderNo }, { label: '类型', value: row.type === 'whole' ? '整单外协' : '工序外协' }],
    [{ label: '生产工单', value: orderNo(row.orderId) }, { label: '外协供应商', value: supplierName(row.supplierId) }],
    [{ label: '数量', value: row.quantity }, { label: '交货日期', value: row.deliveryDate }]
  ]
  printRemark.value = row.remark
  printVisible.value = true
}

loadBase()
load()
</script>

<style scoped>
.pick-field { display: flex; gap: 6px; align-items: center; }
.pick-field .el-input { flex: 1; }
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
