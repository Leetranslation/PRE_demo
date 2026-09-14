<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="供应商">
        <el-select v-model="query.supplierId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="未核销" value="pending" />
          <el-option label="部分核销" value="partial" />
          <el-option label="已核销完" value="settled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增付款</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="paymentNo" label="付款单号" width="170" />
      <el-table-column prop="supplierName" label="供应商" min-width="160" />
      <el-table-column prop="amount" label="付款金额" width="120" align="right" />
      <el-table-column prop="writtenOffAmount" label="已核销" width="120" align="right" />
      <el-table-column prop="balance" label="未核销余额" width="120" align="right" />
      <el-table-column prop="paymentDate" label="付款日期" width="120" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.balance > 0" link type="primary" @click="openWriteoff(row)">核销</el-button>
          <el-button link type="info" @click="viewItems(row)">明细</el-button>
          <el-button link type="primary" @click="handlePrint(row)">打印</el-button>
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

  <!-- 新增付款 -->
  <el-dialog v-model="dialogVisible" title="新增付款" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="供应商" required>
        <el-select v-model="form.supplierId" filterable placeholder="选择供应商" style="width: 100%">
          <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="付款金额" required><el-input v-model="form.amount" type="number" /></el-form-item>
      <el-form-item label="付款日期"><el-date-picker v-model="form.paymentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      <el-form-item label="付款账户"><el-input v-model="form.account" placeholder="如：基本户" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <!-- 核销应付 -->
  <el-dialog v-model="writeoffVisible" :title="'核销付款单 ' + currentPayment.paymentNo" width="760px">
    <div class="writeoff-tip">未核销余额：<b>{{ currentPayment.balance }}</b></div>
    <el-table :data="payables" border size="small">
      <el-table-column prop="payableNo" label="应付单号" width="160" />
      <el-table-column prop="invoiceNo" label="来源发票" width="160" />
      <el-table-column prop="balance" label="应付余额" width="120" align="right" />
      <el-table-column label="本次核销金额" min-width="160">
        <template #default="{ row }">
          <el-input v-model="row.writeoffAmount" size="small" type="number" placeholder="核销金额" />
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="writeoffVisible = false">取消</el-button>
      <el-button type="primary" @click="handleWriteoff">确认核销</el-button>
    </template>
  </el-dialog>

  <!-- 核销明细 -->
  <el-dialog v-model="itemsVisible" title="核销明细" width="640px">
    <el-table :data="writeoffItems" border size="small">
      <el-table-column prop="payableNo" label="应付单号" width="170" />
      <el-table-column prop="supplierName" label="供应商" width="140" />
      <el-table-column prop="amount" label="核销金额" width="120" align="right" />
      <el-table-column prop="writeoffDate" label="核销日期" width="120" />
    </el-table>
  </el-dialog>

  <PrintDialog
    v-model="printVisible"
    title="付款单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    total-label="付款金额"
    :total-value="printTotal"
    :remark="printRemark"
    :show-upper="true"
    :total-amount="printTotal"
    :sign-labels="['制单人', '审核人', '供应商']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Payment' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { paymentList, paymentCreate, paymentWriteoff, paymentWriteoffItems, payableList } from '@/api/finance'
import { supplierList } from '@/api/base'
import PrintDialog from '@/components/PrintDialog.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const suppliers = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, supplierId: undefined as number | undefined, status: undefined as string | undefined })

const dialogVisible = ref(false)
const writeoffVisible = ref(false)
const itemsVisible = ref(false)
const form = reactive<any>({})
const currentPayment = reactive<any>({})
const payables = ref<any[]>([])
const writeoffItems = ref<any[]>([])

function statusText(v: string) { return { pending: '未核销', partial: '部分核销', settled: '已核销完' }[v] || v }
function statusTagType(v: string) { return { pending: 'primary', partial: 'warning', settled: 'success' }[v] || 'info' }

async function loadSuppliers() {
  const res = await supplierList({ page: 1, pageSize: 200 })
  suppliers.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await paymentList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.supplierId = undefined; query.status = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.supplierId || form.amount == null || Number(form.amount) <= 0) {
    ElMessage.warning('请选择供应商并填写付款金额')
    return
  }
  await paymentCreate({ ...form, amount: Number(form.amount) })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function openWriteoff(row: any) {
  Object.assign(currentPayment, row)
  const res = await payableList({ page: 1, pageSize: 200, supplierId: row.supplierId, status: undefined })
  payables.value = res.records.filter((p: any) => p.balance > 0).map((p: any) => ({ ...p, writeoffAmount: undefined }))
  writeoffVisible.value = true
}

async function handleWriteoff() {
  const items = payables.value
    .filter((p: any) => p.writeoffAmount != null && Number(p.writeoffAmount) > 0)
    .map((p: any) => ({ payableId: p.id, amount: Number(p.writeoffAmount) }))
  if (!items.length) { ElMessage.warning('请填写核销金额'); return }
  await paymentWriteoff(currentPayment.id, items)
  ElMessage.success('核销成功')
  writeoffVisible.value = false
  load()
}

async function viewItems(row: any) {
  writeoffItems.value = await paymentWriteoffItems(row.id)
  itemsVisible.value = true
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printTotal = ref<any>(0)
const printCols = [
  { label: '项目', prop: 'item' },
  { label: '金额', prop: 'amt', align: 'right' }
]

function handlePrint(row: any) {
  printHead.value = [
    [{ label: '付款单号', value: row.paymentNo }, { label: '付款日期', value: row.paymentDate }],
    [{ label: '供应商', value: row.supplierName }, { label: '付款账户', value: row.account }],
    [{ label: '状态', value: statusText(row.status) }]
  ]
  printRows.value = [
    { item: '付款金额', amt: row.amount },
    { item: '已核销金额', amt: row.writtenOffAmount },
    { item: '未核销余额', amt: row.balance }
  ]
  printRemark.value = row.remark
  printTotal.value = row.amount
  printVisible.value = true
}

loadSuppliers()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.writeoff-tip { margin-bottom: 8px; color: #666; }
</style>
