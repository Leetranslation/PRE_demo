<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="采购单号" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="审批状态">
        <el-select v-model="query.approveStatus" placeholder="全部" clearable style="width: 140px">
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
      <el-button type="primary" @click="openDialog()">新增采购单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="orderNo" label="采购单号" width="200" />
      <el-table-column prop="supplierId" label="供应商ID" min-width="120" />
      <el-table-column prop="orderDate" label="订单日期" width="140" />
      <el-table-column label="审批状态" width="120">
        <template #default="{ row }">
          <el-tag :type="approveTagType(row.approveStatus)">{{ approveText(row.approveStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="业务状态" width="120">
        <template #default="{ row }">{{ bizText(row.bizStatus) }}</template>
      </el-table-column>
      <el-table-column prop="totalAmount" label="金额" width="140" align="right" />
      <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.approveStatus === 'pending'" link type="primary" @click="handleSubmit(row)">提交</el-button>
          <el-button v-if="row.approveStatus === 'approving'" link type="success" @click="handleApprove(row)">审批通过</el-button>
          <el-button v-if="row.approveStatus === 'approved' && row.bizStatus === 'pending_inbound'" link type="warning" @click="handleUnapprove(row)">反审核</el-button>
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

  <!-- 新增采购单 -->
  <el-dialog v-model="dialogVisible" title="新增采购单" width="820px">
    <el-form :model="form" label-width="90px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="供应商" required>
            <el-select v-model="form.supplierId" filterable remote :remote-method="searchSupplier" :loading="supplierLoading" placeholder="搜索供应商" clearable style="width: 100%">
              <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12"><el-form-item label="采购类型">
          <el-select v-model="form.purchaseType" style="width: 100%">
            <el-option label="备货采购" value="stock" />
            <el-option label="生产采购" value="production" />
            <el-option label="MRP采购" value="mrp" />
          </el-select>
        </el-form-item></el-col>
        <el-col :span="12"><el-form-item label="预计到货"><el-date-picker v-model="form.expectedDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="采购员"><el-input v-model="form.buyer" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item></el-col>
      </el-row>
    </el-form>

    <div class="item-toolbar">
      <span>采购明细</span>
      <el-button size="small" type="primary" @click="addItem">添加明细</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column label="物料" min-width="220">
        <template #default="{ row }">
          <div class="material-cell">
            <template v-if="row.materialId">
              <span class="material-name">{{ row.materialName }}</span>
              <span class="material-spec">{{ row.spec }}</span>
            </template>
            <template v-else>
              <span class="material-empty">未选择物料</span>
            </template>
          </div>
          <el-button link size="small" type="primary" @click="openMaterialPicker(row)">
            {{ row.materialId ? '更换' : '选择物料' }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="120">
        <template #default="{ row }"><el-input-number v-model="row.quantity" :min="0" :precision="6" size="small" style="width: 100%" /></template>
      </el-table-column>
      <el-table-column label="含税单价" width="130">
        <template #default="{ row }"><el-input-number v-model="row.price" :min="0" :precision="6" size="small" style="width: 100%" /></template>
      </el-table-column>
      <el-table-column label="税率%" width="100">
        <template #default="{ row }"><el-input-number v-model="row.taxRate" :min="0" :precision="2" size="small" style="width: 100%" /></template>
      </el-table-column>
      <el-table-column label="单位" width="90">
        <template #default="{ row }">{{ row.unit }}</template>
      </el-table-column>
      <el-table-column label="价税合计" width="120" align="right">
        <template #default="{ row }">{{ calcTotal(row) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="70">
        <template #default="{ $index }"><el-button link type="danger" @click="form.items.splice($index, 1)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <!-- 明细查看 -->
  <el-dialog v-model="itemsVisible" title="采购明细" width="760px">
    <el-table :data="items" border size="small">
      <el-table-column prop="materialId" label="物料ID" width="100" />
      <el-table-column prop="quantity" label="数量" width="120" />
      <el-table-column prop="price" label="含税单价" width="120" />
      <el-table-column prop="amountExTax" label="不含税金额" width="120" />
      <el-table-column prop="taxAmount" label="税额" width="100" />
      <el-table-column prop="totalAmount" label="价税合计" width="120" />
      <el-table-column prop="inboundQty" label="已入库" width="100" />
    </el-table>
  </el-dialog>

  <!-- 物料选择弹窗 -->
  <MaterialPicker v-model="pickerVisible" @select="onMaterialSelected" />
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  purchaseOrderList, purchaseOrderItems, purchaseOrderCreate,
  purchaseOrderSubmit, purchaseOrderApprove, purchaseOrderUnapprove
} from '@/api/purchase'
import { supplierList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const itemsVisible = ref(false)
const items = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', approveStatus: undefined as string | undefined })
const form = reactive<any>({ items: [] })

// 供应商下拉
const suppliers = ref<any[]>([])
const supplierLoading = ref(false)
// 物料选择
const pickerVisible = ref(false)
const currentItemRow = ref<any>(null)

async function searchSupplier(keyword: string) {
  supplierLoading.value = true
  try {
    const res = await supplierList({ page: 1, pageSize: 20, keyword, status: 1 })
    suppliers.value = res.records
  } finally {
    supplierLoading.value = false
  }
}

function openMaterialPicker(row: any) {
  currentItemRow.value = row
  pickerVisible.value = true
}

function onMaterialSelected(material: any) {
  if (currentItemRow.value) {
    currentItemRow.value.materialId = material.id
    currentItemRow.value.materialName = material.name
    currentItemRow.value.spec = material.spec
    currentItemRow.value.unit = material.unit
  }
}

function approveText(v: string) {
  return { pending: '待提交', approving: '审批中', approved: '已审批' }[v] || v
}
function approveTagType(v: string) {
  return { pending: 'info', approving: 'warning', approved: 'success' }[v] || 'info'
}
function bizText(v: string) {
  return { pending_inbound: '待入库', partial_inbound: '部分入库', completed: '已完成' }[v] || v
}
function calcTotal(row: any) {
  return ((row.quantity || 0) * (row.price || 0)).toFixed(2)
}

async function load() {
  loading.value = true
  try {
    const res = await purchaseOrderList(query)
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
  form.purchaseType = 'stock'
  searchSupplier('')
  dialogVisible.value = true
}

function addItem() {
  form.items.push({ materialId: undefined, materialName: '', spec: '', quantity: 0, price: 0, taxRate: 13, unit: '' })
}

async function handleSave() {
  if (!form.supplierId) {
    ElMessage.warning('请选择供应商')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请至少添加一条明细')
    return
  }
  for (const item of form.items) {
    if (!item.materialId) {
      ElMessage.warning('存在未选择物料的明细行')
      return
    }
    if (!item.quantity || item.quantity <= 0) {
      ElMessage.warning('存在数量未填或为0的明细行')
      return
    }
  }
  await purchaseOrderCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function handleSubmit(row: any) {
  await ElMessageBox.confirm('确定提交审批吗？', '提示', { type: 'warning' })
  await purchaseOrderSubmit(row.id)
  ElMessage.success('已提交')
  load()
}

async function handleApprove(row: any) {
  await ElMessageBox.confirm('确定审批通过吗？', '提示', { type: 'warning' })
  await purchaseOrderApprove(row.id)
  ElMessage.success('已审批')
  load()
}

async function handleUnapprove(row: any) {
  await ElMessageBox.confirm('确定反审核吗？', '提示', { type: 'warning' })
  await purchaseOrderUnapprove(row.id)
  ElMessage.success('已反审核')
  load()
}

async function viewItems(row: any) {
  items.value = await purchaseOrderItems(row.id)
  itemsVisible.value = true
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.item-toolbar { display: flex; justify-content: space-between; align-items: center; margin: 8px 0; }
.material-cell { display: flex; flex-direction: column; margin-bottom: 2px; }
.material-name { font-weight: 500; color: #333; }
.material-spec { font-size: 12px; color: #999; }
.material-empty { font-size: 12px; color: #c0c4cc; }
</style>
