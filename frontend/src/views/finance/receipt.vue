<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="客户">
        <el-select v-model="query.customerId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
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
      <el-button type="primary" @click="openDialog()">新增收款</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="receiptNo" label="收款单号" width="170" />
      <el-table-column prop="customerName" label="客户" min-width="160" />
      <el-table-column prop="amount" label="收款金额" width="120" align="right" />
      <el-table-column prop="writtenOffAmount" label="已核销" width="120" align="right" />
      <el-table-column prop="balance" label="未核销余额" width="120" align="right" />
      <el-table-column prop="receiptDate" label="收款日期" width="120" />
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

  <!-- 新增收款 -->
  <el-dialog v-model="dialogVisible" title="新增收款" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="客户" required>
        <el-select v-model="form.customerId" filterable placeholder="选择客户" style="width: 100%">
          <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="收款金额" required><el-input v-model="form.amount" type="number" /></el-form-item>
      <el-form-item label="收款日期"><el-date-picker v-model="form.receiptDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item>
      <el-form-item label="收款账户"><el-input v-model="form.account" placeholder="如：基本户" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <!-- 核销应收 -->
  <el-dialog v-model="writeoffVisible" :title="'核销收款单 ' + currentReceipt.receiptNo" width="760px">
    <div class="writeoff-tip">未核销余额：<b>{{ currentReceipt.balance }}</b></div>
    <el-table :data="receivables" border size="small">
      <el-table-column prop="receivableNo" label="应收单号" width="160" />
      <el-table-column prop="invoiceNo" label="来源发票" width="160" />
      <el-table-column prop="balance" label="应收余额" width="120" align="right" />
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
      <el-table-column prop="receivableNo" label="应收单号" width="170" />
      <el-table-column prop="customerName" label="客户" width="140" />
      <el-table-column prop="amount" label="核销金额" width="120" align="right" />
      <el-table-column prop="writeoffDate" label="核销日期" width="120" />
    </el-table>
  </el-dialog>

  <PrintDialog
    v-model="printVisible"
    title="收款单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    total-label="收款金额"
    :total-value="printTotal"
    :remark="printRemark"
    :show-upper="true"
    :total-amount="printTotal"
    :sign-labels="['制单人', '审核人', '客户签收']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Receipt' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { receiptList, receiptCreate, receiptWriteoff, receiptWriteoffItems, receivableList } from '@/api/finance'
import { customerList } from '@/api/base'
import PrintDialog from '@/components/PrintDialog.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const customers = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, customerId: undefined as number | undefined, status: undefined as string | undefined })

const dialogVisible = ref(false)
const writeoffVisible = ref(false)
const itemsVisible = ref(false)
const form = reactive<any>({})
const currentReceipt = reactive<any>({})
const receivables = ref<any[]>([])
const writeoffItems = ref<any[]>([])

function statusText(v: string) { return { pending: '未核销', partial: '部分核销', settled: '已核销完' }[v] || v }
function statusTagType(v: string) { return { pending: 'primary', partial: 'warning', settled: 'success' }[v] || 'info' }

async function loadCustomers() {
  const res = await customerList({ page: 1, pageSize: 200 })
  customers.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await receiptList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.customerId = undefined; query.status = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.customerId || form.amount == null || Number(form.amount) <= 0) {
    ElMessage.warning('请选择客户并填写收款金额')
    return
  }
  await receiptCreate({ ...form, amount: Number(form.amount) })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function openWriteoff(row: any) {
  Object.assign(currentReceipt, row)
  const res = await receivableList({ page: 1, pageSize: 200, customerId: row.customerId, status: undefined })
  receivables.value = res.records.filter((r: any) => r.balance > 0).map((r: any) => ({ ...r, writeoffAmount: undefined }))
  writeoffVisible.value = true
}

async function handleWriteoff() {
  const items = receivables.value
    .filter((r: any) => r.writeoffAmount != null && Number(r.writeoffAmount) > 0)
    .map((r: any) => ({ receivableId: r.id, amount: Number(r.writeoffAmount) }))
  if (!items.length) { ElMessage.warning('请填写核销金额'); return }
  await receiptWriteoff(currentReceipt.id, items)
  ElMessage.success('核销成功')
  writeoffVisible.value = false
  load()
}

async function viewItems(row: any) {
  writeoffItems.value = await receiptWriteoffItems(row.id)
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
    [{ label: '收款单号', value: row.receiptNo }, { label: '收款日期', value: row.receiptDate }],
    [{ label: '客户', value: row.customerName }, { label: '收款账户', value: row.account }],
    [{ label: '状态', value: statusText(row.status) }]
  ]
  printRows.value = [
    { item: '收款金额', amt: row.amount },
    { item: '已核销金额', amt: row.writtenOffAmount },
    { item: '未核销余额', amt: row.balance }
  ]
  printRemark.value = row.remark
  printTotal.value = row.amount
  printVisible.value = true
}

loadCustomers()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.writeoff-tip { margin-bottom: 8px; color: #666; }
</style>
