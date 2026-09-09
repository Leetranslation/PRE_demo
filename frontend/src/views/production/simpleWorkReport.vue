<template>
  <div class="simple-report">
    <el-card shadow="never">
      <div class="header">
        <h3>简易报工</h3>
        <div class="filters">
          <el-select v-model="workshopId" placeholder="选择车间" clearable filterable style="width: 180px" @change="loadOrders">
            <el-option v-for="w in workshops" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
          <el-select v-model="orderId" placeholder="选择工单" clearable filterable style="width: 220px" @change="onOrderChange">
            <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
          </el-select>
        </div>
      </div>

      <div class="quick-form" v-if="orderId">
        <el-form :inline="true">
          <el-form-item label="工序">
            <el-select v-model="processId" placeholder="选择工序" filterable style="width: 180px">
              <el-option v-for="p in processes" :key="p.processId" :label="p.processName" :value="p.processId" />
            </el-select>
          </el-form-item>
          <el-form-item label="报工数量">
            <el-input v-model="quantity" type="number" style="width: 120px" />
          </el-form-item>
          <el-form-item label="良品">
            <el-input v-model="qualifiedQty" type="number" style="width: 120px" />
          </el-form-item>
          <el-form-item label="不良品">
            <el-input v-model="defectiveQty" type="number" style="width: 120px" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" @click="submitReport">提交报工</el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="recentReports" v-loading="loading" border stripe>
        <el-table-column prop="reportNo" label="报工单号" width="160" />
        <el-table-column label="工单" width="160">
          <template #default="{ row }">{{ orderNo(row.workOrderId) }}</template>
        </el-table-column>
        <el-table-column label="工序" width="120">
          <template #default="{ row }">{{ processName(row.processId) }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="90" align="right" />
        <el-table-column prop="qualifiedQty" label="良品" width="80" align="right" />
        <el-table-column prop="defectiveQty" label="不良品" width="80" align="right" />
        <el-table-column prop="reporterName" label="报工人" width="100" />
        <el-table-column prop="reportDate" label="报工时间" width="170" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.auditStatus === 'approved' ? 'success' : 'warning'" size="small">
              {{ row.auditStatus === 'approved' ? '已审' : '待审' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'SimpleWorkReport' })
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { productionOrderList, reportWorkCreate, reportWorkList } from '@/api/production'
import { workshopList, processRouteItems, processList } from '@/api/base'

const loading = ref(false)
const workshops = ref<any[]>([])
const orders = ref<any[]>([])
const processes = ref<any[]>([])
const allProcesses = ref<any[]>([])
const workshopId = ref<number>()
const orderId = ref<number>()
const processId = ref<number>()
const quantity = ref<number>()
const qualifiedQty = ref<number>()
const defectiveQty = ref<number>()
const recentReports = ref<any[]>([])

function orderNo(id?: number) {
  const o = orders.value.find((x) => x.id === id)
  return o ? o.orderNo : ''
}
function processName(id?: number) {
  const p = allProcesses.value.find((x) => x.id === id)
  return p ? p.name : ''
}

async function loadWorkshops() {
  const res = await workshopList({ page: 1, pageSize: 100, status: 1 })
  workshops.value = res.records
}
async function loadAllProcesses() {
  const res = await processList({ page: 1, pageSize: 200 })
  allProcesses.value = res.records
}

async function loadOrders() {
  const res = await productionOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records.filter((o: any) => !workshopId.value || o.workshopId === workshopId.value)
}

async function onOrderChange(orderId?: number) {
  processId.value = undefined
  processes.value = []
  if (!orderId) return
  const order = orders.value.find((x) => x.id === orderId)
  if (order && order.processRouteId) {
    const items = await processRouteItems(order.processRouteId)
    processes.value = items.map((it: any) => ({
      processId: it.processId,
      processName: processName(it.processId) || ('工序' + it.processId)
    }))
  }
  loadRecentReports()
}

async function loadRecentReports() {
  loading.value = true
  try {
    const res = await reportWorkList({ page: 1, pageSize: 20 })
    recentReports.value = res.records
  } finally {
    loading.value = false
  }
}

async function submitReport() {
  if (!orderId.value || !processId.value || quantity.value == null) {
    ElMessage.warning('请选择工单、工序并填写数量')
    return
  }
  await reportWorkCreate({
    workOrderId: orderId.value,
    processId: processId.value,
    quantity: Number(quantity.value),
    qualifiedQty: qualifiedQty.value != null ? Number(qualifiedQty.value) : Number(quantity.value),
    defectiveQty: defectiveQty.value != null ? Number(defectiveQty.value) : 0
  })
  ElMessage.success('报工成功')
  quantity.value = undefined
  qualifiedQty.value = undefined
  defectiveQty.value = undefined
  loadRecentReports()
}

loadWorkshops()
loadAllProcesses()
loadOrders()
loadRecentReports()
</script>

<style scoped>
.simple-report { padding: 16px; }
.header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.header h3 { margin: 0; }
.filters { display: flex; gap: 8px; }
.quick-form { background: #f5f7fa; padding: 16px; border-radius: 6px; margin-bottom: 16px; }
</style>
