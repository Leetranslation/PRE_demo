<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="订单">
        <el-select v-model="query.orderId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openForm()">新增退货</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="returnNo" label="退货单号" width="160" />
      <el-table-column prop="sourceOrderNo" label="采购订单号" width="160" />
      <el-table-column prop="returnDate" label="退货日期" width="110" />
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

  <!-- 新增采购退货：整页单据表单（无审批流，隐藏提交） -->
  <DocForm
    v-else
    title="新建采购退货（库存出库）"
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
    @save="handleSave()"
  >
    <template #tools>
      <el-button type="primary" size="small" @click="materialPickerVisible = true">添加物料</el-button>
    </template>
    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
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

  <el-dialog v-model="detailVisible" title="退货明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialName" label="物料" width="140" />
      <el-table-column prop="quantity" label="退货数量" width="110" align="right" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="materialPickerVisible" @select="onMaterialSelect" />

  <PrintDialog
    v-model="printVisible"
    title="采购退货单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['制单人', '仓管员', '供应商']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseReturn' })
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { purchaseReturnList, purchaseReturnCreate, purchaseReturnItems, purchaseOrderList } from '@/api/purchase'
import { warehouseList } from '@/api/base'
import DocForm from '@/components/DocForm.vue'
import DocPicker from '@/components/DocPicker.vue'
import { PURCHASE_ORDER_COLS } from '@/components/docPickerPresets'
import type { DocColumn, DocField } from '@/components/docForm'
import MaterialPicker from '@/components/MaterialPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const orderPickerVisible = ref(false)
const pickedOrder = ref<any>(null)
const detailVisible = ref(false)
const materialPickerVisible = ref(false)
const orders = ref<any[]>([])
const warehouses = ref<any[]>([])
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, orderId: undefined as number | undefined })
const form = reactive<any>({ items: [] })

async function loadOrders() {
  const res = await purchaseOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}
async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

function onMaterialSelect(material: any) {
  form.items.push({ materialId: material.id, materialName: `${material.code} ${material.name}`, quantity: 1 })
}

async function load() {
  loading.value = true
  try {
    const res = await purchaseReturnList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.orderId = undefined; query.page = 1; load() }

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  pickedOrder.value = null
  loadOrders()
  loadWarehouses()
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

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

/** 基本信息字段（下拉选项随数据加载刷新，所以用 computed） */
const docFields = computed<DocField[]>(() => [
  { key: 'orderId', label: '采购订单', type: 'pick', required: true },
  {
    key: 'warehouseId',
    label: '出库仓库',
    type: 'select',
    options: warehouses.value.map((w: any) => ({ label: w.name, value: w.id }))
  },
  { key: 'remark', label: '备注', span: 2 }
])

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'materialName', label: '物料', width: '260' },
  { key: 'quantity', label: '退货数量', width: '120', type: 'input', align: 'right' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)

async function handleSave() {
  if (!form.orderId || !form.items || form.items.length === 0) { ElMessage.warning('请选择订单并添加退货明细'); return }
  await purchaseReturnCreate({
    orderId: form.orderId, warehouseId: form.warehouseId, remark: form.remark,
    items: form.items.map((i: any) => ({ materialId: i.materialId, quantity: Number(i.quantity) }))
  })
  ElMessage.success('退货成功')
  formVisible.value = false
  load()
}

async function viewItems(row: any) {
  detailItems.value = await purchaseReturnItems(row.id)
  detailVisible.value = true
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printCols = [
  { label: '序号', prop: '_idx', width: '40px', align: 'center', formatter: (_r: any, i: number) => i + 1 },
  { label: '物料', prop: 'materialName' },
  { label: '退货数量', prop: 'quantity', align: 'right' }
]

async function handlePrint(row: any) {
  printRows.value = await purchaseReturnItems(row.id)
  printHead.value = [
    [{ label: '退货单号', value: row.returnNo }, { label: '退货日期', value: row.returnDate }],
    [{ label: '采购订单号', value: row.sourceOrderNo }]
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
