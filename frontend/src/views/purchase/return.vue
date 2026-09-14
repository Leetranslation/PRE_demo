<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="订单">
        <el-select v-model="query.orderId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增退货</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="returnNo" label="退货单号" width="160" />
      <el-table-column prop="sourceOrderNo" label="采购订单号" width="160" />
      <el-table-column prop="returnDate" label="退货日期" width="110" />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
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

  <el-dialog v-model="dialogVisible" title="新增采购退货（库存出库）" width="680px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="采购订单" required>
        <el-select v-model="form.orderId" placeholder="请选择订单" filterable style="width: 100%">
          <el-option v-for="o in orders" :key="o.id" :label="o.orderNo" :value="o.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="出库仓库">
        <el-select v-model="form.warehouseId" placeholder="请选择仓库" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>

    <el-divider content-position="left">退货明细</el-divider>
    <div class="item-toolbar">
      <el-button type="primary" size="small" @click="materialPickerVisible = true">添加物料</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column prop="materialName" label="物料" min-width="160" />
      <el-table-column label="退货数量" width="130">
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

  <el-dialog v-model="detailVisible" title="退货明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialName" label="物料" width="140" />
      <el-table-column prop="quantity" label="退货数量" width="110" align="right" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="materialPickerVisible" @select="onMaterialSelect" />

  <PrintDialog
    v-model="printVisible"
    title="采购退货单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['制单人', '仓管员', '供应商']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'PurchaseReturn' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { purchaseReturnList, purchaseReturnCreate, purchaseReturnItems, purchaseOrderList } from '@/api/purchase'
import { warehouseList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const materialPickerVisible = ref(false)
const orders = ref<any[]>([])
const warehouses = ref<any[]>([])
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, orderId: undefined as number | undefined })
const form = reactive<any>({ items: [] })

async function loadOrders() {
  const res = await purchaseOrderList({ page: 1, pageSize: 200 })
  orders.value = res.records
}
async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

function onMaterialSelect(material: any) {
  form.items.push({ materialId: material.id, materialName: `${material.code} ${material.name}`, quantity: 1 })
}

async function load() {
  loading.value = true
  try {
    const res = await purchaseReturnList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.orderId = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  loadOrders()
  loadWarehouses()
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.orderId || !form.items || form.items.length === 0) { ElMessage.warning('请选择订单并添加退货明细'); return }
  await purchaseReturnCreate({
    orderId: form.orderId, warehouseId: form.warehouseId, remark: form.remark,
    items: form.items.map((i: any) => ({ materialId: i.materialId, quantity: Number(i.quantity) }))
  })
  ElMessage.success('退货成功')
  dialogVisible.value = false
  load()
}

async function viewItems(row: any) {
  detailItems.value = await purchaseReturnItems(row.id)
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
  { label: '退货数量', prop: 'quantity', align: 'right' }
]

async function handlePrint(row: any) {
  printRows.value = await purchaseReturnItems(row.id)
  printHead.value = [
    [{ label: '退货单号', value: row.returnNo }, { label: '退货日期', value: row.returnDate }],
    [{ label: '采购订单号', value: row.sourceOrderNo }]
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
