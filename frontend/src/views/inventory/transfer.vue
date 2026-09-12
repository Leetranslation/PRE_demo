<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="源仓库">
        <el-select v-model="query.sourceWarehouseId" placeholder="全部" clearable filterable style="width: 150px">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标仓库">
        <el-select v-model="query.targetWarehouseId" placeholder="全部" clearable filterable style="width: 150px">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog">新增调拨</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="transferNo" label="调拨单号" width="170" />
      <el-table-column label="源仓库" width="140">
        <template #default="{ row }">{{ warehouseName(row.sourceWarehouseId) }}</template>
      </el-table-column>
      <el-table-column label="目标仓库" width="140">
        <template #default="{ row }">{{ warehouseName(row.targetWarehouseId) }}</template>
      </el-table-column>
      <el-table-column prop="transferDate" label="调拨日期" width="110" />
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="100" fixed="right">
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

  <el-dialog v-model="dialogVisible" title="新增调拨" width="680px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="源仓库" required>
        <el-select v-model="form.sourceWarehouseId" placeholder="请选择" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标仓库" required>
        <el-select v-model="form.targetWarehouseId" placeholder="请选择" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>

    <el-divider content-position="left">明细</el-divider>
    <div class="item-toolbar">
      <el-button type="primary" size="small" @click="pickerVisible = true">添加物料</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column prop="itemName" label="物料" min-width="180" />
      <el-table-column label="数量" width="130">
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

  <el-dialog v-model="detailVisible" title="调拨明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="itemName" label="物料/产品" width="160" />
      <el-table-column prop="itemType" label="类型" width="100" />
      <el-table-column prop="quantity" label="数量" width="110" align="right" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'Transfer' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { transferList, transferItems, transferCreate } from '@/api/inventory'
import { warehouseList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const warehouses = ref<any[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const pickerVisible = ref(false)
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, sourceWarehouseId: undefined as number | undefined, targetWarehouseId: undefined as number | undefined })
const form = reactive<any>({ items: [] })

function warehouseName(id: number) { return warehouses.value.find((w) => w.id === id)?.name || id }

function onSelect(item: any) {
  form.items.push({ itemId: item.id, itemType: 'material', itemName: `${item.code} ${item.name}`, quantity: 1, unit: item.unit })
}

async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await transferList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.sourceWarehouseId = undefined; query.targetWarehouseId = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.sourceWarehouseId || !form.targetWarehouseId) { ElMessage.warning('请选择源仓库和目标仓库'); return }
  if (!form.items || form.items.length === 0) { ElMessage.warning('请添加明细'); return }
  await transferCreate({
    sourceWarehouseId: form.sourceWarehouseId, targetWarehouseId: form.targetWarehouseId, remark: form.remark,
    items: form.items.map((i: any) => ({ itemId: i.itemId, itemType: i.itemType, quantity: Number(i.quantity), unit: i.unit }))
  })
  ElMessage.success('调拨成功')
  dialogVisible.value = false
  load()
}

async function viewItems(row: any) {
  detailItems.value = await transferItems(row.id)
  detailVisible.value = true
}

loadWarehouses()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
