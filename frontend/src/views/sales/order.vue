<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="销售单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
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
      <el-button type="primary" @click="openCreate">新增销售单</el-button>
      <el-button @click="doExport">导出</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="orderNo" label="销售单号" width="170" />
      <el-table-column prop="customerName" label="客户" width="140" />
      <el-table-column prop="productCode" label="产品编号" width="150" show-overflow-tooltip />
      <el-table-column prop="productName" label="产品名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="orderDate" label="订单日期" width="120" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="业务状态" width="100">
        <template #default="{ row }">{{ bizText(row.bizStatus) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="140" align="right" />
      <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="handleSubmit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="handleApprove(row)">审批通过</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="primary" @click="openOutbound(row)">出库</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="warning" @click="openReturn(row)">退货</el-button>
          <el-button v-if="row.approveStatus === 'approved' && row.bizStatus === 'pending_outbound'" link type="warning" @click="handleUnapprove(row)">反审核</el-button>
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

  <!-- 新增销售单 -->
  <el-dialog v-model="createVisible" title="新增销售单" width="800px">
    <el-form :model="form" label-width="90px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="客户" required>
            <el-select v-model="form.customerId" filterable remote :remote-method="searchCustomer" :loading="customerLoading" placeholder="搜索客户" clearable style="width: 100%">
              <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12"><el-form-item label="销售类型">
          <el-select v-model="form.salesType" style="width: 100%">
            <el-option label="普通销售" value="normal" />
          </el-select>
        </el-form-item></el-col>
        <el-col :span="12"><el-form-item label="交货日期"><el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="业务员"><el-input v-model="form.salesman" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <div class="item-toolbar"><span>销售明细</span><el-button size="small" type="primary" @click="addItem">添加明细</el-button></div>
    <el-table :data="form.items" border size="small">
      <el-table-column label="产品" min-width="200">
        <template #default="{ row }">
          <div class="picker-cell">
            <template v-if="row.productId">
              <span class="picker-name">{{ row.productCode }} {{ row.productName }}</span>
              <span class="picker-spec">{{ row.spec }}</span>
            </template>
            <span v-else class="picker-empty">未选择产品</span>
          </div>
          <el-button link size="small" type="primary" @click="openProductPicker(row)">
            {{ row.productId ? '更换' : '选择产品' }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="120"><template #default="{ row }"><el-input v-model="row.quantity" size="small" placeholder="数量" /></template></el-table-column>
      <el-table-column label="含税单价" width="130"><template #default="{ row }"><el-input v-model="row.price" size="small" placeholder="单价" /></template></el-table-column>
      <el-table-column label="税率%" width="100"><template #default="{ row }"><el-input v-model="row.taxRate" size="small" placeholder="税率" /></template></el-table-column>
      <el-table-column label="单位" width="90"><template #default="{ row }">{{ row.unit }}</template></el-table-column>
      <el-table-column label="价税合计" width="110" align="right"><template #default="{ row }">{{ ((row.quantity||0)*(row.price||0)).toFixed(2) }}</template></el-table-column>
      <el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="form.items.splice($index,1)">删除</el-button></template></el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="handleCreate">保存</el-button>
    </template>
  </el-dialog>

  <!-- 出库 -->
  <el-dialog v-model="outboundVisible" title="销售出库" width="600px">
    <el-form :model="outboundForm" label-width="90px">
      <el-form-item label="仓库" required>
        <el-select v-model="outboundForm.warehouseId" filterable remote :remote-method="searchWarehouse" :loading="warehouseLoading" placeholder="搜索仓库" clearable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <div class="item-toolbar"><span>出库明细</span><el-button size="small" type="primary" @click="addOutboundItem">添加</el-button></div>
    <el-table :data="outboundForm.items" border size="small">
      <el-table-column label="产品" min-width="180">
        <template #default="{ row }">
          <div class="picker-cell">
            <span v-if="row.productId" class="picker-name">{{ row.productName }}</span>
            <span v-else class="picker-empty">未选择产品</span>
          </div>
          <el-button link size="small" type="primary" @click="openProductPicker(row)">
            {{ row.productId ? '更换' : '选择产品' }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="130"><template #default="{ row }"><el-input v-model="row.quantity" size="small" placeholder="数量" /></template></el-table-column>
      <el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="outboundForm.items.splice($index,1)">删除</el-button></template></el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="outboundVisible = false">取消</el-button>
      <el-button type="primary" @click="handleOutbound">确认出库</el-button>
    </template>
  </el-dialog>

  <!-- 退货 -->
  <el-dialog v-model="returnVisible" title="销售退货" width="480px">
    <el-form :model="returnForm" label-width="90px">
      <el-form-item label="仓库" required>
        <el-select v-model="returnForm.warehouseId" filterable remote :remote-method="searchWarehouse" :loading="warehouseLoading" placeholder="搜索仓库" clearable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="产品" required>
        <el-input v-model="returnForm.productName" placeholder="点击选择产品" readonly style="width: 100%">
          <template #append>
            <el-button @click="openReturnProductPicker">选择</el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="退货数量" required><el-input v-model="returnForm.quantity" placeholder="退货数量" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="returnVisible = false">取消</el-button>
      <el-button type="primary" @click="handleReturn">确认退货</el-button>
    </template>
  </el-dialog>

  <!-- 明细查看 -->
  <el-dialog v-model="itemsVisible" title="销售明细" width="720px">
    <el-table :data="items" border size="small">
      <el-table-column label="产品" min-width="220">
        <template #default="{ row }">{{ row.productCode }} {{ row.productName }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="110" />
      <el-table-column prop="price" label="含税单价" width="110" />
      <el-table-column prop="amountExTax" label="不含税金额" width="120" />
      <el-table-column prop="taxAmount" label="税额" width="100" />
      <el-table-column prop="totalAmount" label="价税合计" width="120" />
      <el-table-column prop="outboundQty" label="已出库" width="100" />
    </el-table>
  </el-dialog>

  <!-- 产品选择弹窗 -->
  <ProductPicker v-model="productPickerVisible" @select="onProductSelected" />
</template>

<script setup lang="ts">
defineOptions({ name: 'SalesOrder' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  salesOrderList, salesOrderItems, salesOrderCreate, salesOrderSubmit,
  salesOrderApprove, salesOrderUnapprove, salesOutboundCreate, salesReturnCreate
} from '@/api/sales'
import { customerList, warehouseList } from '@/api/base'
import ProductPicker from '@/components/ProductPicker.vue'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })

const createVisible = ref(false)

// 客户下拉
const customers = ref<any[]>([])
const customerLoading = ref(false)
// 产品选择
const productPickerVisible = ref(false)
const currentProductRow = ref<any>(null)
const pickerTarget = ref<'row' | 'return'>('row')
// 仓库下拉
const warehouses = ref<any[]>([])
const warehouseLoading = ref(false)
// 导出
const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'orderNo', label: '销售单号' },
    { prop: 'customerName', label: '客户' },
    { prop: 'productCode', label: '产品编号' },
    { prop: 'productName', label: '产品名称' },
    { prop: 'orderDate', label: '订单日期' },
    { prop: 'approveStatus', label: '审批状态', formatter: (row: any) => approveText(row.approveStatus) },
    { prop: 'bizStatus', label: '业务状态', formatter: (row: any) => bizText(row.bizStatus) },
    { prop: 'totalAmount', label: '金额' },
    { prop: 'remark', label: '备注' }
  ],
  '销售订单'
)

function doExport() {
  handleExport(records.value)
}

async function searchCustomer(keyword: string) {
  customerLoading.value = true
  try {
    const res = await customerList({ page: 1, pageSize: 20, keyword, status: 1 })
    customers.value = res.records
  } finally {
    customerLoading.value = false
  }
}

function openProductPicker(row: any) {
  pickerTarget.value = 'row'
  currentProductRow.value = row
  productPickerVisible.value = true
}

function openReturnProductPicker() {
  pickerTarget.value = 'return'
  productPickerVisible.value = true
}

function onProductSelected(product: any) {
  if (pickerTarget.value === 'return') {
    returnForm.productId = product.id
    returnForm.productName = product.name
  } else if (currentProductRow.value) {
    currentProductRow.value.productId = product.id
    currentProductRow.value.productCode = product.code
    currentProductRow.value.productName = product.name
    currentProductRow.value.spec = product.spec
    currentProductRow.value.unit = product.unit
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
const outboundVisible = ref(false)
const returnVisible = ref(false)
const itemsVisible = ref(false)
const items = ref<any[]>([])

const form = reactive<any>({ items: [] })
const outboundForm = reactive<any>({ orderId: undefined, warehouseId: undefined, items: [] })
const returnForm = reactive<any>({ orderId: undefined, warehouseId: undefined, productId: undefined, quantity: 0 })

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }
function bizText(v: string) { return { pending_outbound: '待出库', partial_outbound: '部分出库', completed: '已完成' }[v] || v }

async function load() {
  loading.value = true
  try {
    const res = await salesOrderList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.approveStatus = undefined; query.page = 1; load() }

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  form.salesType = 'normal'
  searchCustomer('')
  createVisible.value = true
}
function addItem() { form.items.push({ productId: undefined, productName: '', spec: '', quantity: 0, price: 0, taxRate: 13, unit: '' }) }
async function handleCreate() {
  if (!form.customerId) { ElMessage.warning('请选择客户'); return }
  if (!form.items.length) { ElMessage.warning('请至少添加一条明细'); return }
  for (const item of form.items) {
    if (!item.productId) { ElMessage.warning('存在未选择产品的明细行'); return }
    const qty = Number(item.quantity)
    if (!qty || qty <= 0) { ElMessage.warning('存在数量未填或为0的明细行'); return }
    item.quantity = qty
    item.price = Number(item.price) || 0
    item.taxRate = Number(item.taxRate) || 0
  }
  await salesOrderCreate(form)
  ElMessage.success('保存成功')
  createVisible.value = false
  load()
}
async function handleSubmit(row: any) {
  await ElMessageBox.confirm('确定提交审批吗？', '提示', { type: 'warning' })
  await salesOrderSubmit(row.id)
  ElMessage.success('已提交')
  load()
}
async function handleApprove(row: any) {
  await ElMessageBox.confirm('确定审批通过吗？', '提示', { type: 'warning' })
  await salesOrderApprove(row.id)
  ElMessage.success('已审批')
  load()
}

async function handleUnapprove(row: any) {
  await ElMessageBox.confirm('确定反审核该订单吗？反审核后将回到待提交状态。', '提示', { type: 'warning' })
  await salesOrderUnapprove(row.id)
  ElMessage.success('已反审核')
  load()
}

function openOutbound(row: any) {
  outboundForm.orderId = row.id
  outboundForm.warehouseId = undefined
  outboundForm.items = []
  searchWarehouse('')
  outboundVisible.value = true
}
function addOutboundItem() { outboundForm.items.push({ orderItemId: undefined, productId: undefined, productName: '', quantity: 0 }) }
async function handleOutbound() {
  if (!outboundForm.warehouseId || !outboundForm.items.length) { ElMessage.warning('请填写仓库和明细'); return }
  for (const item of outboundForm.items) {
    if (!item.productId) { ElMessage.warning('存在未选择产品的明细行'); return }
    const qty = Number(item.quantity)
    if (!qty || qty <= 0) { ElMessage.warning('存在数量未填或为0的明细行'); return }
    item.quantity = qty
  }
  outboundForm.requestId = crypto.randomUUID()
  await salesOutboundCreate(outboundForm)
  ElMessage.success('出库成功')
  outboundVisible.value = false
  load()
}

function openReturn(row: any) {
  returnForm.orderId = row.id
  returnForm.warehouseId = undefined
  returnForm.productId = undefined
  returnForm.productName = ''
  returnForm.quantity = 0
  searchWarehouse('')
  returnVisible.value = true
}
async function handleReturn() {
  const qty = Number(returnForm.quantity)
  if (!returnForm.warehouseId || !returnForm.productId || !qty) { ElMessage.warning('请填写完整'); return }
  await salesReturnCreate({
    orderId: returnForm.orderId,
    warehouseId: returnForm.warehouseId,
    requestId: crypto.randomUUID(),
    items: [{ productId: returnForm.productId, quantity: qty }]
  })
  ElMessage.success('退货成功')
  returnVisible.value = false
  load()
}

async function viewItems(row: any) {
  items.value = await salesOrderItems(row.id)
  itemsVisible.value = true
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.item-toolbar { display: flex; justify-content: space-between; align-items: center; margin: 8px 0; }
.picker-cell { display: flex; flex-direction: column; margin-bottom: 2px; }
.picker-name { font-weight: 500; color: #333; }
.picker-spec { font-size: 12px; color: #999; }
.picker-empty { font-size: 12px; color: #c0c4cc; }
</style>
