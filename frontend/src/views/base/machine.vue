<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="名称/编号" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="车间">
        <el-select v-model="query.workshopId" placeholder="全部" clearable filterable style="width: 160px">
          <el-option v-for="w in workshops" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增</el-button>
      <el-button @click="doExport">导出</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="code" label="机台编号" width="160" />
      <el-table-column prop="name" label="机台名称" min-width="140" />
      <el-table-column label="所属车间" width="140">
        <template #default="{ row }">{{ workshopName(row.workshopId) }}</template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑机台' : '新增机台'" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="机台名称" required><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="所属车间">
        <el-select v-model="form.workshopId" placeholder="请选择车间" filterable style="width: 100%">
          <el-option v-for="w in workshops" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型"><el-input v-model="form.type" placeholder="如：CNC/注塑机" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'Machine' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { machineList, machineCreate, machineUpdate, machineStatus, workshopList } from '@/api/base'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const workshops = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as number | undefined, workshopId: undefined as number | undefined })
const form = reactive<any>({})

const { handleExport } = useExport(
  [
    { prop: 'code', label: '机台编号' },
    { prop: 'name', label: '机台名称' },
    { prop: 'workshopId', label: '所属车间', formatter: (row: any) => workshopName(row.workshopId) },
    { prop: 'type', label: '类型' },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '机台设备'
)
function doExport() { handleExport(records.value) }

function workshopName(id: number) {
  const w = workshops.value.find((x) => x.id === id)
  return w ? w.name : ''
}

async function loadWorkshops() {
  const res = await workshopList({ page: 1, pageSize: 100, status: 1 })
  workshops.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await machineList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.status = undefined; query.workshopId = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name) {
    ElMessage.warning('请填写机台名称')
    return
  }
  if (form.id) await machineUpdate(form)
  else await machineCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该机台吗？`, '提示', { type: 'warning' })
  await machineStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

loadWorkshops()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
