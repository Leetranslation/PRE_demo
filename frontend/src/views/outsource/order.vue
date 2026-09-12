<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="类型">
        <el-select v-model="query.type" placeholder="全部" clearable style="width: 130px">
          <el-option label="整单外协" value="whole" />
          <el-option label="工序外协" value="process" />
        </el-select>
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
      <el-button type="primary" @click="openDialog()">新增外协单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="orderNo" label="外协单号" width="160" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'whole' ? 'primary' : 'warning'" size="small">
            {{ row.type === 'whole' ? '整单外协' : '工序外协' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="生产工单" width="150">
        <template #default="{ row }">{{ orderNo(row.orderId) }}</template>
      </el-table-column>
      <el-table-column label="外协供应商" min-width="140">
        <template #default="{ row }">{{ supplierName(row.supplierId) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="100" align="right" />
      <el-table-column prop="deliveryDate" label="交货日期" width="110" />
      <el-table-column label="审批状态" width="90">
        <template #default="{ row }">{{ approveText(row.approveStatus) }}</template>
      </el-table-column>
      <el-table-column label="业务状态" width="90">
        <template #default="{ row }">
          <el-tag :type="bizType(row.bizStatus)" size="small">{{ bizText(row.bizStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="doSubmit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="doApprove(row)">审批</el-button>
          <el-button v-if="row.approveStatus === 'approved' && row.bizStatus === 'pending'" link type="warning" @click="doStart(row)">开始外协</el-button>
          <el-button v-if="row.bizStatus === 'outsourcing'" link type="success" @click="doFinish(row)">完工</el-button>
          <el-button v-if="row.bizStatus !== 'finished' && row.bizStatus !== 'voided'" link type="danger" @click="doVoid(row)">作废</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑外协单' : '新增外协单'" width="680px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="类型" required>
        <el-radio-group v-model="form.type" :disabled="!!form.id">
          <el-radio-button value="whole">整单外协</el-radio-button>
          <el-radio-button value="process">工序外协</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="生产工单" required>
        <el-select v-model="form.orderId" placeholder="请选择工单" filterable style="width: 100%">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.type === 'process'" label="外协工序" required>
        <el-select v-model="form.processId" placeholder="请选择工序" filterable style="width: 100%">
          <el-option v-for="p in processes" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="外协供应商" required>
        <el-select v-model="form.supplierId" placeholder="请选择供应商" filterable style="width: 100%">
          <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="外协数量"><el-input v-model="form.quantity" type="number" style="width: 100%" /></el-form-item>
      <el-form-item label="交货日期">
        <el-date-picker v-model="form.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>

    <el-divider content-position="left">外协明细（发出物料/产品）</el-divider>
    <div class="item-toolbar">
      <el-button type="primary" size="small" @click="pickerVisible = true">添加物料</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column prop="itemName" label="物料/产品" min-width="180" />
      <el-table-column label="数量" width="130">
        <template #default="{ row }"><el-input v-model="row.quantity" type="number" size="small" /></template>
      </el-table-column>
      <el-table-column label="操作" width="70">
        <template #default="{ $index }">
          <el-button link type="danger" @click="form.items.splice($index, 1)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="detailVisible" title="外协明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="itemName" label="物料/产品" width="160" />
      <el-table-column prop="itemType" label="类型" width="100" />
      <el-table-column prop="quantity" label="数量" width="110" align="right" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'OutsourceOrder' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  outsourceOrderList, outsourceOrderItems, outsourceOrderCreate, outsourceOrderUpdate,
  outsourceOrderSubmit, outsourceOrderApprove, outsourceOrderStart, outsourceOrderFinish, outsourceOrderVoid
} from '@/api/outsource'
import { productionOrderList } from '@/api/production'
import { supplierList, processList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const orders = ref<any[]>([])
const suppliers = ref<any[]>([])
const processes = ref<any[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const pickerVisible = ref(false)
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, type: undefined as string | undefined, approveStatus: undefined as string | undefined })
const form = reactive<any>({ items: [] })

function orderNo(id: number) { return orders.value.find((o) => o.id === id)?.orderNo || `#${id}` }
function supplierName(id: number) { return suppliers.value.find((s) => s.id === id)?.name || `#${id}` }
function approveText(s: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[s] || s }
function bizText(s: string) { return { pending: '未开始', outsourcing: '外协中', finished: '已完工', voided: '已作废' }[s] || s }
function bizType(s: string) { return { pending: 'info', outsourcing: 'primary', finished: 'success', voided: 'danger' }[s] || 'info' }

function onSelect(item: any) {
  form.items.push({ itemId: item.id, itemType: 'material', itemName: `${item.code} ${item.name}`, quantity: 1 })
}

async function loadBase() {
  const [o, s, p] = await Promise.all([
    productionOrderList({ page: 1, pageSize: 200 }),
    supplierList({ page: 1, pageSize: 200, status: 1 }),
    processList({ page: 1, pageSize: 200 })
  ])
  orders.value = o.records
  suppliers.value = s.records
  processes.value = p.records
}

async function load() {
  loading.value = true
  try {
    const res = await outsourceOrderList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.type = undefined; query.approveStatus = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  if (row) { Object.assign(form, row); detailItems.value = [] }
  else form.type = 'whole'
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.orderId) { ElMessage.warning('请选择生产工单'); return }
  if (!form.supplierId) { ElMessage.warning('请选择外协供应商'); return }
  if (form.type === 'process' && !form.processId) { ElMessage.warning('工序外协必须选择工序'); return }
  const data = {
    id: form.id, type: form.type, orderId: form.orderId, processId: form.processId,
    supplierId: form.supplierId, quantity: form.quantity ? Number(form.quantity) : undefined,
    deliveryDate: form.deliveryDate, remark: form.remark,
    items: form.items.map((i: any) => ({ itemId: i.itemId, itemType: i.itemType, quantity: Number(i.quantity), unit: i.unit }))
  }
  if (form.id) await outsourceOrderUpdate(data)
  else await outsourceOrderCreate(data)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function doSubmit(row: any) { await outsourceOrderSubmit(row.id); ElMessage.success('已提交'); load() }
async function doApprove(row: any) { await outsourceOrderApprove(row.id); ElMessage.success('已审批'); load() }
async function doStart(row: any) { await outsourceOrderStart(row.id); ElMessage.success('开始外协'); load() }
async function doFinish(row: any) { await outsourceOrderFinish(row.id); ElMessage.success('外协完工'); load() }
async function doVoid(row: any) {
  await ElMessageBox.confirm('确定作废该外协单吗？', '提示', { type: 'warning' })
  await outsourceOrderVoid(row.id); ElMessage.success('已作废'); load()
}
async function viewItems(row: any) {
  detailItems.value = await outsourceOrderItems(row.id)
  detailVisible.value = true
}

loadBase()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
