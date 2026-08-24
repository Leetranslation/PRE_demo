<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="仓库">
        <el-select v-model="query.warehouseId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="编号/名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增库位</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="warehouseId" label="仓库ID" width="100" />
      <el-table-column prop="code" label="库位编号" width="160" />
      <el-table-column prop="name" label="库位名称" min-width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑库位' : '新增库位'" width="480px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="仓库" required>
        <el-select v-model="form.warehouseId" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="库位编号" required><el-input v-model="form.code" /></el-form-item>
      <el-form-item label="库位名称"><el-input v-model="form.name" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { locationList, locationCreate, locationUpdate } from '@/api/inventory'
import { warehouseList } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const warehouses = ref<any[]>([])
const dialogVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, warehouseId: undefined as number | undefined, keyword: '' })
const form = reactive<any>({})

async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100 })
  warehouses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await locationList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.warehouseId = undefined; query.keyword = ''; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.warehouseId || !form.code) {
    ElMessage.warning('请填写仓库和库位编号')
    return
  }
  if (form.id) await locationUpdate(form)
  else await locationCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
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
