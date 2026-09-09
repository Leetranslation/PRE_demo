<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="名称/编号" clearable style="width: 200px" @keyup.enter="handleSearch" />
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

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="code" label="仓库编号" width="160" />
      <el-table-column prop="name" label="仓库名称" min-width="180" />
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column label="状态" width="90">
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑仓库' : '新增仓库'" width="500px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="仓库编号" required><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
      <el-form-item label="仓库名称" required><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="类型" required><el-input v-model="form.type" placeholder="如：原材料仓/成品仓" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'Warehouse' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { warehouseList, warehouseCreate, warehouseUpdate, warehouseStatus } from '@/api/base'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })
const form = reactive<any>({})

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'code', label: '仓库编号' },
    { prop: 'name', label: '仓库名称' },
    { prop: 'type', label: '类型' },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '仓库'
)
function doExport() { handleExport(records.value) }

async function load() {
  loading.value = true
  try {
    const res = await warehouseList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.status = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name || !form.code || !form.type) {
    ElMessage.warning('请填写编号、名称和类型')
    return
  }
  if (form.id) await warehouseUpdate(form)
  else await warehouseCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该仓库吗？`, '提示', { type: 'warning' })
  await warehouseStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
