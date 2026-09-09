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
      <el-button @click="openBatchPrice">批量设置标准单价</el-button>
      <el-button @click="openBatchPriceType">批量设置计价方式</el-button>
      <el-button @click="doExport">导出</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="code" label="工序编号" width="160" />
      <el-table-column prop="name" label="工序名称" min-width="140" />
      <el-table-column prop="category" label="类别" width="100" />
      <el-table-column label="计价方式" width="100">
        <template #default="{ row }">{{ priceTypeText(row.priceType) }}</template>
      </el-table-column>
      <el-table-column prop="standardPrice" label="标准单价" width="110" />
      <el-table-column prop="description" label="说明" min-width="160" show-overflow-tooltip />
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑工序' : '新增工序'" width="640px">
    <el-form :model="form" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="工序名称" required><el-input v-model="form.name" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="类别"><el-input v-model="form.category" placeholder="如：加工/检验/外协" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="计价方式" required>
            <el-select v-model="form.priceType" style="width: 100%">
              <el-option label="计件" value="piece" />
              <el-option label="计时" value="time" />
              <el-option label="固定价" value="fixed" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="标准单价"><el-input v-model="form.standardPrice" placeholder="标准单价" /></el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="说明"><el-input v-model="form.description" type="textarea" /></el-form-item>
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

  <el-dialog v-model="batchPriceVisible" title="批量设置标准单价" width="400px">
    <el-form label-width="110px">
      <el-form-item label="标准单价" required>
        <el-input v-model="batchPrice" type="number" placeholder="请输入标准单价" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchPriceVisible = false">取消</el-button>
      <el-button type="primary" @click="handleBatchPrice">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="batchTypeVisible" title="批量设置计价方式" width="400px">
    <el-form label-width="110px">
      <el-form-item label="计价方式" required>
        <el-select v-model="batchPriceType" style="width: 100%">
          <el-option label="计件" value="piece" />
          <el-option label="计时" value="time" />
          <el-option label="固定价" value="fixed" />
        </el-select>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchTypeVisible = false">取消</el-button>
      <el-button type="primary" @click="handleBatchPriceType">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'Process' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { processList, processCreate, processUpdate, processStatus, processBatchPrice, processBatchPriceType } from '@/api/base'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const batchPriceVisible = ref(false)
const batchTypeVisible = ref(false)
const batchPrice = ref<number>()
const batchPriceType = ref('piece')
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })
const form = reactive<any>({})

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'code', label: '工序编号' },
    { prop: 'name', label: '工序名称' },
    { prop: 'category', label: '类别' },
    { prop: 'priceType', label: '计价方式', formatter: (row: any) => priceTypeText(row.priceType) },
    { prop: 'standardPrice', label: '标准单价' },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '工序管理'
)
function doExport() { handleExport(records.value) }

function priceTypeText(v: string) {
  return { piece: '计件', time: '计时', fixed: '固定价' }[v] || v
}

async function load() {
  loading.value = true
  try {
    const res = await processList(query)
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
    form.priceType = 'piece'
  }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name || !form.priceType) {
    ElMessage.warning('请填写名称和计价方式')
    return
  }
  if (form.id) {
    await processUpdate(form)
  } else {
    await processCreate(form)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该工序吗？`, '提示', { type: 'warning' })
  await processStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

function openBatchPrice() {
  if (selection.value.length === 0) {
    ElMessage.warning('请先勾选要设置的工序')
    return
  }
  batchPrice.value = undefined
  batchPriceVisible.value = true
}

async function handleBatchPrice() {
  if (batchPrice.value == null) {
    ElMessage.warning('请输入标准单价')
    return
  }
  await processBatchPrice(selection.value.map((r) => r.id), Number(batchPrice.value))
  ElMessage.success('批量设置成功')
  batchPriceVisible.value = false
  load()
}

function openBatchPriceType() {
  if (selection.value.length === 0) {
    ElMessage.warning('请先勾选要设置的工序')
    return
  }
  batchPriceType.value = 'piece'
  batchTypeVisible.value = true
}

async function handleBatchPriceType() {
  await processBatchPriceType(selection.value.map((r) => r.id), batchPriceType.value)
  ElMessage.success('批量设置成功')
  batchTypeVisible.value = false
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
