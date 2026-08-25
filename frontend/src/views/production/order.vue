<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="工单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
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
      <el-button type="primary" @click="openCreate">新增工单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="工单号" width="170" />
      <el-table-column prop="productId" label="产品ID" width="90" />
      <el-table-column prop="quantity" label="生产数量" width="110" align="right" />
      <el-table-column prop="completedQty" label="已完成" width="110" align="right" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="业务状态" width="100">
        <template #default="{ row }">{{ bizText(row.bizStatus) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="340" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="handleSubmit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="handleApprove(row)">审批通过</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="primary" @click="openIssue(row)">领料</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="warning" @click="openReturn(row)">退料</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="success" @click="openInbound(row)">入库</el-button>
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

  <!-- 新增工单 -->
  <el-dialog v-model="createVisible" title="新增生产工单" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="产品ID" required><el-input-number v-model="form.productId" :min="1" style="width: 100%" /></el-form-item>
      <el-form-item label="生产数量" required><el-input-number v-model="form.quantity" :min="1" :precision="6" style="width: 100%" /></el-form-item>
      <el-form-item label="生产类型">
        <el-select v-model="form.productionType" style="width: 100%">
          <el-option label="订单生产" value="order" />
          <el-option label="库存生产" value="stock" />
          <el-option label="MRP生产" value="mrp" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划生产日期"><el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      <el-form-item label="负责人"><el-input v-model="form.owner" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="createVisible = false">取消</el-button>
      <el-button type="primary" @click="handleCreate">保存</el-button>
    </template>
  </el-dialog>

  <!-- 领料 -->
  <el-dialog v-model="issueVisible" title="生产领料" width="620px">
    <el-form :model="issueForm" label-width="90px">
      <el-form-item label="仓库ID" required><el-input-number v-model="issueForm.warehouseId" :min="1" style="width: 100%" /></el-form-item>
    </el-form>
    <div class="item-toolbar"><span>领料明细</span><el-button size="small" type="primary" @click="addIssueItem">添加</el-button></div>
    <el-table :data="issueForm.items" border size="small">
      <el-table-column label="物料ID" width="120"><template #default="{ row }"><el-input-number v-model="row.materialId" :min="1" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="数量" width="140"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="批次号" width="140"><template #default="{ row }"><el-input v-model="row.batchNo" size="small" /></template></el-table-column>
      <el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="issueForm.items.splice($index,1)">删除</el-button></template></el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="issueVisible = false">取消</el-button>
      <el-button type="primary" @click="handleIssue">确认领料</el-button>
    </template>
  </el-dialog>

  <!-- 退料 -->
  <el-dialog v-model="returnVisible" title="生产退料" width="620px">
    <el-form :model="returnForm" label-width="90px">
      <el-form-item label="仓库ID" required><el-input-number v-model="returnForm.warehouseId" :min="1" style="width: 100%" /></el-form-item>
    </el-form>
    <div class="item-toolbar"><span>退料明细</span><el-button size="small" type="primary" @click="addReturnItem">添加</el-button></div>
    <el-table :data="returnForm.items" border size="small">
      <el-table-column label="物料ID" width="120"><template #default="{ row }"><el-input-number v-model="row.materialId" :min="1" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="数量" width="140"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="returnForm.items.splice($index,1)">删除</el-button></template></el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="returnVisible = false">取消</el-button>
      <el-button type="primary" @click="handleReturn">确认退料</el-button>
    </template>
  </el-dialog>

  <!-- 入库 -->
  <el-dialog v-model="inboundVisible" title="生产入库" width="620px">
    <el-form :model="inboundForm" label-width="90px">
      <el-form-item label="仓库ID" required><el-input-number v-model="inboundForm.warehouseId" :min="1" style="width: 100%" /></el-form-item>
    </el-form>
    <div class="item-toolbar"><span>入库明细</span><el-button size="small" type="primary" @click="addInboundItem">添加</el-button></div>
    <el-table :data="inboundForm.items" border size="small">
      <el-table-column label="产品ID" width="120"><template #default="{ row }"><el-input-number v-model="row.productId" :min="1" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="数量" width="140"><template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :precision="6" size="small" style="width:100%" /></template></el-table-column>
      <el-table-column label="操作" width="70"><template #default="{ $index }"><el-button link type="danger" @click="inboundForm.items.splice($index,1)">删除</el-button></template></el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="inboundVisible = false">取消</el-button>
      <el-button type="primary" @click="handleInbound">确认入库</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  productionOrderList, productionOrderCreate, productionOrderSubmit,
  productionOrderApprove, materialIssueCreate, materialReturnCreate, productionInboundCreate
} from '@/api/production'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })

const createVisible = ref(false)
const issueVisible = ref(false)
const returnVisible = ref(false)
const inboundVisible = ref(false)

const form = reactive<any>({})
const issueForm = reactive<any>({ orderId: undefined, warehouseId: undefined, items: [] })
const returnForm = reactive<any>({ orderId: undefined, warehouseId: undefined, items: [] })
const inboundForm = reactive<any>({ orderId: undefined, warehouseId: undefined, items: [] })

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }
function bizText(v: string) { return { pending: '待生产', producing: '生产中', partial: '部分完成', completed: '已完成' }[v] || v }

async function load() {
  loading.value = true
  try {
    const res = await productionOrderList(query)
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
  form.productionType = 'stock'
  createVisible.value = true
}
async function handleCreate() {
  if (!form.productId || !form.quantity) { ElMessage.warning('请填写产品和数量'); return }
  await productionOrderCreate(form)
  ElMessage.success('保存成功')
  createVisible.value = false
  load()
}
async function handleSubmit(row: any) {
  await ElMessageBox.confirm('确定提交审批吗？', '提示', { type: 'warning' })
  await productionOrderSubmit(row.id)
  ElMessage.success('已提交')
  load()
}
async function handleApprove(row: any) {
  await ElMessageBox.confirm('确定审批通过吗？', '提示', { type: 'warning' })
  await productionOrderApprove(row.id)
  ElMessage.success('已审批')
  load()
}

function openIssue(row: any) {
  issueForm.orderId = row.id
  issueForm.warehouseId = 1
  issueForm.items = []
  issueVisible.value = true
}
function addIssueItem() { issueForm.items.push({ materialId: undefined, quantity: 0, batchNo: '' }) }
async function handleIssue() {
  if (!issueForm.warehouseId || !issueForm.items.length) { ElMessage.warning('请填写仓库和明细'); return }
  await materialIssueCreate(issueForm)
  ElMessage.success('领料成功')
  issueVisible.value = false
  load()
}

function openReturn(row: any) {
  returnForm.orderId = row.id
  returnForm.warehouseId = 1
  returnForm.items = []
  returnVisible.value = true
}
function addReturnItem() { returnForm.items.push({ materialId: undefined, quantity: 0 }) }
async function handleReturn() {
  if (!returnForm.warehouseId || !returnForm.items.length) { ElMessage.warning('请填写仓库和明细'); return }
  await materialReturnCreate(returnForm)
  ElMessage.success('退料成功')
  returnVisible.value = false
  load()
}

function openInbound(row: any) {
  inboundForm.orderId = row.id
  inboundForm.warehouseId = 1
  inboundForm.items = []
  inboundVisible.value = true
}
function addInboundItem() { inboundForm.items.push({ productId: undefined, quantity: 0 }) }
async function handleInbound() {
  if (!inboundForm.warehouseId || !inboundForm.items.length) { ElMessage.warning('请填写仓库和明细'); return }
  await productionInboundCreate(inboundForm)
  ElMessage.success('入库成功')
  inboundVisible.value = false
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.item-toolbar { display: flex; justify-content: space-between; align-items: center; margin: 8px 0; }
</style>
