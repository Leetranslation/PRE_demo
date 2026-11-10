<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="售后单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="待处理" value="pending" />
          <el-option label="处理中" value="processing" />
          <el-option label="已完结" value="finished" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增售后单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="afterSaleNo" label="售后单号" width="160" />
      <el-table-column prop="sourceOrderNo" label="销售订单号" width="160" />
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'pending'" link type="primary" @click="changeStatus(row, 'processing')">开始处理</el-button>
          <el-button v-if="row.status === 'processing'" link type="success" @click="changeStatus(row, 'finished')">完结</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增售后单" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="销售订单" required>
        <div class="pick-field">
          <el-input
            :model-value="pickedOrder ? `${pickedOrder.orderNo}  ${pickedOrder.customerName || ''}  ￥${pickedOrder.totalAmount ?? ''}` : ''"
            readonly
            placeholder="点右侧选择订单"
          />
          <el-button @click="orderPickerVisible = true">选择</el-button>
        </div>
      </el-form-item>
      <el-form-item label="售后类型">
        <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
          <el-option label="退货" value="退货" />
          <el-option label="维修" value="维修" />
          <el-option label="换货" value="换货" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <DocPicker
    v-model="orderPickerVisible"
    title="选择销售订单"
    keyword-placeholder="销售单号 / 客户 / 产品"
    :columns="SALES_ORDER_COLS"
    :fetcher="(p) => salesOrderList(p as any)"
    @select="applyOrder"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'AfterSale' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { afterSaleList, afterSaleCreate, afterSaleStatus, salesOrderList } from '@/api/sales'
import DocPicker from '@/components/DocPicker.vue'
import { SALES_ORDER_COLS } from '@/components/docPickerPresets'

const orderPickerVisible = ref(false)
const pickedOrder = ref<any>(null)

function applyOrder(o: any) {
  pickedOrder.value = o
  form.orderId = o.id
}

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const orders = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as string | undefined })
const form = reactive<any>({})

function statusText(v: string) { return { pending: '待处理', processing: '处理中', finished: '已完结' }[v] || v }
function statusTagType(v: string) { return { pending: 'warning', processing: 'primary', finished: 'success' }[v] || 'info' }

async function loadOrders() {
  const res = await salesOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await afterSaleList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.status = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  loadOrders()
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.orderId) { ElMessage.warning('请选择订单'); return }
  await afterSaleCreate({ orderId: form.orderId, type: form.type, remark: form.remark })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function changeStatus(row: any, status: string) {
  await afterSaleStatus(row.id, status)
  ElMessage.success('状态已更新')
  load()
}

load()
</script>

<style scoped>
.pick-field { display: flex; gap: 6px; align-items: center; }
.pick-field .el-input { flex: 1; }
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
