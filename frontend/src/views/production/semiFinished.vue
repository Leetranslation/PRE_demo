<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="工单号">
        <el-input v-model="query.keyword" placeholder="工单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog">新增半成品加工单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="orderNo" label="加工单号" width="170" />
      <el-table-column prop="productId" label="产品ID" width="100" />
      <el-table-column prop="quantity" label="数量" width="100" align="right" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">{{ approveText(row.approveStatus) }}</template>
      </el-table-column>
      <el-table-column label="业务状态" width="100">
        <template #default="{ row }">
          <el-tag :type="bizType(row.bizStatus)" size="small">{{ bizText(row.bizStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="planDate" label="计划日期" width="110" />
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="doSubmit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="doApprove(row)">审批</el-button>
          <el-button v-if="row.bizStatus === 'pending' && row.approveStatus === 'approved'" link type="primary" @click="doStart(row)">开始</el-button>
          <el-button v-if="row.bizStatus === 'producing'" link type="warning" @click="doFinish(row)">完工</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增半成品加工单" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="产品" required>
        <el-input :model-value="form.productName" placeholder="点击选择产品" readonly style="width: 100%">
          <template #append><el-button @click="pickerVisible = true">选择</el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item label="数量" required>
        <el-input v-model="form.quantity" type="number" style="width: 100%" />
      </el-form-item>
      <el-form-item label="计划日期">
        <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <ProductPicker v-model="pickerVisible" @select="onProduct" />
</template>

<script setup lang="ts">
defineOptions({ name: 'SemiFinished' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  productionOrderList, productionOrderCreate, productionOrderSubmit,
  productionOrderApprove, productionOrderStart, productionOrderFinish
} from '@/api/production'
import ProductPicker from '@/components/ProductPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const pickerVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const form = reactive<any>({})

function approveText(s: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[s] || s }
function bizText(s: string) { return { pending: '未开始', producing: '执行中', finished: '已完工', completed: '已完成', voided: '已作废' }[s] || s }
function bizType(s: string) { return { pending: 'info', producing: 'primary', finished: 'success', completed: 'success', voided: 'danger' }[s] || 'info' }

function onProduct(p: any) {
  form.productId = p.id
  form.productName = `${p.code} ${p.name}`
}

async function load() {
  loading.value = true
  try {
    const res = await productionOrderList({ ...query, orderType: 'semi' })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  form.quantity = 1
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.productId) { ElMessage.warning('请选择产品'); return }
  if (!form.quantity || Number(form.quantity) <= 0) { ElMessage.warning('数量必须大于0'); return }
  await productionOrderCreate({ orderType: 'semi', productId: form.productId, quantity: Number(form.quantity), planDate: form.planDate, remark: form.remark })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function doSubmit(row: any) { await productionOrderSubmit(row.id); ElMessage.success('已提交'); load() }
async function doApprove(row: any) { await productionOrderApprove(row.id); ElMessage.success('已审批'); load() }
async function doStart(row: any) { await productionOrderStart(row.id); ElMessage.success('已开始'); load() }
async function doFinish(row: any) { await productionOrderFinish(row.id); ElMessage.success('已完工'); load() }

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
