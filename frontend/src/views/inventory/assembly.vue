<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="类型">
        <el-select v-model="query.type" placeholder="全部" clearable style="width: 120px">
          <el-option label="组装" value="assembly" />
          <el-option label="拆卸" value="disassembly" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog">新增组装/拆卸</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="orderNo" label="单号" width="170" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'assembly' ? 'success' : 'warning'" size="small">
            {{ row.type === 'assembly' ? '组装' : '拆卸' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="productName" label="产品" width="140" />
      <el-table-column prop="quantity" label="成品数量" width="100" align="right" />
      <el-table-column label="仓库" width="140">
        <template #default="{ row }">{{ warehouseName(row.warehouseId) }}</template>
      </el-table-column>
      <el-table-column prop="bizDate" label="日期" width="110" />
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

  <el-dialog v-model="dialogVisible" title="新增组装/拆卸" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="类型" required>
        <el-radio-group v-model="form.type">
          <el-radio-button value="assembly">组装</el-radio-button>
          <el-radio-button value="disassembly">拆卸</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="产品" required>
        <el-input :model-value="productName" placeholder="点击选择产品" readonly style="width: 100%">
          <template #append><el-button @click="productPickerVisible = true">选择</el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item label="仓库" required>
        <el-select v-model="form.warehouseId" placeholder="请选择" filterable style="width: 100%">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="成品数量" required>
        <el-input v-model="form.quantity" type="number" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <div class="tip">提示：按产品 BOM 自动展开子件并变动库存（组装=子件出/成品入，拆卸=成品出/子件入）</div>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="detailVisible" title="子件明细" width="560px">
    <el-table :data="detailItems" border size="small">
      <el-table-column prop="materialName" label="物料" width="140" />
      <el-table-column prop="quantity" label="数量" width="120" align="right" />
      <el-table-column prop="unit" label="单位" width="90" />
    </el-table>
  </el-dialog>

  <ProductPicker v-model="productPickerVisible" @select="onProduct" />

  <PrintDialog
    v-model="printVisible"
    title="组装/拆卸单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['制单人', '仓管员', '审核人']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Assembly' })
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { assemblyList, assemblyItems, assemblyCreate } from '@/api/inventory'
import { warehouseList } from '@/api/base'
import ProductPicker from '@/components/ProductPicker.vue'
import PrintDialog from '@/components/PrintDialog.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const warehouses = ref<any[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const productPickerVisible = ref(false)
const detailItems = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, type: undefined as string | undefined })
const form = reactive<any>({})

const productName = computed(() => form.productName || '')

function warehouseName(id: number) { return warehouses.value.find((w) => w.id === id)?.name || id }

function onProduct(p: any) {
  form.productId = p.id
  form.productName = `${p.code} ${p.name}`
}

async function loadWarehouses() {
  const res = await warehouseList({ page: 1, pageSize: 100, status: 1 })
  warehouses.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await assemblyList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.type = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  form.type = 'assembly'
  form.quantity = 1
  loadWarehouses()
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.type) { ElMessage.warning('请选择类型'); return }
  if (!form.productId) { ElMessage.warning('请选择产品'); return }
  if (!form.warehouseId) { ElMessage.warning('请选择仓库'); return }
  if (!form.quantity || Number(form.quantity) <= 0) { ElMessage.warning('数量必须大于0'); return }
  await assemblyCreate({
    requestId: crypto.randomUUID(),
    type: form.type, productId: form.productId, warehouseId: form.warehouseId,
    quantity: Number(form.quantity), remark: form.remark
  })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function viewItems(row: any) {
  detailItems.value = await assemblyItems(row.id)
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
  { label: '数量', prop: 'quantity', align: 'right' },
  { label: '单位', prop: 'unit', align: 'center' }
]

async function handlePrint(row: any) {
  printRows.value = await assemblyItems(row.id)
  printHead.value = [
    [{ label: '单号', value: row.orderNo }, { label: '日期', value: row.bizDate }],
    [{ label: '类型', value: row.type === 'assembly' ? '组装' : '拆卸' }, { label: '产品', value: row.productName }],
    [{ label: '成品数量', value: row.quantity }, { label: '仓库', value: warehouseName(row.warehouseId) }]
  ]
  printRemark.value = row.remark
  printVisible.value = true
}

loadWarehouses()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.tip { font-size: 12px; color: #909399; margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
