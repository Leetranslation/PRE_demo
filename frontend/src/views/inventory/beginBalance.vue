<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="类型">
        <el-select v-model="query.targetType" placeholder="全部" clearable style="width: 130px">
          <el-option label="客户应收" value="customer" />
          <el-option label="供应商应付" value="supplier" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="名称" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增期初</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column label="类型" width="110">
        <template #default="{ row }">{{ row.targetType === 'customer' ? '客户应收' : '供应商应付' }}</template>
      </el-table-column>
      <el-table-column prop="targetName" label="对象名称" min-width="160" />
      <el-table-column prop="amount" label="期初金额" width="130" align="right" />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑期初' : '新增期初'" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="类型" required>
        <el-radio-group v-model="form.targetType" :disabled="!!form.id">
          <el-radio-button value="customer">客户应收</el-radio-button>
          <el-radio-button value="supplier">供应商应付</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="对象" required>
        <el-input :model-value="form.targetName" placeholder="点击选择" readonly style="width: 100%">
          <template #append><el-button @click="openPicker">选择</el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item label="期初金额" required>
        <el-input v-model="form.amount" type="number" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="pickerVisible" :title="form.targetType === 'customer' ? '选择客户' : '选择供应商'" width="560px">
    <el-table :data="pickerRecords" border size="small" @row-click="pick" highlight-current-row style="cursor: pointer">
      <el-table-column prop="code" label="编号" width="150" />
      <el-table-column prop="name" label="名称" min-width="160" />
    </el-table>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'BeginBalance' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { beginBalanceList, beginBalanceSave, beginBalanceDelete } from '@/api/inventory'
import { customerList, supplierList } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const pickerVisible = ref(false)
const pickerRecords = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, targetType: undefined as string | undefined, keyword: '' })
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const res = await beginBalanceList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.targetType = undefined; query.keyword = ''; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  else form.targetType = 'customer'
  dialogVisible.value = true
}

async function openPicker() {
  if (!form.targetType) { ElMessage.warning('请先选择类型'); return }
  const res = form.targetType === 'customer'
    ? await customerList({ page: 1, pageSize: 100, status: 1 })
    : await supplierList({ page: 1, pageSize: 100, status: 1 })
  pickerRecords.value = res.records
  pickerVisible.value = true
}

function pick(row: any) {
  form.targetId = row.id
  form.targetName = `${row.code} ${row.name}`
  pickerVisible.value = false
}

async function handleSave() {
  if (!form.targetType || !form.targetId) { ElMessage.warning('请选择类型和对象'); return }
  if (form.amount === undefined || form.amount === null || form.amount === '') { ElMessage.warning('请填写期初金额'); return }
  await beginBalanceSave({ id: form.id, targetType: form.targetType, targetId: form.targetId, targetName: form.targetName, amount: Number(form.amount), remark: form.remark })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function doDelete(row: any) {
  await ElMessageBox.confirm('确定删除该期初余额吗？', '提示', { type: 'warning' })
  await beginBalanceDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
