<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="装箱单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增装箱单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="packingNo" label="装箱单号" width="160" />
      <el-table-column prop="outboundId" label="出库单ID" width="90" />
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

  <el-dialog v-model="dialogVisible" title="新增装箱单" width="680px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>

    <el-divider content-position="left">装箱明细</el-divider>
    <div class="item-toolbar">
      <el-button type="primary" size="small" @click="addRow">添加一行</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column label="箱号" width="140">
        <template #default="{ row }"><el-input v-model="row.boxNo" size="small" /></template>
      </el-table-column>
      <el-table-column label="产品" min-width="140">
        <template #default="{ row }">
          <el-button size="small" @click="pickProduct(row)">{{ row.productName || '选产品' }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="120">
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

  <ProductPicker v-model="productPickerVisible" @select="onProductSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'Packing' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { packingList, packingCreate } from '@/api/sales'
import ProductPicker from '@/components/ProductPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const productPickerVisible = ref(false)
const currentRow = ref<any>(null)
const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const form = reactive<any>({ items: [] })

function addRow() {
  form.items.push({ boxNo: '', productId: null, productName: '', quantity: 1 })
}
function pickProduct(row: any) {
  currentRow.value = row
  productPickerVisible.value = true
}
function onProductSelect(product: any) {
  if (currentRow.value) {
    currentRow.value.productId = product.id
    currentRow.value.productName = `${product.code} ${product.name}`
  }
}

async function load() {
  loading.value = true
  try {
    const res = await packingList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.items || form.items.length === 0) { ElMessage.warning('请添加装箱明细'); return }
  await packingCreate({
    remark: form.remark,
    items: form.items.map((i: any) => ({ boxNo: i.boxNo, productId: i.productId, quantity: Number(i.quantity) }))
  })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

function viewItems(row: any) {
  ElMessage.info('装箱明细：' + row.packingNo)
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
