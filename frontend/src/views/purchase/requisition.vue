<template>
  <el-card v-if="!formVisible" shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="申请单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
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
      <el-button type="primary" @click="openForm()">新增申请单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="requisitionNo" label="申请单号" width="160" />
      <el-table-column prop="requisitionDate" label="申请日期" width="110" />
      <el-table-column prop="applicant" label="申请人" width="100" />
      <el-table-column label="审批状态" width="100">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)" size="small">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="submit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="approve(row)">审批</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="danger" @click="unapprove(row)">反审核</el-button>
          <el-button link type="primary" @click="viewItems(row)">明细</el-button>
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

  <!-- 新增采购申请单：整页单据表单 -->
  <DocForm
    v-else
    title="新建采购申请单"
    :model-value="form"
    :items="form.items"
    :fields="docFields"
    :columns="docColumns"
    hide-add
    @update:items="(v) => (form.items = v)"
    @cancel="closeForm"
    @save="handleSave(false)"
    @submit="handleSave(true)"
  >
    <template #tools>
      <el-button type="primary" size="small" @click="materialPickerVisible = true">添加物料</el-button>
    </template>
    <template #summary>
      <span>数量合计：{{ sumQty }}</span>
    </template>
  </DocForm>

  <el-dialog v-model="detailVisible" title="申请明细" width="600px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialName" label="物料" width="140" />
      <el-table-column prop="quantity" label="需求数量" width="110" align="right" />
      <el-table-column prop="requiredDate" label="需求日期" width="120" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="materialPickerVisible" @select="onMaterialSelect" />

  <PrintDialog
    v-model="printVisible"
    title="采购申请单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['申请人', '审批人']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseRequisition' })
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseRequisitionList, purchaseRequisitionCreate, purchaseRequisitionSubmit, purchaseRequisitionApprove, purchaseRequisitionUnapprove, purchaseRequisitionItems } from '@/api/purchase'
import MaterialPicker from '@/components/MaterialPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'
import DocForm from '@/components/DocForm.vue'
import type { DocColumn, DocField } from '@/components/docForm'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const formVisible = ref(false)
const detailVisible = ref(false)
const materialPickerVisible = ref(false)
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({ items: [] })

function approveText(v: string) { return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v }
function approveTagType(v: string) { return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info' }

async function load() {
  loading.value = true
  try {
    const res = await purchaseRequisitionList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.approveStatus = undefined; query.page = 1; load() }

function openForm() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  formVisible.value = true
}
function closeForm() {
  formVisible.value = false
}

/** 基本信息字段 */
const docFields: DocField[] = [
  { key: 'applicant', label: '申请人' },
  { key: 'remark', label: '备注', span: 2 }
]

/** 明细列 */
const docColumns: DocColumn[] = [
  { key: 'materialName', label: '物料', width: '240' },
  { key: 'quantity', label: '需求数量', width: '120', type: 'input', align: 'right' },
  { key: 'requiredDate', label: '需求日期', width: '160', type: 'date' }
]

const sumQty = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0), 0)
)

function onMaterialSelect(material: any) {
  form.items.push({ materialId: material.id, materialName: `${material.code} ${material.name}`, quantity: 1, requiredDate: undefined })
}

async function handleSave(submitAfter = false) {
  if (!form.items || form.items.length === 0) { ElMessage.warning('请添加申请明细'); return }
  const payload = {
    applicant: form.applicant,
    remark: form.remark,
    items: form.items.map((i: any) => ({
      materialId: i.materialId, spec: i.spec, unit: i.unit,
      quantity: Number(i.quantity), requiredDate: i.requiredDate, remark: i.remark
    }))
  }
  const created: any = await purchaseRequisitionCreate(payload)
  if (submitAfter && created?.id) {
    await purchaseRequisitionSubmit(created.id)
    ElMessage.success('已保存并提交')
  } else {
    ElMessage.success('保存成功')
  }
  formVisible.value = false
  load()
}

async function submit(row: any) { await purchaseRequisitionSubmit(row.id); ElMessage.success('已提交'); load() }
async function approve(row: any) { await purchaseRequisitionApprove(row.id); ElMessage.success('审批通过'); load() }
async function unapprove(row: any) {
  await ElMessageBox.confirm('确定反审核该申请单吗？', '提示', { type: 'warning' })
  await purchaseRequisitionUnapprove(row.id); ElMessage.success('已反审核'); load()
}
async function viewItems(row: any) {
  detailItems.value = await purchaseRequisitionItems(row.id)
  detailVisible.value = true
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printCols = [
  { label: '序号', prop: '_idx', width: '40px', align: 'center', formatter: (_r: any, i: number) => i + 1 },
  { label: '物料', prop: 'materialName' },
  { label: '需求数量', prop: 'quantity', align: 'right' },
  { label: '需求日期', prop: 'requiredDate' }
]

async function handlePrint(row: any) {
  printRows.value = await purchaseRequisitionItems(row.id)
  printHead.value = [
    [{ label: '申请单号', value: row.requisitionNo }, { label: '申请日期', value: row.requisitionDate }],
    [{ label: '申请人', value: row.applicant }]
  ]
  printRemark.value = row.remark
  printVisible.value = true
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
