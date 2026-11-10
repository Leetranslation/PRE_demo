<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="合同号" clearable style="width: 180px" @keyup.enter="handleSearch" />
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
      <el-button type="primary" @click="openForm()">新增合同</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="contractNo" label="合同号" width="160" />
      <el-table-column prop="customerName" label="客户" width="140" />
      <el-table-column prop="productCode" label="产品编号" width="150" show-overflow-tooltip />
      <el-table-column prop="productName" label="产品名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="contractDate" label="签订日期" width="110" />
      <el-table-column prop="amount" label="合同金额" width="120" align="right" />
      <el-table-column prop="salesman" label="业务员" width="100" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)" size="small">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="业务状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.bizStatus === 'converted' ? 'success' : 'info'" size="small">
            {{ row.bizStatus === 'converted' ? '已转单' : '未转单' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="submit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="approve(row)">审批</el-button>
          <el-button v-if="row.approveStatus === 'approved' && row.bizStatus !== 'converted'" link type="warning" @click="convert(row)">转销售订单</el-button>
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

  <!-- 新增合同：整页单据表单 -->
  <DocForm
    v-else
    title="新建合同"
    :model-value="form"
    :items="form.items"
    :fields="docFields"
    :columns="docColumns"
    hide-add
    @update:items="(v) => (form.items = v)"
    @cancel="closeForm"
    @save="handleSave(false)"
    @submit="handleSave(true)"
  >
    <template #field-customerId>
      <el-select
        v-model="form.customerId"
        filterable
        remote
        :remote-method="searchCustomer"
        :loading="customerLoading"
        placeholder="搜索客户"
        clearable
        size="small"
        style="width: 100%"
      >
        <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
    </template>
    <template #tools>
      <el-button type="primary" size="small" @click="productPickerVisible = true">添加产品</el-button>
    </template>
    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
      <span>价税合计：￥{{ sumAmount }}</span>
    </template>
  </DocForm>

  <ProductPicker v-model="productPickerVisible" @select="onProductSelect" />

  <PrintDialog
    v-model="printVisible"
    title="销售合同"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    total-label="合同金额（含税）"
    :total-value="printTotal"
    :remark="printRemark"
    :show-upper="true"
    :total-amount="printTotal"
    :sign-labels="['制单人', '审核人', '客户签章']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Contract' })
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { contractList, contractItems, contractCreate, contractSubmit, contractApprove, contractUnapprove, contractConvert } from '@/api/sales'
import { customerList } from '@/api/base'
import DocForm from '@/components/DocForm.vue'
import type { DocColumn, DocField } from '@/components/docForm'
import ProductPicker from '@/components/ProductPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const productPickerVisible = ref(false)
const customers = ref<any[]>([])
const customerLoading = ref(false)
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({ items: [] })

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }

async function searchCustomer(keyword: string) {
  customerLoading.value = true
  try {
    const res = await customerList({ page: 1, pageSize: 20, keyword, status: 1 })
    customers.value = res.records
  } finally {
    customerLoading.value = false
  }
}

async function load() {
  loading.value = true
  try {
    const res = await contractList(query)
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
  form.items = []
  searchCustomer('')
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

/** 基本信息字段 */
const docFields: DocField[] = [
  { key: 'customerId', label: '客户', type: 'slot', required: true },
  { key: 'salesman', label: '业务员' },
  { key: 'paymentTerms', label: '付款条款', span: 2, placeholder: '如：30%预付，70%货到付款' },
  { key: 'remark', label: '备注', span: 2 }
]

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'productName', label: '产品', width: '240' },
  { key: 'quantity', label: '数量', width: '110', type: 'input', align: 'right' },
  { key: 'price', label: '含税单价', width: '120', type: 'input', align: 'right' },
  { key: 'taxRate', label: '税率%', width: '90', type: 'input', align: 'right' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)
const sumAmount = computed(() =>
  form.items
    .reduce((s: number, r: any) => s + (Number(r.quantity) || 0) * (Number(r.price) || 0), 0)
    .toFixed(2)
)

function onProductSelect(product: any) {
  form.items.push({ productId: product.id, productName: `${product.code} ${product.name}`, quantity: 1, price: 0, taxRate: 13 })
}

async function handleSave(submitAfter = false) {
  if (!form.customerId) { ElMessage.warning('请选择客户'); return }
  if (!form.items || form.items.length === 0) { ElMessage.warning('请添加合同明细'); return }
  const payload = {
    customerId: form.customerId,
    salesman: form.salesman,
    paymentTerms: form.paymentTerms,
    remark: form.remark,
    items: form.items.map((i: any) => ({
      productId: i.productId, spec: i.spec, unit: i.unit,
      quantity: Number(i.quantity), price: Number(i.price), taxRate: Number(i.taxRate), remark: i.remark
    }))
  }
  const created: any = await contractCreate(payload)
  if (submitAfter && created?.id) {
    await contractSubmit(created.id)
    ElMessage.success('已保存并提交')
  } else {
    ElMessage.success('保存成功')
  }
  formVisible.value = false
  load()
}

async function submit(row: any) { await contractSubmit(row.id); ElMessage.success('已提交'); load() }
async function approve(row: any) { await contractApprove(row.id); ElMessage.success('审批通过'); load() }
async function unapprove(row: any) {
  await ElMessageBox.confirm('确定反审核该合同吗？', '提示', { type: 'warning' })
  await contractUnapprove(row.id); ElMessage.success('已反审核'); load()
}
async function convert(row: any) {
  await ElMessageBox.confirm('确定将该合同转为销售订单吗？', '提示', { type: 'warning' })
  const res = await contractConvert(row.id)
  ElMessage.success('已生成销售订单：' + (res as any).data?.orderNo || '')
  load()
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printTotal = ref<any>(0)
const printCols = [
  { label: '序号', prop: '_idx', width: '40px', align: 'center', formatter: (_r: any, i: number) => i + 1 },
  { label: '产品编号', prop: 'productCode' },
  { label: '产品名称', prop: 'productName' },
  { label: '规格', prop: 'spec' },
  { label: '单位', prop: 'unit', align: 'center' },
  { label: '数量', prop: 'quantity', align: 'right' },
  { label: '含税单价', prop: 'price', align: 'right' },
  { label: '税率', prop: 'taxRate', align: 'right', formatter: (r: any) => (r.taxRate ?? '') + '%' },
  { label: '价税合计', prop: 'totalAmount', align: 'right' }
]

async function handlePrint(row: any) {
  printRows.value = await contractItems(row.id)
  printHead.value = [
    [{ label: '合同号', value: row.contractNo }, { label: '签订日期', value: row.contractDate }],
    [{ label: '客户', value: row.customerName }, { label: '业务员', value: row.salesman }],
    [{ label: '付款条款', value: row.paymentTerms }]
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
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
