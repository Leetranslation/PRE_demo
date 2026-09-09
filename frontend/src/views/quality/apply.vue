<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="质检来源">
        <el-select v-model="query.sourceType" placeholder="全部" clearable style="width: 150px">
          <el-option label="来料质检" value="purchase_qc" />
          <el-option label="生产质检" value="production_qc" />
          <el-option label="工序质检" value="process_qc" />
          <el-option label="出库质检" value="outbound_qc" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="待检" value="pending" />
          <el-option label="检验中" value="inspecting" />
          <el-option label="已检" value="finished" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增质检申请</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="applyNo" label="申请单号" width="160" />
      <el-table-column label="质检来源" width="110">
        <template #default="{ row }">{{ sourceText(row.sourceType) }}</template>
      </el-table-column>
      <el-table-column prop="sourceNo" label="来源单号" width="150" />
      <el-table-column prop="itemId" label="物料/产品ID" width="120" />
      <el-table-column prop="quantity" label="质检数量" width="100" align="right" />
      <el-table-column prop="applyDate" label="申请日期" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="130" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" link type="primary" @click="generate(row)">生成检验单</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增质检申请" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="质检来源" required>
        <el-select v-model="form.sourceType" placeholder="请选择" style="width: 100%">
          <el-option label="来料质检" value="purchase_qc" />
          <el-option label="生产质检" value="production_qc" />
          <el-option label="工序质检" value="process_qc" />
          <el-option label="出库质检" value="outbound_qc" />
        </el-select>
      </el-form-item>
      <el-form-item label="物料/产品" required>
        <div style="display: flex; gap: 8px; width: 100%">
          <el-input :model-value="itemLabel" placeholder="点击选择" readonly />
          <el-button @click="pickerVisible = true">选择</el-button>
        </div>
      </el-form-item>
      <el-form-item label="来源单号"><el-input v-model="form.sourceNo" placeholder="可选" /></el-form-item>
      <el-form-item label="质检数量" required><el-input v-model="form.quantity" type="number" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'QcApply' })
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { qcApplyList, qcApplyCreate, qcApplyGenerate } from '@/api/purchase'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const pickerVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, sourceType: undefined as string | undefined, status: undefined as string | undefined })
const form = reactive<any>({})

const itemLabel = computed(() => form.itemName || '')

function sourceText(v: string) {
  return { purchase_qc: '来料质检', production_qc: '生产质检', process_qc: '工序质检', outbound_qc: '出库质检' }[v] || v
}
function statusText(v: string) { return { pending: '待检', inspecting: '检验中', finished: '已检' }[v] || v }
function statusTagType(v: string) { return { pending: 'warning', inspecting: 'primary', finished: 'success' }[v] || 'info' }

function onSelect(item: any) {
  form.itemId = item.id
  form.itemType = 'material'
  form.itemName = `${item.code} ${item.name}`
}

async function load() {
  loading.value = true
  try {
    const res = await qcApplyList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.sourceType = undefined; query.status = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.sourceType || !form.itemId || form.quantity == null) { ElMessage.warning('请选择来源、物料并填写数量'); return }
  await qcApplyCreate({
    sourceType: form.sourceType, sourceNo: form.sourceNo,
    itemId: form.itemId, itemType: form.itemType,
    quantity: Number(form.quantity), remark: form.remark
  })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function generate(row: any) {
  await ElMessageBox.confirm('确定生成检验单吗？', '提示', { type: 'warning' })
  await qcApplyGenerate(row.id)
  ElMessage.success('已生成检验单')
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
