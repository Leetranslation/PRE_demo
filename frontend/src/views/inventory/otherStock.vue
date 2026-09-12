<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="方向">
        <el-select v-model="query.direction" placeholder="全部" clearable style="width: 120px">
          <el-option label="入库" value="in" />
          <el-option label="出库" value="out" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.type" placeholder="全部" clearable style="width: 120px">
          <el-option label="期初" value="期初" />
          <el-option label="盘盈" value="盘盈" />
          <el-option label="盘亏" value="盘亏" />
          <el-option label="领用" value="领用" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增其他出入库</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="stockNo" label="单号" width="160" />
      <el-table-column label="方向" width="80">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'in' ? 'success' : 'warning'" size="small">
            {{ row.direction === 'in' ? '入库' : '出库' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="90" />
      <el-table-column prop="stockDate" label="日期" width="110" />
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

  <el-dialog v-model="dialogVisible" title="新增其他出入库" width="680px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="方向" required>
        <el-radio-group v-model="form.direction">
          <el-radio-button value="in">入库</el-radio-button>
          <el-radio-button value="out">出库</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type" placeholder="请选择" style="width: 100%">
          <el-option label="期初" value="期初" />
          <el-option label="盘盈" value="盘盈" />
          <el-option label="盘亏" value="盘亏" />
          <el-option label="领用" value="领用" />
          <el-option label="其他" value="其他" />
        </el-select>
      </el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="form.warehouseId" placeholder="请选择" filterable style="width: 100%">
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
      <el-table-column prop="itemName" label="物料" min-width="160" />
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

  <el-dialog v-model="detailVisible" title="明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="itemName" label="物料/产品" width="140" />
      <el-table-column prop="quantity" label="数量" width="110" align="right" />
    </el-table>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'OtherStock' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { otherStockList, otherStockCreate } from '@/api/inventory'
import { warehouseList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const pickerVisible = ref(false)
const warehouses = ref<any[]>([])
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, direction: undefined as string | undefined, type: undefined as string | undefined })
const form = reactive<any>({ items: [] })

function onSelect(item: any) {
  form.items.push({ itemId: item.id, itemType: 'material', itemName: `${item.code} ${item.name}`, quantity: 1 })
}

async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await otherStockList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.direction = undefined; query.type = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  form.direction = 'in'
  loadWarehouses()
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.direction || !form.items || form.items.length === 0) { ElMessage.warning('请选择方向并添加明细'); return }
  await otherStockCreate({
    requestId: crypto.randomUUID(),
    direction: form.direction, type: form.type, warehouseId: form.warehouseId, remark: form.remark,
    items: form.items.map((i: any) => ({ itemId: i.itemId, itemType: i.itemType, quantity: Number(i.quantity) }))
  })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

function viewItems(row: any) {
  ElMessage.info('明细：' + row.stockNo)
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
