<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="名称/编码" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="query.warehouseId" placeholder="全部" clearable filterable style="width: 160px">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
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
      <el-table-column prop="code" label="库区编码" width="140" />
      <el-table-column prop="name" label="库区名称" min-width="140" />
      <el-table-column label="所属仓库" width="140">
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑库区' : '新增库区'" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="库区编码"><el-input v-model="form.code" :disabled="!!form.id" placeholder="留空自动生成" /></el-form-item>
      <el-form-item label="库区名称" required><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="所属仓库" required>
        <el-select v-model="form.warehouseId" placeholder="请选择仓库" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
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
defineOptions({ name: 'Area' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { areaList, areaCreate, areaUpdate, areaStatus, warehouseList } from '@/api/base'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const warehouses = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as number | undefined, warehouseId: undefined as number | undefined })
const form = reactive<any>({})

const { handleExport } = useExport(
  [
    { prop: 'code', label: '库区编码' },
    { prop: 'name', label: '库区名称' },
    { prop: 'warehouseId', label: '所属仓库', formatter: (row: any) => warehouseName(row.warehouseId) },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '库区'
)
function doExport() { handleExport(records.value) }

function warehouseName(id: number) {
  const w = warehouses.value.find((x) => x.id === id)
  return w ? w.name : ''
}

async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await areaList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.status = undefined; query.warehouseId = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name || !form.warehouseId) {
    ElMessage.warning('请填写库区名称并选择仓库')
    return
  }
  if (form.id) await areaUpdate(form)
  else await areaCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该库区吗？`, '提示', { type: 'warning' })
  await areaStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

loadWarehouses()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
