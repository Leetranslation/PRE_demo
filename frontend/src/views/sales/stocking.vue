<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="备货单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增备货单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="stockingNo" label="备货单号" width="160" />
      <el-table-column prop="sourceOrderNo" label="销售订单号" width="160" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)" size="small">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="submit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="approve(row)">审批</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增备货单" width="680px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="销售订单">
        <el-select v-model="form.orderId" placeholder="可选" clearable filterable style="width: 100%">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>

    <el-divider content-position="left">备货明细</el-divider>
    <div class="item-toolbar">
      <el-button type="primary" size="small" @click="productPickerVisible = true">添加产品</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column prop="productName" label="产品" min-width="160" />
      <el-table-column label="数量" width="120">
        <template #default="{ row }"><el-input v-model="row.quantity" type="number" size="small" /></template>
      </el-table-column>
      <el-table-column label="操作" width="70">
        <template #default="{ $index }">
          <el-button link type="danger" @click="form.items.splice($index, 1)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <ProductPicker v-model="productPickerVisible" @select="onProductSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'SalesStocking' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { salesStockingList, salesStockingCreate, salesStockingSubmit, salesStockingApprove, salesOrderList } from '@/api/sales'
import ProductPicker from '@/components/ProductPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const productPickerVisible = ref(false)
const orders = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({ items: [] })

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }

async function loadOrders() {
  const res = await salesOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}

function onProductSelect(product: any) {
  form.items.push({ productId: product.id, productName: `${product.code} ${product.name}`, quantity: 1 })
}

async function load() {
  loading.value = true
  try {
    const res = await salesStockingList(query)
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
  form.items = []
  loadOrders()
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.items || form.items.length === 0) { ElMessage.warning('请添加备货明细'); return }
  await salesStockingCreate({
    orderId: form.orderId, remark: form.remark,
    items: form.items.map((i: any) => ({ productId: i.productId, quantity: Number(i.quantity) }))
  })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function submit(row: any) { await salesStockingSubmit(row.id); ElMessage.success('已提交'); load() }
async function approve(row: any) { await salesStockingApprove(row.id); ElMessage.success('审批通过'); load() }

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
