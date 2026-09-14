<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="设备">
        <el-select v-model="query.machineId" placeholder="全部" clearable filterable style="width: 220px">
          <el-option v-for="m in machines" :key="m.id" :label="`${m.code} ${m.name}`" :value="m.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="计划" value="planned" />
          <el-option label="完成" value="done" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增保养计划</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="machineCode" label="设备编号" width="140" />
      <el-table-column prop="machineName" label="设备名称" min-width="130" />
      <el-table-column prop="planDate" label="计划日期" width="120" />
      <el-table-column prop="actualDate" label="实际日期" width="120" />
      <el-table-column prop="content" label="保养内容" min-width="150" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'done' ? 'success' : 'warning'" size="small">{{ row.status === 'done' ? '完成' : '计划' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="maintainer" label="保养人" width="100" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'done'" link type="success" @click="handleComplete(row)">完成</el-button>
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑保养' : '新增保养计划'" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="设备" required>
        <el-select v-model="form.machineId" filterable placeholder="选择设备" style="width: 100%">
          <el-option v-for="m in machines" :key="m.id" :label="`${m.code} ${m.name}`" :value="m.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="计划日期"><el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      <el-form-item label="保养内容"><el-input v-model="form.content" type="textarea" placeholder="如：更换润滑油、检查皮带" /></el-form-item>
      <el-form-item label="保养人"><el-input v-model="form.maintainer" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'EquipmentMaintenance' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { equipmentMaintenanceList, equipmentMaintenanceCreate, equipmentMaintenanceUpdate, equipmentMaintenanceComplete, equipmentMaintenanceDelete } from '@/api/equipment'
import { machineList } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const machines = ref<any[]>([])
const dialogVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, machineId: undefined as number | undefined, status: undefined as string | undefined })
const form = reactive<any>({})

async function loadMachines() {
  const res = await machineList({ page: 1, pageSize: 200 })
  machines.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await equipmentMaintenanceList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.machineId = undefined; query.status = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.machineId) { ElMessage.warning('请选择设备'); return }
  if (form.id) await equipmentMaintenanceUpdate(form)
  else await equipmentMaintenanceCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleComplete(row: any) {
  await ElMessageBox.confirm('确定标记为完成保养吗？（会回写设备上次保养日期）', '提示', { type: 'warning' })
  await equipmentMaintenanceComplete(row.id)
  ElMessage.success('已完成保养')
  load()
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm('确定删除该保养记录吗？', '提示', { type: 'warning' })
  await equipmentMaintenanceDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

loadMachines()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
