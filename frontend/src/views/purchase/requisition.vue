<template>
  <el-card shadow="never">
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
      <el-button type="primary" @click="openDialog()">新增申请单</el-button>
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
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="submit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="approve(row)">审批</el-button>
          <el-button v-if="row.approveStatus === 'approved'" link type="danger" @click="unapprove(row)">反审核</el-button>
          <el-button link type="primary" @click="viewItems(row)">明细</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增采购申请单" width="720px">
    <el-form :model="form" label-width="100px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="申请人"><el-input v-model="form.applicant" /></el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">申请明细</el-divider>
    <div class="item-toolbar">
      <el-button type="primary" size="small" @click="materialPickerVisible = true">添加物料</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column prop="materialName" label="物料" min-width="160" />
      <el-table-column label="需求数量" width="130">
        <template #default="{ row }"><el-input v-model="row.quantity" type="number" size="small" /></template>
      </el-table-column>
      <el-table-column label="需求日期" width="160">
        <template #default="{ row }">
          <el-date-picker v-model="row.requiredDate" type="date" value-format="YYYY-MM-DD" size="small" style="width: 100%" />
        </template>
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

  <el-dialog v-model="detailVisible" title="申请明细" width="600px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialId" label="物料ID" width="90" />
      <el-table-column prop="quantity" label="需求数量" width="110" align="right" />
      <el-table-column prop="requiredDate" label="需求日期" width="120" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="materialPickerVisible" @select="onMaterialSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseRequisition' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { purchaseRequisitionList, purchaseRequisitionCreate, purchaseRequisitionSubmit, purchaseRequisitionApprove, purchaseRequisitionUnapprove, purchaseRequisitionItems } from '@/api/purchase'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
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

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  dialogVisible.value = true
}

function onMaterialSelect(material: any) {
  form.items.push({ materialId: material.id, materialName: `${material.code} ${material.name}`, quantity: 1, requiredDate: undefined })
}

async function handleSave() {
  if (!form.items || form.items.length === 0) { ElMessage.warning('请添加申请明细'); return }
  const payload = {
    applicant: form.applicant,
    remark: form.remark,
    items: form.items.map((i: any) => ({
      materialId: i.materialId, spec: i.spec, unit: i.unit,
      quantity: Number(i.quantity), requiredDate: i.requiredDate, remark: i.remark
    }))
  }
  await purchaseRequisitionCreate(payload)
  ElMessage.success('保存成功')
  dialogVisible.value = false
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

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
