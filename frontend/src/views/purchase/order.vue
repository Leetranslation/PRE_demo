<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="采购单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="审批状态">
        <el-select v-model="query.approveStatus" placeholder="全部" clearable style="width: 140px">
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
      <el-button type="primary" @click="openForm()">新增采购单</el-button>
      <el-button @click="doExport">导出</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="orderNo" label="采购单号" width="200" />
      <el-table-column prop="supplierName" label="供应商" min-width="160" />
      <el-table-column prop="materialCode" label="物料编号" width="150" show-overflow-tooltip />
      <el-table-column prop="materialName" label="物料名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="orderDate" label="订单日期" width="140" />
      <el-table-column label="审批状态" width="120">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="业务状态" width="120">
        <template #default="{ row }">{{ bizText(row.bizStatus) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="140" align="right" />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="400" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="handleSubmit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="handleApprove(row)">审批通过</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="success" @click="openInbound(row)">入库</el-button>
          <el-button v-if="row.approveStatus === 'approved' && row.bizStatus === 'pending_inbound'" link type="warning" @click="handleUnapprove(row)">反审核</el-button>
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

  <!-- 新增采购单：整页单据表单 -->
  <DocForm
    v-else
    title="新建采购订单"
    :model-value="form"
    :items="form.items"
    :fields="docFields"
    :columns="docColumns"
    @update:items="(v) => (form.items = v)"
    @add="addItem"
    @cancel="closeForm"
    @save="handleSave(false)"
    @submit="handleSave(true)"
  >
    <template #field-supplierId>
      <el-select
        v-model="form.supplierId"
        filterable
        remote
        :remote-method="searchSupplier"
        :loading="supplierLoading"
        placeholder="搜索供应商"
        clearable
        size="small"
        style="width: 100%"
      >
        <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
    </template>

    <template #col-material="{ row }">
      <div class="material-cell">
        <template v-if="row.materialId">
          <span class="material-name">{{ row.materialCode }} {{ row.materialName }}</span>
          <span class="material-spec">{{ row.spec }}</span>
        </template>
        <span v-else class="material-empty">未选择物料</span>
      </div>
      <el-button link size="small" type="primary" @click="openMaterialPicker(row)">
        {{ row.materialId ? '更换' : '选择物料' }}
      </el-button>
    </template>

    <template #col-total="{ row }">{{ calcTotal(row) }}</template>

    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
      <span>价税合计：￥{{ sumAmount }}</span>
    </template>
  </DocForm>

  <!-- 明细查看 -->
  <el-dialog v-model="itemsVisible" title="采购明细" width="760px">
    <el-table :data="items" border size="small">
      <el-table-column label="物料" min-width="220">
        <template #default="{ row }">{{ row.materialCode }} {{ row.materialName }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="120" />
      <el-table-column prop="price" label="含税单价" width="120" />
      <el-table-column prop="amountExTax" label="不含税金额" width="120" />
      <el-table-column prop="taxAmount" label="税额" width="100" />
      <el-table-column prop="totalAmount" label="价税合计" width="120" />
      <el-table-column prop="inboundQty" label="已入库" width="100" />
    </el-table>
  </el-dialog>

  <!-- 入库 -->
  <el-dialog v-model="inboundVisible" title="采购入库" width="620px">
    <el-form :model="inboundForm" label-width="90px">
      <el-form-item label="仓库" required>
        <el-select v-model="inboundForm.warehouseId" filterable remote :remote-method="searchWarehouse" :loading="warehouseLoading" placeholder="搜索仓库" clearable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <div class="item-toolbar"><span>入库明细</span><el-button size="small" type="primary" @click="addInboundItem">添加</el-button></div>
    <el-table :data="inboundForm.items" border size="small">
      <el-table-column label="物料" min-width="180">
        <template #default="{ row }">
          <div class="picker-cell">
            <span v-if="row.materialId" class="picker-name">{{ row.materialName }}</span>
            <span v-else class="picker-empty">未选择物料</span>
          </div>
          <el-button link size="small" type="primary" @click="openMaterialPicker(row)">
            {{ row.materialId ? '更换' : '选择物料' }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="130"><template #default="{ row }"><el-input v-model="row.quantity" size="small" placeholder="数量" /></template></el-table-column>
      <el-table-column label="批次号" width="140"><template #default="{ row }"><el-input v-model="row.batchNo" size="small" /></template></el-table-column>
      <el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="inboundForm.items.splice($index,1)">删除</el-button></template></el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="inboundVisible = false">取消</el-button>
      <el-button type="primary" @click="handleInbound">确认入库</el-button>
    </template>
  </el-dialog>

  <!-- 物料选择弹窗 -->
  <MaterialPicker v-model="pickerVisible" @select="onMaterialSelected" />

  <PrintDialog
    v-model="printVisible"
    title="采购订单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    total-label="合计金额（含税）"
    :total-value="printTotal"
    :remark="printRemark"
    :show-upper="true"
    :total-amount="printTotal"
    :sign-labels="['制单人', '审核人', '供应商']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseOrder' })
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  purchaseOrderList, purchaseOrderItems, purchaseOrderCreate,
  purchaseOrderSubmit, purchaseOrderApprove, purchaseOrderUnapprove, purchaseInboundCreate
} from '@/api/purchase'
import { supplierList, warehouseList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'
import DocForm from '@/components/DocForm.vue'
import type { DocColumn, DocField } from '@/components/docForm'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
/** 整页表单是否展开（true = 隐藏列表、显示新建页） */
const formVisible = ref(false)
const itemsVisible = ref(false)
const items = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({ items: [] })

// 供应商下拉
const suppliers = ref<any[]>([])
const supplierLoading = ref(false)
// 物料选择
const pickerVisible = ref(false)
const currentItemRow = ref<any>(null)
// 入库
const inboundVisible = ref(false)
const inboundForm = reactive<any>({ orderId: undefined, warehouseId: undefined, items: [] })
const warehouses = ref<any[]>([])
const warehouseLoading = ref(false)

// 导出
const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'orderNo', label: '采购单号' },
    { prop: 'supplierName', label: '供应商' },
    { prop: 'materialCode', label: '物料编号' },
    { prop: 'materialName', label: '物料名称' },
    { prop: 'orderDate', label: '订单日期' },
    { prop: 'approveStatus', label: '审批状态', formatter: (row: any) => approveText(row.approveStatus) },
    { prop: 'bizStatus', label: '业务状态', formatter: (row: any) => bizText(row.bizStatus) },
    { prop: 'totalAmount', label: '金额' },
    { prop: 'remark', label: '备注' }
  ],
  '采购订单'
)

function doExport() {
  handleExport(records.value)
}

async function searchSupplier(keyword: string) {
  supplierLoading.value = true
  try {
    const res = await supplierList({ page: 1, pageSize: 20, keyword, status: 1 })
    suppliers.value = res.records
  } finally {
    supplierLoading.value = false
  }
}

function openMaterialPicker(row: any) {
  currentItemRow.value = row
  pickerVisible.value = true
}

function onMaterialSelected(material: any) {
  if (currentItemRow.value) {
    currentItemRow.value.materialId = material.id
    currentItemRow.value.materialCode = material.code
    currentItemRow.value.materialName = material.name
    currentItemRow.value.spec = material.spec
    currentItemRow.value.unit = material.unit
  }
}

async function searchWarehouse(keyword: string) {
  warehouseLoading.value = true
  try {
    const res = await warehouseList({ page: 1, pageSize: 20, keyword, status: 1 })
    warehouses.value = res.records
  } finally {
    warehouseLoading.value = false
  }
}

function openInbound(row: any) {
  inboundForm.orderId = row.id
  inboundForm.warehouseId = undefined
  inboundForm.items = []
  searchWarehouse('')
  inboundVisible.value = true
}

function addInboundItem() {
  inboundForm.items.push({ materialId: undefined, materialName: '', quantity: 0, batchNo: '' })
}

async function handleInbound() {
  if (!inboundForm.warehouseId || !inboundForm.items.length) {
    ElMessage.warning('请选择仓库并添加明细')
    return
  }
  for (const item of inboundForm.items) {
    if (!item.materialId) { ElMessage.warning('存在未选择物料的明细行'); return }
    const qty = Number(item.quantity)
    if (!qty || qty <= 0) { ElMessage.warning('存在数量未填或为0的明细行'); return }
    item.quantity = qty
  }
  inboundForm.requestId = crypto.randomUUID()
  await purchaseInboundCreate(inboundForm)
  ElMessage.success('入库成功')
  inboundVisible.value = false
  load()
}

function approveText(v: string) {
  return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v
}
function approveTagType(v: string) {
  return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info'
}
function bizText(v: string) {
  return { pending_inbound: '待入库', partial_inbound: '部分入库', completed: '已完成' }[v] || v
}
function calcTotal(row: any) {
  return ((row.quantity || 0) * (row.price || 0)).toFixed(2)
}

async function load() {
  loading.value = true
  try {
    const res = await purchaseOrderList(query)
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
  form.purchaseType = 'stock'
  searchSupplier('')
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
}

/** 基本信息字段（5 列网格） */
const docFields: DocField[] = [
  { key: 'supplierId', label: '供应商', type: 'slot', required: true },
  {
    key: 'purchaseType',
    label: '采购类型',
    type: 'select',
    options: [
      { label: '备货采购', value: 'stock' },
      { label: '生产采购', value: 'production' },
      { label: 'MRP采购', value: 'mrp' }
    ]
  },
  { key: 'expectedDate', label: '预计到货', type: 'date' },
  { key: 'buyer', label: '采购员' },
  { key: 'remark', label: '备注', span: 2 }
]

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'material', label: '物料名称', width: '260' },
  { key: 'quantity', label: '数量', width: '110', type: 'input', align: 'right', placeholder: '数量' },
  { key: 'price', label: '含税单价', width: '120', type: 'input', align: 'right', placeholder: '单价' },
  { key: 'taxRate', label: '税率%', width: '90', type: 'input', align: 'right', placeholder: '税率' },
  { key: 'unit', label: '基本单位', width: '90' },
  { key: 'total', label: '价税合计', width: '120', align: 'right' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)
const sumAmount = computed(() =>
  form.items
    .reduce((s: number, r: any) => s + (Number(r.quantity) || 0) * (Number(r.price) || 0), 0)
    .toFixed(2)
)

function addItem() {
  form.items.push({ materialId: undefined, materialName: '', spec: '', quantity: 0, price: 0, taxRate: 13, unit: '' })
}

async function handleSave(submitAfter = false) {
  if (!form.supplierId) {
    ElMessage.warning('请选择供应商')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请至少添加一条明细')
    return
  }
  for (const item of form.items) {
    if (!item.materialId) {
      ElMessage.warning('存在未选择物料的明细行')
      return
    }
    const qty = Number(item.quantity)
    if (!qty || qty <= 0) {
      ElMessage.warning('存在数量未填或为0的明细行')
      return
    }
    // 统一转为数字类型，保证后端接收正确
    item.quantity = qty
    item.price = Number(item.price) || 0
    item.taxRate = Number(item.taxRate) || 0
  }
  const created: any = await purchaseOrderCreate(form)
  if (submitAfter && created?.id) {
    await purchaseOrderSubmit(created.id)
    ElMessage.success('已保存并提交')
  } else {
    ElMessage.success('保存成功')
  }
  formVisible.value = false
  load()
}

async function handleSubmit(row: any) {
  await ElMessageBox.confirm('确定提交审批吗？', '提示', { type: 'warning' })
  await purchaseOrderSubmit(row.id)
  ElMessage.success('已提交')
  load()
}

async function handleApprove(row: any) {
  await ElMessageBox.confirm('确定审批通过吗？', '提示', { type: 'warning' })
  await purchaseOrderApprove(row.id)
  ElMessage.success('已审批')
  load()
}

async function handleUnapprove(row: any) {
  await ElMessageBox.confirm('确定反审核吗？', '提示', { type: 'warning' })
  await purchaseOrderUnapprove(row.id)
  ElMessage.success('已反审核')
  load()
}

async function viewItems(row: any) {
  items.value = await purchaseOrderItems(row.id)
  itemsVisible.value = true
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printTotal = ref<any>(0)
const printCols = [
  { label: '序号', prop: '_idx', width: '40px', align: 'center', formatter: (_r: any, i: number) => i + 1 },
  { label: '物料编号', prop: 'materialCode' },
  { label: '物料名称', prop: 'materialName' },
  { label: '规格', prop: 'spec' },
  { label: '单位', prop: 'unit', align: 'center' },
  { label: '数量', prop: 'quantity', align: 'right' },
  { label: '含税单价', prop: 'price', align: 'right' },
  { label: '税率', prop: 'taxRate', align: 'right', formatter: (r: any) => (r.taxRate ?? '') + '%' },
  { label: '价税合计', prop: 'totalAmount', align: 'right' }
]

async function handlePrint(row: any) {
  printRows.value = await purchaseOrderItems(row.id)
  printHead.value = [
    [{ label: '采购单号', value: row.orderNo }, { label: '订单日期', value: row.orderDate }],
    [{ label: '供应商', value: row.supplierName }, { label: '采购员', value: row.buyer }],
    [{ label: '预计到货', value: row.expectedDate }]
  ]
  printRemark.value = row.remark
  printTotal.value = row.totalAmount
  printVisible.value = true
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.item-toolbar { display: flex; justify-content: space-between; align-items: center; margin: 8px 0; }
.material-cell { display: flex; flex-direction: column; margin-bottom: 2px; }
.material-name { font-weight: 500; color: #333; }
.material-spec { font-size: 12px; color: #999; }
.material-empty { font-size: 12px; color: #c0c4cc; }
.picker-cell { display: flex; flex-direction: column; margin-bottom: 2px; }
.picker-name { font-weight: 500; color: #333; }
.picker-empty { font-size: 12px; color: #c0c4cc; }
</style>
