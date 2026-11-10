<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="工单">
        <el-select v-model="query.workOrderId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="审工状态">
        <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 140px">
          <el-option label="待审" value="pending" />
          <el-option label="已审" value="approved" />
          <el-option label="驳回" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openForm()">新增报工</el-button>
      <el-button type="success" @click="handleBatchApprove">一键审核</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="reportNo" label="报工单号" width="160" />
      <el-table-column label="工单" width="160">
        <template #default="{ row }">{{ orderNo(row.workOrderId) }}</template>
      </el-table-column>
      <el-table-column label="工序" width="120">
        <template #default="{ row }">{{ processName(row.processId) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="报工数量" width="100" align="right" />
      <el-table-column prop="qualifiedQty" label="良品" width="80" align="right" />
      <el-table-column prop="defectiveQty" label="不良品" width="80" align="right" />
      <el-table-column label="不良品项" width="120">
        <template #default="{ row }">{{ defectName(row.defectItemId) }}</template>
      </el-table-column>
      <el-table-column prop="workHours" label="工时" width="80" align="right" />
      <el-table-column prop="reporterName" label="报工人" width="100" />
      <el-table-column label="审工状态" width="90">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditStatus)" size="small">{{ auditText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.auditStatus === 'pending'" link type="success" @click="approve(row)">审核</el-button>
          <el-button v-if="row.auditStatus === 'pending'" link type="danger" @click="reject(row)">驳回</el-button>
          <el-button v-if="row.auditStatus === 'approved'" link type="warning" @click="unapprove(row)">反审核</el-button>
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

  <!-- 新增报工：整页单据表单 -->
  <DocForm
    v-else
    title="新建报工单"
    :model-value="form"
    :items="[]"
    :fields="docFields"
    :columns="[]"
    hide-submit
    @update:items="() => {}"
    @cancel="closeForm"
    @save="handleSave"
  >
    <!-- 生产工单：点开是表格选择器，看得见产品/数量/状态，不是一个光秃秃的编号下拉 -->
    <template #field-workOrderId>
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
      <el-select
        v-model="form.processId"
        placeholder="请选择工序"
        filterable
        size="small"
        :disabled="!form.workOrderId"
        style="width: 100%"
      >
        <el-option v-for="p in processes" :key="p.processId" :label="p.processName" :value="p.processId" />
      </el-select>
    </template>
    <template #field-defectItemId>
      <el-select v-model="form.defectItemId" placeholder="选择不良品项" clearable filterable size="small" style="width: 100%">
        <el-option v-for="d in defects" :key="d.id" :label="d.name" :value="d.id" />
      </el-select>
    </template>
  </DocForm>

  <!-- 生产工单选择器（表格） -->
  <DocPicker
    v-model="orderPickerVisible"
    title="选择生产工单"
    keyword-placeholder="工单号 / 产品编号 / 产品名称"
    :columns="orderPickCols"
    :fetcher="fetchOrders"
    @select="applyOrder"
  />

  <PrintDialog
    v-model="printVisible"
    title="报工单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['报工人', '审核人']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'ReportWork' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reportWorkList, reportWorkCreate, reportWorkApprove, reportWorkUnapprove, reportWorkReject, reportWorkBatchApprove, productionOrderList } from '@/api/production'
import { processRouteItems, processList, defectList } from '@/api/base'
import PrintDialog from '@/components/PrintDialog.vue'
import DocForm from '@/components/DocForm.vue'
import DocPicker from '@/components/DocPicker.vue'
import type { DocField } from '@/components/docForm'
import type { DocPickColumn } from '@/components/DocPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const orderPickerVisible = ref(false)
/** 选中的生产工单（只用于显示，不塞进 form 免得污染提交体） */
const pickedOrder = ref<any>(null)
const orders = ref<any[]>([])
const processes = ref<any[]>([])
const allProcesses = ref<any[]>([])
const defects = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, workOrderId: undefined as number | undefined, auditStatus: undefined as string | undefined })
const form = reactive<any>({})

function orderNo(id?: number) {
  const o = orders.value.find((x) => x.id === id)
  return o ? o.orderNo : ''
}
function processName(id?: number) {
  const p = allProcesses.value.find((x) => x.id === id)
  return p ? p.name : ''
}

async function loadAllProcesses() {
  const res = await processList({ page: 1, pageSize: 200 })
  allProcesses.value = res.records
}
function defectName(id?: number) {
  const d = defects.value.find((x) => x.id === id)
  return d ? d.name : ''
}
async function loadDefects() {
  const res = await defectList({ page: 1, pageSize: 200, status: 1 })
  defects.value = res.records
}
function auditText(v: string) { return { pending: '待审', approved: '已审', rejected: '驳回' }[v] || v }
function auditTagType(v: string) { return { pending: 'warning', approved: 'success', rejected: 'danger' }[v] || 'info' }

async function loadOrders() {
  const res = await productionOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}

async function onOrderChange(orderId?: number) {
  form.processId = undefined
  processes.value = []
  if (!orderId) return
  const order = orders.value.find((x) => x.id === orderId)
  if (order && order.processRouteId) {
    const items = await processRouteItems(order.processRouteId)
    processes.value = items.map((it: any) => ({
      processId: it.processId,
      processName: processName(it.processId) || ('工序' + it.processId)
    }))
  }
}

async function load() {
  loading.value = true
  try {
    const res = await reportWorkList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.workOrderId = undefined; query.auditStatus = undefined; query.page = 1; load() }

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  processes.value = []
  pickedOrder.value = null
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

/** 基本信息字段 */
const docFields: DocField[] = [
  { key: 'workOrderId', label: '生产工单', type: 'slot', required: true },
  { key: 'processId', label: '工序', type: 'slot', required: true },
  { key: 'quantity', label: '报工数量', required: true },
  { key: 'qualifiedQty', label: '良品数量' },
  { key: 'defectiveQty', label: '不良品数量' },
  { key: 'defectItemId', label: '不良品项', type: 'slot' },
  { key: 'defectReason', label: '不良原因' },
  { key: 'workHours', label: '工时(小时)' },
  { key: 'remark', label: '备注', span: 2 }
]

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
  },
  {
    prop: 'bizStatus',
    label: '业务状态',
    width: '100',
    format: (r: any) => ({ pending: '待开始', started: '生产中', finished: '已完工' }[r.bizStatus as string] || r.bizStatus || '')
  }
]

function fetchOrders(p: { page: number; pageSize: number; keyword: string }) {
  return productionOrderList(p as any)
}

/** 选中工单 → 带出工序 */
async function applyOrder(order: any) {
  pickedOrder.value = order
  form.workOrderId = order.id
  form.processId = undefined
  processes.value = []
  if (order.processRouteId) {
    const items = await processRouteItems(order.processRouteId)
    processes.value = items.map((it: any) => ({
      processId: it.processId,
      processName: processName(it.processId) || '工序' + it.processId
    }))
  }
}

async function handleSave() {
  if (!form.workOrderId || !form.processId || form.quantity == null) {
    ElMessage.warning('请选择工单、工序并填写报工数量')
    return
  }
  await reportWorkCreate(form)
  ElMessage.success('报工成功')
  formVisible.value = false
  load()
}

async function approve(row: any) {
  await reportWorkApprove(row.id)
  ElMessage.success('审核通过')
  load()
}
async function unapprove(row: any) {
  await ElMessageBox.confirm('确定反审核该报工单吗？', '提示', { type: 'warning' })
  await reportWorkUnapprove(row.id)
  ElMessage.success('反审核成功')
  load()
}
async function reject(row: any) {
  const { value } = await ElMessageBox.prompt('请输入驳回意见', '驳回', { inputPlaceholder: '驳回原因' })
  await reportWorkReject(row.id, value || '')
  ElMessage.success('已驳回')
  load()
}
async function handleBatchApprove() {
  await ElMessageBox.confirm('确定一键审核所有待审报工单吗？', '提示', { type: 'warning' })
  await reportWorkBatchApprove(query.workOrderId)
  ElMessage.success('一键审核完成')
  load()
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printCols = [
  { label: '项目', prop: 'item' },
  { label: '数值', prop: 'amt', align: 'right' }
]

function handlePrint(row: any) {
  printHead.value = [
    [{ label: '报工单号', value: row.reportNo }, { label: '生产工单', value: orderNo(row.workOrderId) }],
    [{ label: '工序', value: processName(row.processId) }, { label: '报工人', value: row.reporterName }],
    [{ label: '审工状态', value: auditText(row.auditStatus) }, { label: '不良品项', value: defectName(row.defectItemId) }]
  ]
  printRows.value = [
    { item: '报工数量', amt: row.quantity },
    { item: '良品数量', amt: row.qualifiedQty },
    { item: '不良品数量', amt: row.defectiveQty },
    { item: '工时（小时）', amt: row.workHours }
  ]
  printRemark.value = row.remark
  printVisible.value = true
}

loadOrders()
loadAllProcesses()
loadDefects()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.pick-field { display: flex; gap: 6px; align-items: center; }
.pick-field .el-input { flex: 1; }
</style>
