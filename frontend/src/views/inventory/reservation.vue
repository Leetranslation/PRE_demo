<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="已锁定" value="locked" />
          <el-option label="已释放" value="released" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">锁库</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="itemCode" label="物料/产品编号" width="150" show-overflow-tooltip />
      <el-table-column prop="itemName" label="物料/产品名称" min-width="140" show-overflow-tooltip />
      <el-table-column prop="bizType" label="业务类型" width="140" />
      <el-table-column prop="bizNo" label="业务单号" width="160" />
      <el-table-column prop="quantity" label="锁定数量" width="110" align="right" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'locked' ? 'warning' : 'info'" size="small">
            {{ row.status === 'locked' ? '已锁定' : '已释放' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="锁定时间" width="170" />
      <el-table-column prop="releaseTime" label="释放时间" width="170" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'locked'" link type="danger" @click="release(row)">解锁</el-button>
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

  <el-dialog v-model="dialogVisible" title="锁库" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="物料/产品" required>
        <div style="display: flex; gap: 8px; width: 100%">
          <el-input :model-value="itemLabel" placeholder="点击选择" readonly />
          <el-button @click="pickerVisible = true">选择</el-button>
        </div>
      </el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="form.warehouseId" placeholder="请选择" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="锁定数量" required><el-input v-model="form.quantity" type="number" /></el-form-item>
      <el-form-item label="业务类型"><el-input v-model="form.bizType" placeholder="如 sales_outbound" /></el-form-item>
      <el-form-item label="业务单号"><el-input v-model="form.bizNo" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">锁定</el-button>
    </template>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'InventoryReservation' })
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { reservationList, reservationLock, reservationRelease } from '@/api/inventory'
import { warehouseList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const pickerVisible = ref(false)
const warehouses = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, status: undefined as string | undefined })
const form = reactive<any>({})

const itemLabel = computed(() => form.itemName || '')

function onSelect(item: any) {
  form.itemId = item.id
  form.itemType = 'material'
  form.itemName = `${item.code} ${item.name}`
}

async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await reservationList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.status = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  loadWarehouses()
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.itemId || form.quantity == null) { ElMessage.warning('请选择物料并填写数量'); return }
  await reservationLock({
    itemId: form.itemId, itemType: form.itemType, warehouseId: form.warehouseId,
    stockType: 'available', quantity: Number(form.quantity),
    bizType: form.bizType, bizNo: form.bizNo
  })
  ElMessage.success('锁库成功')
  dialogVisible.value = false
  load()
}

async function release(row: any) {
  await ElMessageBox.confirm('确定解锁该锁库记录吗？', '提示', { type: 'warning' })
  await reservationRelease(row.id)
  ElMessage.success('已解锁')
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
