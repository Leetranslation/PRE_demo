<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="工单">
        <el-select v-model="query.workOrderId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="已排产" value="planned" />
          <el-option label="已开始" value="started" />
          <el-option label="已完成" value="finished" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增排产</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="scheduleNo" label="排产单号" width="160" />
      <el-table-column label="工单" width="160">
        <template #default="{ row }">{{ orderNo(row.workOrderId) }}</template>
      </el-table-column>
      <el-table-column label="机台" width="120">
        <template #default="{ row }">{{ machineName(row.machineId) }}</template>
      </el-table-column>
      <el-table-column prop="planStart" label="计划开始" width="160" />
      <el-table-column prop="planEnd" label="计划结束" width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'planned'" link type="success" @click="changeStatus(row, 'started')">开始</el-button>
          <el-button v-if="row.status === 'started'" link type="primary" @click="changeStatus(row, 'finished')">完成</el-button>
          <el-button v-if="row.status === 'planned'" link type="danger" @click="changeStatus(row, 'cancelled')">取消</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增排产计划" width="560px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="生产工单" required>
        <el-select v-model="form.workOrderId" placeholder="请选择工单" filterable style="width: 100%" @change="onOrderChange">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="机台">
        <el-select v-model="form.machineId" placeholder="请选择机台" clearable filterable style="width: 100%">
          <el-option v-for="m in machines" :key="m.id" :label="m.name" :value="m.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划开始" required>
        <el-date-picker v-model="form.planStart" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
      </el-form-item>
      <el-form-item label="计划结束" required>
        <el-date-picker v-model="form.planEnd" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'Schedule' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { schedulePlanList, schedulePlanCreate, schedulePlanStatus, productionOrderList } from '@/api/production'
import { machineList } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const orders = ref<any[]>([])
const machines = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, workOrderId: undefined as number | undefined, status: undefined as string | undefined })
const form = reactive<any>({})

function orderNo(id?: number) {
  const o = orders.value.find((x) => x.id === id)
  return o ? o.orderNo : ''
}
function machineName(id?: number) {
  const m = machines.value.find((x) => x.id === id)
  return m ? m.name : ''
}
function statusText(v: string) { return { planned: '已排产', started: '已开始', finished: '已完成', cancelled: '已取消' }[v] || v }
function statusTagType(v: string) { return { planned: 'primary', started: 'warning', finished: 'success', cancelled: 'info' }[v] || 'info' }

async function loadOrders() {
  const res = await productionOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}
async function loadMachines() {
  const res = await machineList({ page: 1, pageSize: 200 })
  machines.value = res.records
}

function onOrderChange(orderId?: number) {
  const order = orders.value.find((x) => x.id === orderId)
  if (order && order.machineId) form.machineId = order.machineId
}

async function load() {
  loading.value = true
  try {
    const res = await schedulePlanList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.workOrderId = undefined; query.status = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.workOrderId || !form.planStart || !form.planEnd) {
    ElMessage.warning('请选择工单并填写计划起止时间')
    return
  }
  await schedulePlanCreate(form)
  ElMessage.success('排产成功')
  dialogVisible.value = false
  load()
}

async function changeStatus(row: any, status: string) {
  const text = statusText(status)
  await ElMessageBox.confirm(`确定${text}该排产计划吗？`, '提示', { type: 'warning' })
  await schedulePlanStatus(row.id, status)
  ElMessage.success('操作成功')
  load()
}

loadOrders()
loadMachines()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
