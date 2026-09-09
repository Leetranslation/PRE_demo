<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="客户">
        <el-select v-model="query.customerId" placeholder="全部" clearable filterable style="width: 180px">
          <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="query.itemType" placeholder="全部" clearable style="width: 120px">
          <el-option label="产品" value="product" />
          <el-option label="物料" value="material" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增价格</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column label="客户" min-width="150">
        <template #default="{ row }">{{ customerName(row.customerId) }}</template>
      </el-table-column>
      <el-table-column label="产品/物料" min-width="150">
        <template #default="{ row }">{{ itemName(row) }}</template>
      </el-table-column>
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ row.itemType === 'product' ? '产品' : '物料' }}</template>
      </el-table-column>
      <el-table-column prop="price" label="销售单价" width="120" align="right" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="130" show-overflow-tooltip />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑价格' : '新增价格'" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="客户" required>
        <el-select v-model="form.customerId" placeholder="请选择客户" filterable :disabled="!!form.id" style="width: 100%">
          <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型" required>
        <el-radio-group v-model="form.itemType" :disabled="!!form.id">
          <el-radio-button value="product">产品</el-radio-button>
          <el-radio-button value="material">物料</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="产品/物料" required>
        <el-input :model-value="form.itemName" placeholder="点击选择" readonly style="width: 100%">
          <template #append>
            <el-button @click="form.itemType === 'material' ? (materialPickerVisible = true) : (productPickerVisible = true)">选择</el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item label="销售单价" required>
        <el-input v-model="form.price" type="number" style="width: 100%" />
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <ProductPicker v-model="productPickerVisible" @select="onProduct" />
  <MaterialPicker v-model="materialPickerVisible" @select="onMaterial" />
</template>

<script setup lang="ts">
defineOptions({ name: 'CustomerPrice' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { customerPriceList, customerPriceCreate, customerPriceUpdate, customerPriceStatus, customerPriceDelete, customerList, productList, materialList } from '@/api/base'
import ProductPicker from '@/components/ProductPicker.vue'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const customers = ref<any[]>([])
const products = ref<any[]>([])
const materials = ref<any[]>([])
const dialogVisible = ref(false)
const productPickerVisible = ref(false)
const materialPickerVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, customerId: undefined as number | undefined, itemType: undefined as string | undefined })
const form = reactive<any>({})

function customerName(id: number) { return customers.value.find((c) => c.id === id)?.name || `#${id}` }
function itemName(row: any) {
  if (row.itemType === 'product') {
    const p = products.value.find((x) => x.id === row.itemId)
    return p ? `${p.code} ${p.name}` : `#${row.itemId}`
  }
  const m = materials.value.find((x) => x.id === row.itemId)
  return m ? `${m.code} ${m.name}` : `#${row.itemId}`
}

async function loadBase() {
  const [c, p, m] = await Promise.all([
    customerList({ page: 1, pageSize: 200, status: 1 }),
    productList({ page: 1, pageSize: 200, status: 1 }),
    materialList({ page: 1, pageSize: 200, status: 1 })
  ])
  customers.value = c.records
  products.value = p.records
  materials.value = m.records
}

async function load() {
  loading.value = true
  try {
    const res = await customerPriceList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.customerId = undefined; query.itemType = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) {
    Object.assign(form, row)
    form.itemName = itemName(row)
  } else {
    form.itemType = 'product'
  }
  dialogVisible.value = true
}

function onProduct(p: any) { form.itemId = p.id; form.itemName = `${p.code} ${p.name}` }
function onMaterial(m: any) { form.itemId = m.id; form.itemName = `${m.code} ${m.name}` }

async function handleSave() {
  if (!form.customerId || !form.itemId) { ElMessage.warning('请选择客户和产品/物料'); return }
  if (form.price === undefined || form.price === null || form.price === '') { ElMessage.warning('请填写价格'); return }
  const data = { id: form.id, customerId: form.customerId, itemId: form.itemId, itemType: form.itemType || 'product', price: Number(form.price), remark: form.remark }
  if (form.id) await customerPriceUpdate(data)
  else await customerPriceCreate(data)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定${action}该价格吗？`, '提示', { type: 'warning' })
  await customerPriceStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

async function doDelete(row: any) {
  await ElMessageBox.confirm('确定删除该价格记录吗？', '提示', { type: 'warning' })
  await customerPriceDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

loadBase()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
