<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="发票号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="审批状态">
        <el-select v-model="query.approveStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="待提交" value="pending" />
          <el-option label="审批中" value="approving" />
          <el-option label="已审批" value="approved" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增发票</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="invoiceNo" label="发票号" width="160" />
      <el-table-column prop="orderId" label="订单ID" width="90" />
      <el-table-column prop="invoiceDate" label="开票日期" width="110" />
      <el-table-column prop="amount" label="发票金额" width="120" align="right" />
      <el-table-column prop="verifiedAmount" label="已核销" width="110" align="right" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)" size="small">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="submit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="approve(row)">审批</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="danger" @click="unapprove(row)">反审核</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增采购发票" width="520px">
    <el-form :model="form" label-width="110px">
      <el-form-item label="采购订单" required>
        <el-select v-model="form.orderId" placeholder="请选择订单" filterable style="width: 100%">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="可开票金额">
        <el-input :model-value="availableAmount" readonly />
      </el-form-item>
      <el-form-item label="发票金额" required>
        <el-input v-model="form.amount" type="number" placeholder="不超过可开票金额" />
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
defineOptions({ name: 'PurchaseInvoice' })
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseInvoiceList, purchaseInvoiceCreate, purchaseInvoiceSubmit, purchaseInvoiceApprove, purchaseInvoiceUnapprove, purchaseOrderList } from '@/api/purchase'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const orders = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({})

const availableAmount = computed(() => {
  const o = orders.value.find((x) => x.id === form.orderId)
  if (!o) return ''
  const invoiced = Number(o.invoicedAmount || 0)
  const totalAmt = Number(o.totalAmount || 0)
  return (totalAmt - invoiced).toFixed(2)
})

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }

async function loadOrders() {
  const res = await purchaseOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records.filter((o: any) => o.approveStatus === 'approved')
}

async function load() {
  loading.value = true
  try {
    const res = await purchaseInvoiceList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.approveStatus = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  loadOrders()
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.orderId || form.amount == null) { ElMessage.warning('请选择订单并填写发票金额'); return }
  await purchaseInvoiceCreate({ orderId: form.orderId, amount: Number(form.amount), remark: form.remark })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function submit(row: any) { await purchaseInvoiceSubmit(row.id); ElMessage.success('已提交'); load() }
async function approve(row: any) { await purchaseInvoiceApprove(row.id); ElMessage.success('审批通过'); load() }
async function unapprove(row: any) {
  await ElMessageBox.confirm('确定反审核该发票吗？', '提示', { type: 'warning' })
  await purchaseInvoiceUnapprove(row.id); ElMessage.success('已反审核'); load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
