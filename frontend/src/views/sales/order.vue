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
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="销售单号" width="170" />
      <el-table-column prop="customerId" label="客户ID" width="90" />
      <el-table-column prop="orderDate" label="订单日期" width="120" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="业务状态" width="100">
        <template #default="{ row }">{{ bizText(row.bizStatus) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="120" align="right" />
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="handleSubmit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="handleApprove(row)">审批通过</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="primary" @click="openOutbound(row)">出库</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="warning" @click="openReturn(row)">退货</el-button>
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
        <el-col :span="12"><el-form-item label="客户ID" required><el-input-number v-model="form.customerId" :min="1" style="width: 100%" /></el-form-item></el-col>
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
      <el-table-column label="产品ID" width="110"><template #default="{ row }"><el-input-number v-model="row.productId" :min="1" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="数量" width="120"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="含税单价" width="130"><template #default="{ row }"><el-input-number v-model="row.price" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="税率%" width="100"><template #default="{ row }"><el-input-number v-model="row.taxRate" :min="0" :precision="2" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="单位" width="90"><template #default="{ row }"><el-input v-model="row.unit" size="small" /></template></el-table-column>
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
      <el-form-item label="仓库ID" required><el-input-number v-model="outboundForm.warehouseId" :min="1" style="width: 100%" /></el-form-item>
    </el-form>
    <div class="item-toolbar"><span>出库明细</span><el-button size="small" type="primary" @click="addOutboundItem">添加</el-button></div>
    <el-table :data="outboundForm.items" border size="small">
      <el-table-column label="明细ID" width="110"><template #default="{ row }"><el-input-number v-model="row.orderItemId" :min="1" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="产品ID" width="110"><template #default="{ row }"><el-input-number v-model="row.productId" :min="1" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="数量" width="130"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
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
      <el-form-item label="仓库ID" required><el-input-number v-model="returnForm.warehouseId" :min="1" style="width: 100%" /></el-form-item>
      <el-form-item label="产品ID" required><el-input-number v-model="returnForm.productId" :min="1" style="width: 100%" /></el-form-item>
      <el-form-item label="退货数量" required><el-input-number v-model="returnForm.quantity" :min="0" :precision="6" style="width: 100%" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="returnVisible = false">取消</el-button>
      <el-button type="primary" @click="handleReturn">确认退货</el-button>
    </template>
  </el-dialog>

  <!-- 明细查看 -->
  <el-dialog v-model="itemsVisible" title="销售明细" width="720px">
    <el-table :data="items" border size="small">
      <el-table-column prop="productId" label="产品ID" width="100" />
      <el-table-column prop="quantity" label="数量" width="110" />
      <el-table-column prop="price" label="含税单价" width="110" />
      <el-table-column prop="amountExTax" label="不含税金额" width="120" />
      <el-table-column prop="taxAmount" label="税额" width="100" />
      <el-table-column prop="totalAmount" label="价税合计" width="120" />
      <el-table-column prop="outboundQty" label="已出库" width="100" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  salesOrderList, salesOrderItems, salesOrderCreate, salesOrderSubmit,
  salesOrderApprove, salesOutboundCreate, salesReturnCreate
} from '@/api/sales'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })

const createVisible = ref(false)
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
  createVisible.value = true
}
function addItem() { form.items.push({ productId: undefined, quantity: 0, price: 0, taxRate: 13, unit: '' }) }
async function handleCreate() {
  if (!form.customerId || !form.items.length) { ElMessage.warning('请填写客户和明细'); return }
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

function openOutbound(row: any) {
  outboundForm.orderId = row.id
  outboundForm.warehouseId = 1
  outboundForm.items = []
  outboundVisible.value = true
}
function addOutboundItem() { outboundForm.items.push({ orderItemId: undefined, productId: undefined, quantity: 0 }) }
async function handleOutbound() {
  if (!outboundForm.warehouseId || !outboundForm.items.length) { ElMessage.warning('请填写仓库和明细'); return }
  await salesOutboundCreate(outboundForm)
  ElMessage.success('出库成功')
  outboundVisible.value = false
  load()
}

function openReturn(row: any) {
  returnForm.orderId = row.id
  returnForm.warehouseId = 1
  returnForm.productId = undefined
  returnForm.quantity = 0
  returnVisible.value = true
}
async function handleReturn() {
  if (!returnForm.warehouseId || !returnForm.productId || !returnForm.quantity) { ElMessage.warning('请填写完整'); return }
  await salesReturnCreate(returnForm)
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
</style>
