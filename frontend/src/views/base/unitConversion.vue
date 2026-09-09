<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
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
      <el-table-column label="源单位" width="140">
        <template #default="{ row }">{{ unitName(row.unitId) }}</template>
      </el-table-column>
      <el-table-column label="目标单位" width="140">
        <template #default="{ row }">{{ unitName(row.targetUnitId) }}</template>
      </el-table-column>
      <el-table-column prop="rate" label="换算率" min-width="140" />
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑换算率' : '新增换算率'" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="源单位" required>
        <el-select v-model="form.unitId" placeholder="请选择" filterable style="width: 100%">
          <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标单位" required>
        <el-select v-model="form.targetUnitId" placeholder="请选择" filterable style="width: 100%">
          <el-option v-for="u in units" :key="u.id" :label="u.name" :value="u.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="换算率" required>
        <el-input v-model="form.rate" type="number" placeholder="1源单位 = rate目标单位" />
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
defineOptions({ name: 'UnitConversion' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { unitConversionList, unitConversionCreate, unitConversionUpdate, unitConversionStatus, unitList } from '@/api/base'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const units = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, status: undefined as number | undefined })
const form = reactive<any>({})

const { handleExport } = useExport(
  [
    { prop: 'unitId', label: '源单位', formatter: (row: any) => unitName(row.unitId) },
    { prop: 'targetUnitId', label: '目标单位', formatter: (row: any) => unitName(row.targetUnitId) },
    { prop: 'rate', label: '换算率' },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '单位换算率'
)
function doExport() { handleExport(records.value) }

function unitName(id: number) {
  const u = units.value.find((x) => x.id === id)
  return u ? u.name : ''
}

async function loadUnits() {
  const res = await unitList({ page: 1, pageSize: 100, status: 1 })
  units.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await unitConversionList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.status = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.unitId || !form.targetUnitId || form.rate == null) {
    ElMessage.warning('请填写源单位、目标单位和换算率')
    return
  }
  if (form.unitId === form.targetUnitId) {
    ElMessage.warning('源单位和目标单位不能相同')
    return
  }
  if (form.id) await unitConversionUpdate(form)
  else await unitConversionCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该换算率吗？`, '提示', { type: 'warning' })
  await unitConversionStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

loadUnits()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
