<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="外协类型">
        <el-select v-model="query.issueType" style="width: 150px" @change="handleSearch">
          <el-option label="整单外协领料" value="outsource_whole" />
          <el-option label="工序外协领料" value="outsource_process" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog">新增外协领料</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="issueNo" label="领料单号" width="170" />
      <el-table-column label="类型" width="120">
        <template #default="{ row }">{{ row.issueType === 'outsource_whole' ? '整单外协领料' : '工序外协领料' }}</template>
      </el-table-column>
      <el-table-column label="外协单" width="160">
        <template #default="{ row }">{{ outsourceNo(row.outsourceId) }}</template>
      </el-table-column>
      <el-table-column label="生产工单" width="150">
        <template #default="{ row }">{{ orderNo(row.orderId) }}</template>
      </el-table-column>
      <el-table-column label="仓库" width="120">
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
      <el-table-column prop="issueDate" label="领料日期" width="110" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
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

  <el-dialog v-model="dialogVisible" title="新增外协领料" width="720px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="外协单" required>
        <el-select v-model="form.outsourceId" placeholder="请选择外协单" filterable style="width: 100%" @change="onOutsourceChange">
          <el-option v-for="o in outsourceOrders" :key="o.id" :label="`${o.orderNo} (${o.type === 'whole' ? '整单' : '工序'})`" :value="o.id" />
        </el-select>
        <div class="tip">仅可选「已审批」的外协单</div>
      </el-form-item>
      <el-form-item label="仓库" required>
        <el-select v-model="form.warehouseId" placeholder="请选择仓库" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>

    <el-divider content-position="left">领料明细</el-divider>
    <div class="item-toolbar">
      <el-button type="primary" size="small" @click="pickerVisible = true">添加物料</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column prop="materialName" label="物料" min-width="180" />
      <el-table-column label="数量" width="130">
        <template #default="{ row }"><el-input v-model="row.quantity" type="number" size="small" /></template>
      </el-table-column>
      <el-table-column label="批次号" width="130">
        <template #default="{ row }"><el-input v-model="row.batchNo" size="small" /></template>
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

  <el-dialog v-model="detailVisible" title="领料明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialName" label="物料" width="140" />
      <el-table-column prop="quantity" label="数量" width="110" align="right" />
      <el-table-column prop="batchNo" label="批次号" width="130" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'OutsourceIssue' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { materialIssueList, materialIssueCreate, materialIssueItems, productionOrderList } from '@/api/production'
import { outsourceOrderList } from '@/api/outsource'
import { warehouseList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const outsourceOrders = ref<any[]>([])
const warehouses = ref<any[]>([])
const orders = ref<any[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const pickerVisible = ref(false)
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, issueType: 'outsource_whole' })
const form = reactive<any>({ items: [] })

function outsourceNo(id: number) { return outsourceOrders.value.find((o) => o.id === id)?.orderNo || `#${id}` }
function orderNo(id: number) { return orders.value.find((o) => o.id === id)?.orderNo || `#${id}` }
function warehouseName(id: number) { return warehouses.value.find((w) => w.id === id)?.name || `#${id}` }

function onSelect(item: any) {
  form.items.push({ materialId: item.id, materialName: `${item.code} ${item.name}`, quantity: 1, batchNo: '' })
}

function onOutsourceChange(id: number) {
  const o = outsourceOrders.value.find((x) => x.id === id)
  if (o) form.orderId = o.orderId
}

async function loadBase() {
  const [o, w, ord] = await Promise.all([
    outsourceOrderList({ page: 1, pageSize: 200, approveStatus: 'approved' }),
    warehouseList({ page: 1, pageSize: 100, status: 1 }),
    productionOrderList({ page: 1, pageSize: 200 })
  ])
  outsourceOrders.value = o.records
  warehouses.value = w.records
  orders.value = ord.records
}

async function load() {
  loading.value = true
  try {
    const res = await materialIssueList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.outsourceId) { ElMessage.warning('请选择外协单'); return }
  if (!form.warehouseId) { ElMessage.warning('请选择仓库'); return }
  if (!form.items.length) { ElMessage.warning('请添加领料明细'); return }
  const o = outsourceOrders.value.find((x) => x.id === form.outsourceId)
  const issueType = o && o.type === 'process' ? 'outsource_process' : 'outsource_whole'
  await materialIssueCreate({
    requestId: crypto.randomUUID(),
    issueType,
    orderId: form.orderId,
    outsourceId: form.outsourceId,
    warehouseId: form.warehouseId,
    remark: form.remark,
    items: form.items.map((i: any) => ({ materialId: i.materialId, quantity: Number(i.quantity), batchNo: i.batchNo }))
  })
  ElMessage.success('外协领料成功')
  dialogVisible.value = false
  load()
}

async function viewItems(row: any) {
  detailItems.value = await materialIssueItems(row.id)
  detailVisible.value = true
}

loadBase()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.tip { font-size: 12px; color: #909399; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
