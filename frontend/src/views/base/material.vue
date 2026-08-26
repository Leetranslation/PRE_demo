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
      <el-table-column prop="code" label="物料编号" width="160" />
      <el-table-column prop="name" label="物料名称" min-width="160" />
      <el-table-column prop="spec" label="规格" width="120" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column label="供应属性" width="110">
        <template #default="{ row }">
          {{ supplyTypeText(row.supplyType) }}
        </template>
      </el-table-column>
      <el-table-column label="批次/序列号" width="100">
        <template #default="{ row }">
          {{ batchText(row.batchManage) }}
        </template>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑物料' : '新增物料'" width="640px">
    <el-form :model="form" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="物料名称" required><el-input v-model="form.name" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="分类" required><el-input v-model="form.category" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="单位" required><el-input v-model="form.unit" placeholder="如：个/公斤" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="规格"><el-input v-model="form.spec" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="供应属性">
            <el-select v-model="form.supplyType" style="width: 100%">
              <el-option label="采购" value="purchase" />
              <el-option label="自制" value="make" />
              <el-option label="采购+自制" value="purchase_make" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="批次/序列号">
            <el-select v-model="form.batchManage" style="width: 100%">
              <el-option label="不管理" value="none" />
              <el-option label="批次" value="batch" />
              <el-option label="序列号" value="serial" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="安全库存"><el-input-number v-model="form.safetyStock" :min="0" :precision="6" style="width: 100%" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="采购提前期(天)"><el-input-number v-model="form.purchaseLeadDays" :min="0" style="width: 100%" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="采购质检">
            <el-switch v-model="form.purchaseQc" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="生产质检">
            <el-switch v-model="form.productionQc" :active-value="1" :inactive-value="0" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { materialList, materialCreate, materialUpdate, materialStatus } from '@/api/base'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })
const form = reactive<any>({})

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'code', label: '物料编号' },
    { prop: 'name', label: '物料名称' },
    { prop: 'spec', label: '规格' },
    { prop: 'unit', label: '单位' },
    { prop: 'supplyType', label: '供应属性', formatter: (row: any) => supplyTypeText(row.supplyType) },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '物料档案'
)
function doExport() { handleExport(records.value) }

function supplyTypeText(v: string) {
  return { purchase: '采购', make: '自制', purchase_make: '采购+自制' }[v] || v
}
function batchText(v: string) {
  return { none: '不管理', batch: '批次', serial: '序列号' }[v] || v
}

async function load() {
  loading.value = true
  try {
    const res = await materialList(query)
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
  if (row) {
    Object.assign(form, row)
  } else {
    form.supplyType = 'purchase'
    form.batchManage = 'none'
    form.purchaseQc = 0
    form.productionQc = 0
    form.safetyStock = 0
    form.purchaseLeadDays = 0
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name || !form.category || !form.unit) {
    ElMessage.warning('请填写名称、分类和单位')
    return
  }
  if (form.id) {
    await materialUpdate(form)
  } else {
    await materialCreate(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该物料吗？`, '提示', { type: 'warning' })
  await materialStatus(row.id, row.status === 1 ? 0 : 1)
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
