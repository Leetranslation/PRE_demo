<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="设备">
        <el-select v-model="query.machineId" placeholder="全部" clearable filterable style="width: 220px">
          <el-option v-for="m in machines" :key="m.id" :label="`${m.code} ${m.name}`" :value="m.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.repairStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="待维修" value="pending" />
          <el-option label="维修中" value="repairing" />
          <el-option label="已完成" value="done" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">设备报修</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="repairNo" label="维修单号" width="160" />
      <el-table-column prop="machineName" label="设备" min-width="130" />
      <el-table-column prop="faultDesc" label="故障描述" min-width="160" />
      <el-table-column prop="reportDate" label="报修日期" width="110" />
      <el-table-column prop="repairer" label="维修人" width="90" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.repairStatus)" size="small">{{ statusText(row.repairStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="finishDate" label="完成日期" width="110" />
      <el-table-column prop="downtimeHours" label="停机时(时)" width="100" align="right" />
      <el-table-column prop="cost" label="维修费用" width="100" align="right" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.repairStatus === 'pending'" link type="primary" @click="changeStatus(row, 'repairing')">开始维修</el-button>
          <el-button v-if="row.repairStatus === 'repairing'" link type="success" @click="changeStatus(row, 'done')">完成</el-button>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑维修工单' : '设备报修'" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="设备" required>
        <el-select v-model="form.machineId" filterable placeholder="选择设备" style="width: 100%">
          <el-option v-for="m in machines" :key="m.id" :label="`${m.code} ${m.name}`" :value="m.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="故障描述"><el-input v-model="form.faultDesc" type="textarea" /></el-form-item>
      <el-form-item label="报修日期"><el-date-picker v-model="form.reportDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      <el-form-item label="维修人"><el-input v-model="form.repairer" /></el-form-item>
      <el-form-item label="停机时(时)"><el-input v-model="form.downtimeHours" type="number" /></el-form-item>
      <el-form-item label="维修费用"><el-input v-model="form.cost" type="number" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <PrintDialog
    v-model="printVisible"
    title="设备维修单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['报修人', '维修人', '验收人']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'EquipmentRepair' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { equipmentRepairList, equipmentRepairCreate, equipmentRepairUpdate, equipmentRepairStatus, equipmentRepairDelete } from '@/api/equipment'
import { machineList } from '@/api/base'
import PrintDialog from '@/components/PrintDialog.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const machines = ref<any[]>([])
const dialogVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, machineId: undefined as number | undefined, repairStatus: undefined as string | undefined })
const form = reactive<any>({})

function statusText(v: string) { return { pending: '待维修', repairing: '维修中', done: '已完成' }[v] || v }
function statusTagType(v: string) { return { pending: 'danger', repairing: 'warning', done: 'success' }[v] || 'info' }

async function loadMachines() {
  const res = await machineList({ page: 1, pageSize: 200 })
  machines.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await equipmentRepairList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.machineId = undefined; query.repairStatus = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.machineId) { ElMessage.warning('请选择设备'); return }
  if (form.id) await equipmentRepairUpdate(form)
  else await equipmentRepairCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function changeStatus(row: any, status: string) {
  const label = status === 'done' ? '完成维修（设备恢复正常）' : '开始维修'
  await ElMessageBox.confirm(`确定「${label}」吗？`, '提示', { type: 'warning' })
  await equipmentRepairStatus(row.id, status)
  ElMessage.success('操作成功')
  load()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该维修工单吗？', '提示', { type: 'warning' })
  await equipmentRepairDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printCols = [
  { label: '项目', prop: 'item' },
  { label: '内容', prop: 'val' }
]

function handlePrint(row: any) {
  printHead.value = [
    [{ label: '维修单号', value: row.repairNo }, { label: '报修日期', value: row.reportDate }],
    [{ label: '设备', value: row.machineName }, { label: '维修人', value: row.repairer }],
    [{ label: '状态', value: statusText(row.repairStatus) }, { label: '完成日期', value: row.finishDate }]
  ]
  printRows.value = [
    { item: '故障描述', val: row.faultDesc },
    { item: '停机时长（小时）', val: row.downtimeHours },
    { item: '维修费用', val: row.cost }
  ]
  printRemark.value = row.remark
  printVisible.value = true
}

loadMachines()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
