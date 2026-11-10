<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="核销单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
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
      <el-button type="primary" @click="openForm()">新增核销</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="verificationNo" label="核销单号" width="160" />
      <el-table-column prop="invoiceNo" label="发票号" width="160" />
      <el-table-column prop="amount" label="核销金额" width="120" align="right" />
      <el-table-column prop="verifyDate" label="核销日期" width="110" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)" size="small">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="submit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="approve(row)">审批</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="danger" @click="unapprove(row)">反审核</el-button>
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

  <!-- 新增销售核销：整页单据表单（无明细） -->
  <DocForm
    v-else
    title="新建销售核销"
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
    v-model="invoicePickerVisible"
    title="选择销售发票"
    keyword-placeholder="发票号 / 来源订单号"
    :columns="INVOICE_COLS"
    :fetcher="(p) => salesInvoiceList(p as any)"
    @select="applyInvoice"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'SalesVerification' })
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { salesVerificationList, salesVerificationCreate, salesVerificationSubmit, salesVerificationApprove, salesVerificationUnapprove, salesInvoiceList } from '@/api/sales'
import DocForm from '@/components/DocForm.vue'
import DocPicker from '@/components/DocPicker.vue'
import { INVOICE_COLS } from '@/components/docPickerPresets'
import type { DocField } from '@/components/docForm'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const invoicePickerVisible = ref(false)
const pickedInvoice = ref<any>(null)
const invoices = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({})

const availableAmount = computed(() => {
  const inv = pickedInvoice.value || invoices.value.find((x) => x.id === form.invoiceId)
  if (!inv) return ''
  return (Number(inv.amount || 0) - Number(inv.verifiedAmount || 0)).toFixed(2)
})

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }

async function loadInvoices() {
  const res = await salesInvoiceList({ page: 1, pageSize: 200 })
  invoices.value = res.records.filter((i: any) => i.approveStatus === 'approved' && Number(i.verifiedAmount || 0) < Number(i.amount || 0))
}

function onInvoiceChange() {}

async function load() {
  loading.value = true
  try {
    const res = await salesVerificationList(query)
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
  pickedInvoice.value = null
  loadInvoices()
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

const pickLabels = computed<Record<string, string>>(() => ({
  invoiceId: pickedInvoice.value
    ? `${pickedInvoice.value.invoiceNo}  ${pickedInvoice.value.sourceOrderNo || ''}  ￥${pickedInvoice.value.amount ?? ''}`
    : ''
}))

function onPick(key: string) {
  if (key === 'invoiceId') invoicePickerVisible.value = true
}

function applyInvoice(inv: any) {
  pickedInvoice.value = inv
  form.invoiceId = inv.id
}

/** 基本信息字段 */
const docFields: DocField[] = [
  { key: 'invoiceId', label: '销售发票', type: 'pick', required: true },
  { key: 'availableAmount', label: '可核销金额', type: 'slot' },
  { key: 'amount', label: '核销金额', required: true, placeholder: '不超过可核销金额' },
  { key: 'remark', label: '备注', span: 2 }
]

async function handleSave(submitAfter = false) {
  if (!form.invoiceId || form.amount == null) { ElMessage.warning('请选择发票并填写核销金额'); return }
  const created: any = await salesVerificationCreate({ invoiceId: form.invoiceId, amount: Number(form.amount), remark: form.remark })
  if (submitAfter && created?.id) {
    await salesVerificationSubmit(created.id)
    ElMessage.success('已保存并提交')
  } else {
    ElMessage.success('保存成功')
  }
  formVisible.value = false
  load()
}

async function submit(row: any) { await salesVerificationSubmit(row.id); ElMessage.success('已提交'); load() }
async function approve(row: any) { await salesVerificationApprove(row.id); ElMessage.success('审批通过'); load() }
async function unapprove(row: any) {
  await ElMessageBox.confirm('确定反审核该核销单吗？', '提示', { type: 'warning' })
  await salesVerificationUnapprove(row.id); ElMessage.success('已反审核'); load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
