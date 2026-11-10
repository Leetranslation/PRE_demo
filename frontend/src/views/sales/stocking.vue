<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="备货单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openForm()">新增备货单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="stockingNo" label="备货单号" width="160" />
      <el-table-column prop="sourceOrderNo" label="销售订单号" width="160" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)" size="small">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="submit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="approve(row)">审批</el-button>
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

  <!-- 新增备货单：整页单据表单 -->
  <DocForm
    v-else
    title="新建备货单"
    :model-value="form"
    :items="form.items"
    :fields="docFields"
    :columns="docColumns"
    :pick-labels="pickLabels"
    @pick="onPick"
    hide-add
    @update:items="(v) => (form.items = v)"
    @cancel="closeForm"
    @save="handleSave(false)"
    @submit="handleSave(true)"
  >
    <template #tools>
      <el-button type="primary" size="small" @click="productPickerVisible = true">添加产品</el-button>
    </template>
    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
    </template>
  </DocForm>

  <DocPicker
    v-model="orderPickerVisible"
    title="选择销售订单"
    keyword-placeholder="销售单号 / 客户 / 产品"
    :columns="SALES_ORDER_COLS"
    :fetcher="(p) => salesOrderList(p as any)"
    @select="applyOrder"
  />

  <ProductPicker v-model="productPickerVisible" @select="onProductSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'SalesStocking' })
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { salesStockingList, salesStockingCreate, salesStockingSubmit, salesStockingApprove, salesOrderList } from '@/api/sales'
import ProductPicker from '@/components/ProductPicker.vue'
import DocForm from '@/components/DocForm.vue'
import DocPicker from '@/components/DocPicker.vue'
import { SALES_ORDER_COLS } from '@/components/docPickerPresets'
import type { DocColumn, DocField } from '@/components/docForm'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const orderPickerVisible = ref(false)
const pickedOrder = ref<any>(null)
const productPickerVisible = ref(false)
const orders = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({ items: [] })

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }

async function loadOrders() {
  const res = await salesOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}

function onProductSelect(product: any) {
  form.items.push({ productId: product.id, productName: `${product.code} ${product.name}`, quantity: 1 })
}

async function load() {
  loading.value = true
  try {
    const res = await salesStockingList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.page = 1; load() }

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  pickedOrder.value = null
  loadOrders()
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

const pickLabels = computed<Record<string, string>>(() => ({
  orderId: pickedOrder.value
    ? `${pickedOrder.value.orderNo}  ${pickedOrder.value.customerName || ''}  ￥${pickedOrder.value.totalAmount ?? ''}`
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
  { key: 'orderId', label: '销售订单', type: 'pick', placeholder: '可选，不选则按纯备货' },
  { key: 'remark', label: '备注', span: 2 }
])

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'productName', label: '产品', width: '260' },
  { key: 'quantity', label: '数量', width: '120', type: 'input', align: 'right' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)

async function handleSave(submitAfter = false) {
  if (!form.items || form.items.length === 0) { ElMessage.warning('请添加备货明细'); return }
  const created: any = await salesStockingCreate({
    orderId: form.orderId, remark: form.remark,
    items: form.items.map((i: any) => ({ productId: i.productId, quantity: Number(i.quantity) }))
  })
  if (submitAfter && created?.id) {
    await salesStockingSubmit(created.id)
    ElMessage.success('已保存并提交')
  } else {
    ElMessage.success('保存成功')
  }
  formVisible.value = false
  load()
}

async function submit(row: any) { await salesStockingSubmit(row.id); ElMessage.success('已提交'); load() }
async function approve(row: any) { await salesStockingApprove(row.id); ElMessage.success('审批通过'); load() }

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
