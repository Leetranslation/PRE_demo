<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="未生成工单" value="planned" />
          <el-option label="已生成工单" value="confirmed" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增主生产计划</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="mpsNo" label="计划单号" width="160" />
      <el-table-column label="产品" min-width="160">
        <template #default="{ row }">{{ productName(row.productId) }}</template>
      </el-table-column>
      <el-table-column prop="quantity" label="计划数量" width="110" align="right" />
      <el-table-column prop="planDate" label="计划生产日期" width="130" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="row.status === 'confirmed' ? 'success' : 'warning'" size="small">
            {{ row.status === 'confirmed' ? '已生成工单' : '未生成工单' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="工单号" width="160">
        <template #default="{ row }">{{ row.workOrderId ? orderNo(row.workOrderId) : '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'planned'" link type="primary" @click="generate(row)">生成工单</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增主生产计划" width="520px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="产品" required>
        <div style="display: flex; gap: 8px; width: 100%">
          <el-input :model-value="productLabel" placeholder="点击右侧选择产品" readonly />
          <el-button @click="productPickerVisible = true">选择产品</el-button>
        </div>
      </el-form-item>
      <el-form-item label="计划数量" required><el-input v-model="form.quantity" type="number" /></el-form-item>
      <el-form-item label="计划生产日期">
        <el-date-picker v-model="form.planDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <ProductPicker v-model="productPickerVisible" @select="onProductSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'Mps' })
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mpsPlanList, mpsPlanCreate, mpsPlanGenerate, productionOrderList } from '@/api/production'
import ProductPicker from '@/components/ProductPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const productPickerVisible = ref(false)
const orders = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, status: undefined as string | undefined })
const form = reactive<any>({})

const productLabel = computed(() => form.productName || '')

function productName(id?: number) {
  // 从产品列表拿名称（简化为 ID，实际应查产品）
  return id ? ('产品#' + id) : ''
}
function orderNo(id?: number) {
  const o = orders.value.find((x) => x.id === id)
  return o ? o.orderNo : ''
}

function onProductSelect(product: any) {
  form.productId = product.id
  form.productName = `${product.code} ${product.name}`
}

async function loadOrders() {
  const res = await productionOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await mpsPlanList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.status = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.productId || form.quantity == null) {
    ElMessage.warning('请选择产品并填写计划数量')
    return
  }
  await mpsPlanCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function generate(row: any) {
  await ElMessageBox.confirm('确定根据该计划生成生产工单吗？', '提示', { type: 'warning' })
  const res = await mpsPlanGenerate(row.id)
  ElMessage.success('已生成工单：' + (res as any).data?.orderNo || '')
  load()
}

loadOrders()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
