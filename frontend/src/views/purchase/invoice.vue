<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="发票号" clearable style="width: 180px" @keyup.enter="handleSearch" />
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
      <el-button type="primary" @click="openForm()">新增发票</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="invoiceNo" label="发票号" width="160" />
      <el-table-column prop="sourceOrderNo" label="采购订单号" width="160" />
      <el-table-column prop="invoiceDate" label="开票日期" width="110" />
      <el-table-column prop="amount" label="发票金额" width="120" align="right" />
      <el-table-column prop="verifiedAmount" label="已核销" width="110" align="right" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)" size="small">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="submit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="approve(row)">审批</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="danger" @click="unapprove(row)">反审核</el-button>
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

  <!-- 新增采购发票：整页单据表单（无明细） -->
  <DocForm
    v-else
    title="新建采购发票"
    :model-value="form"
    :items="[]"
    :fields="docFields"
    :columns="[]"
    :pick-labels="pickLabels"
    @pick="onPick"
    @update:items="() => {}"
    @cancel="closeForm"
    @save="handleSave(false)"
    @submit="handleSave(true)"
  >
    <template #field-availableAmount>
      <el-input :model-value="availableAmount" readonly />
    </template>
  </DocForm>

  <DocPicker
    v-model="orderPickerVisible"
    title="选择采购订单"
    keyword-placeholder="采购单号 / 供应商 / 物料"
    :columns="PURCHASE_ORDER_COLS"
    :fetcher="(p) => purchaseOrderList(p as any)"
    @select="applyOrder"
  />

  <PrintDialog
    v-model="printVisible"
    title="采购发票"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    total-label="发票金额"
    :total-value="printTotal"
    :remark="printRemark"
    :show-upper="true"
    :total-amount="printTotal"
    :sign-labels="['制单人', '审核人', '供应商']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseInvoice' })
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseInvoiceList, purchaseInvoiceCreate, purchaseInvoiceSubmit, purchaseInvoiceApprove, purchaseInvoiceUnapprove, purchaseOrderList } from '@/api/purchase'
import PrintDialog from '@/components/PrintDialog.vue'
import DocForm from '@/components/DocForm.vue'
import DocPicker from '@/components/DocPicker.vue'
import { PURCHASE_ORDER_COLS } from '@/components/docPickerPresets'
import type { DocField } from '@/components/docForm'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const orderPickerVisible = ref(false)
const pickedOrder = ref<any>(null)
const orders = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({})

const availableAmount = computed(() => {
  const o = pickedOrder.value || orders.value.find((x) => x.id === form.orderId)
  if (!o) return ''
  const invoiced = Number(o.invoicedAmount || 0)
  const totalAmt = Number(o.totalAmount || 0)
  return (totalAmt - invoiced).toFixed(2)
})

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }

async function loadOrders() {
  const res = await purchaseOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records.filter((o: any) => o.approveStatus === 'approved')
}

async function load() {
  loading.value = true
  try {
    const res = await purchaseInvoiceList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.approveStatus = undefined; query.page = 1; load() }

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  pickedOrder.value = null
  loadOrders()
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

/** 选择器里那一栏显示什么：单号 + 供应商 + 金额（不是裸编号） */
const pickLabels = computed<Record<string, string>>(() => ({
  orderId: pickedOrder.value
    ? `${pickedOrder.value.orderNo}  ${pickedOrder.value.supplierName || ''}  ￥${pickedOrder.value.totalAmount ?? ''}`
    : ''
}))

function onPick(key: string) {
  if (key === 'orderId') orderPickerVisible.value = true
}

function applyOrder(o: any) {
  pickedOrder.value = o
  form.orderId = o.id
}

/** 基本信息字段 */
const docFields = computed<DocField[]>(() => [
  { key: 'orderId', label: '采购订单', type: 'pick', required: true },
  { key: 'availableAmount', label: '可开票金额', type: 'slot' },
  { key: 'amount', label: '发票金额', required: true, placeholder: '不超过可开票金额' },
  { key: 'remark', label: '备注', span: 2 }
])

async function handleSave(submitAfter = false) {
  if (!form.orderId || form.amount == null) { ElMessage.warning('请选择订单并填写发票金额'); return }
  const created: any = await purchaseInvoiceCreate({ orderId: form.orderId, amount: Number(form.amount), remark: form.remark })
  if (submitAfter && created?.id) {
    await purchaseInvoiceSubmit(created.id)
    ElMessage.success('已保存并提交')
  } else {
    ElMessage.success('保存成功')
  }
  formVisible.value = false
  load()
}

async function submit(row: any) { await purchaseInvoiceSubmit(row.id); ElMessage.success('已提交'); load() }
async function approve(row: any) { await purchaseInvoiceApprove(row.id); ElMessage.success('审批通过'); load() }
async function unapprove(row: any) {
  await ElMessageBox.confirm('确定反审核该发票吗？', '提示', { type: 'warning' })
  await purchaseInvoiceUnapprove(row.id); ElMessage.success('已反审核'); load()
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printTotal = ref<any>(0)
const printCols = [
  { label: '项目', prop: 'item' },
  { label: '金额', prop: 'amt', align: 'right' }
]

function handlePrint(row: any) {
  printHead.value = [
    [{ label: '发票号', value: row.invoiceNo }, { label: '开票日期', value: row.invoiceDate }],
    [{ label: '采购订单号', value: row.sourceOrderNo }, { label: '审批状态', value: approveText(row.approveStatus) }]
  ]
  const unverified = (Number(row.amount || 0) - Number(row.verifiedAmount || 0)).toFixed(2)
  printRows.value = [
    { item: '发票金额', amt: row.amount },
    { item: '已核销金额', amt: row.verifiedAmount },
    { item: '未核销金额', amt: unverified }
  ]
  printRemark.value = row.remark
  printTotal.value = row.amount
  printVisible.value = true
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
