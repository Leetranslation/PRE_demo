<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="工单">
        <el-select v-model="query.workOrderId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="审工状态">
        <el-select v-model="query.auditStatus" placeholder="全部" clearable style="width: 140px">
          <el-option label="待审" value="pending" />
          <el-option label="已审" value="approved" />
          <el-option label="驳回" value="rejected" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="success" @click="handleBatchApprove">一键审核</el-button>
      <el-button type="warning" @click="handleBatchUnapprove">一键反审核</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="reportNo" label="报工单号" width="160" />
      <el-table-column label="工单" width="160">
        <template #default="{ row }">{{ orderNo(row.workOrderId) }}</template>
      </el-table-column>
      <el-table-column label="工序" width="120">
        <template #default="{ row }">{{ processName(row.processId) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="报工数量" width="100" align="right" />
      <el-table-column prop="qualifiedQty" label="良品" width="80" align="right" />
      <el-table-column prop="defectiveQty" label="不良品" width="80" align="right" />
      <el-table-column prop="reporterName" label="报工人" width="100" />
      <el-table-column label="审工状态" width="90">
        <template #default="{ row }">
          <el-tag :type="auditTagType(row.auditStatus)" size="small">{{ auditText(row.auditStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="auditorName" label="审核人" width="100" />
      <el-table-column prop="auditTime" label="审核时间" width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.auditStatus === 'pending'" link type="success" @click="approve(row)">审核</el-button>
          <el-button v-if="row.auditStatus === 'pending'" link type="danger" @click="reject(row)">驳回</el-button>
          <el-button v-if="row.auditStatus === 'approved'" link type="warning" @click="unapprove(row)">反审核</el-button>
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
</template>

<script setup lang="ts">
defineOptions({ name: 'AuditWork' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reportWorkList, reportWorkApprove, reportWorkUnapprove, reportWorkReject, reportWorkBatchApprove, productionOrderList } from '@/api/production'
import { processList } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const orders = ref<any[]>([])
const allProcesses = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, workOrderId: undefined as number | undefined, auditStatus: undefined as string | undefined })

function orderNo(id?: number) {
  const o = orders.value.find((x) => x.id === id)
  return o ? o.orderNo : ''
}
function processName(id?: number) {
  const p = allProcesses.value.find((x) => x.id === id)
  return p ? p.name : ''
}
function auditText(v: string) { return { pending: '待审', approved: '已审', rejected: '驳回' }[v] || v }
function auditTagType(v: string) { return { pending: 'warning', approved: 'success', rejected: 'danger' }[v] || 'info' }

async function loadOrders() {
  const res = await productionOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}
async function loadAllProcesses() {
  const res = await processList({ page: 1, pageSize: 200 })
  allProcesses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await reportWorkList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.workOrderId = undefined; query.auditStatus = undefined; query.page = 1; load() }

async function approve(row: any) {
  await reportWorkApprove(row.id)
  ElMessage.success('审核通过')
  load()
}
async function unapprove(row: any) {
  await ElMessageBox.confirm('确定反审核该报工单吗？', '提示', { type: 'warning' })
  await reportWorkUnapprove(row.id)
  ElMessage.success('反审核成功')
  load()
}
async function reject(row: any) {
  const { value } = await ElMessageBox.prompt('请输入驳回意见', '驳回', { inputPlaceholder: '驳回原因' })
  await reportWorkReject(row.id, value || '')
  ElMessage.success('已驳回')
  load()
}
async function handleBatchApprove() {
  await ElMessageBox.confirm('确定一键审核所有待审报工单吗？', '提示', { type: 'warning' })
  await reportWorkBatchApprove(query.workOrderId)
  ElMessage.success('一键审核完成')
  load()
}
async function handleBatchUnapprove() {
  ElMessage.warning('一键反审核请逐条操作（保证审核准确性）')
}

loadOrders()
loadAllProcesses()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
