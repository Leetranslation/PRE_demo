<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="装箱单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openForm()">新增装箱单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="packingNo" label="装箱单号" width="160" />
      <el-table-column prop="outboundNo" label="出库单号" width="160" />
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

  <!-- 新增装箱单：整页单据表单（无审批流） -->
  <DocForm
    v-else
    title="新建装箱单"
    :model-value="form"
    :items="form.items"
    :fields="docFields"
    :columns="docColumns"
    hide-submit
    @update:items="(v) => (form.items = v)"
    @add="addRow"
    @cancel="closeForm"
    @save="handleSave"
  >
    <template #col-product="{ row }">
      <el-button size="small" @click="pickProduct(row)">{{ row.productName || '选产品' }}</el-button>
    </template>
    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
    </template>
  </DocForm>

  <ProductPicker v-model="productPickerVisible" @select="onProductSelect" />

  <!-- 装箱明细：真弹明细，不再只弹一个单号 -->
  <el-dialog v-model="detailVisible" :title="'装箱明细 ' + detailNo" width="640px">
    <el-table :data="detailItems" border size="small">
      <el-table-column type="index" label="序号" width="56" align="center" />
      <el-table-column prop="boxNo" label="箱号" width="130" />
      <el-table-column prop="productCode" label="产品编号" width="140" />
      <el-table-column prop="productName" label="产品名称" min-width="160" />
      <el-table-column prop="quantity" label="数量" width="100" align="right" />
      <el-table-column prop="remark" label="备注" min-width="120" />
    </el-table>
  </el-dialog>

  <PrintDialog
    v-model="printVisible"
    title="装箱单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['装箱人', '复核人', '客户签收']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Packing' })
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { packingList, packingCreate, packingItems } from '@/api/sales'
import ProductPicker from '@/components/ProductPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'
import DocForm from '@/components/DocForm.vue'
import type { DocColumn, DocField } from '@/components/docForm'
import request from '@/utils/request'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const productPickerVisible = ref(false)
const currentRow = ref<any>(null)
const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const form = reactive<any>({ items: [] })

function addRow() {
  form.items.push({ boxNo: '', productId: null, productName: '', quantity: 1 })
}
function pickProduct(row: any) {
  currentRow.value = row
  productPickerVisible.value = true
}
function onProductSelect(product: any) {
  if (currentRow.value) {
    currentRow.value.productId = product.id
    currentRow.value.productName = `${product.code} ${product.name}`
  }
}

async function load() {
  loading.value = true
  try {
    const res = await packingList(query)
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
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

/** 基本信息字段 */
const docFields: DocField[] = [{ key: 'remark', label: '备注', span: 3 }]

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'boxNo', label: '箱号', width: '160', type: 'input' },
  { key: 'product', label: '产品', width: '260' },
  { key: 'quantity', label: '数量', width: '120', type: 'input', align: 'right' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)

async function handleSave() {
  if (!form.items || form.items.length === 0) { ElMessage.warning('请添加装箱明细'); return }
  await packingCreate({
    remark: form.remark,
    items: form.items.map((i: any) => ({ boxNo: i.boxNo, productId: i.productId, quantity: Number(i.quantity) }))
  })
  ElMessage.success('保存成功')
  formVisible.value = false
  load()
}

const detailVisible = ref(false)
const detailItems = ref<any[]>([])
const detailNo = ref('')

async function viewItems(row: any) {
  detailNo.value = row.packingNo
  detailItems.value = await packingItems(row.id)
  detailVisible.value = true
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printCols = [
  { label: '序号', prop: '_idx', width: '40px', align: 'center', formatter: (_r: any, i: number) => i + 1 },
  { label: '箱号', prop: 'boxNo' },
  { label: '产品', prop: 'productName' },
  { label: '数量', prop: 'quantity', align: 'right' }
]

async function handlePrint(row: any) {
  const res: any = await request.get('/sales/packing/items', { params: { packingId: row.id } })
  printRows.value = res || []
  printHead.value = [
    [{ label: '装箱单号', value: row.packingNo }, { label: '出库单号', value: row.outboundNo }]
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
