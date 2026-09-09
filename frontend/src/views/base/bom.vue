<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="名称/编号" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增 BOM</el-button>
      <el-button @click="doExport">导出</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="code" label="BOM编号" width="160" />
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '生效' : '失效' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
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

  <!-- 新增 BOM -->
  <el-dialog v-model="dialogVisible" title="新增 BOM" width="760px">
    <el-form :model="form" label-width="100px">
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="名称"><el-input v-model="form.name" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="版本"><el-input v-model="form.version" placeholder="如 V1.0" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="产品" required>
            <el-input v-model="form.productName" placeholder="点击选择产品" readonly style="width: 100%">
              <template #append>
                <el-button @click="openProductPickerForMain">选择</el-button>
              </template>
            </el-input>
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <div class="item-toolbar">
      <span>BOM 明细</span>
      <el-button size="small" type="primary" @click="addItem">添加明细</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column label="子项类型" width="110">
        <template #default="{ row }">
          <el-select v-model="row.childType" size="small">
            <el-option label="物料" value="material" />
            <el-option label="产品" value="product" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="子项" min-width="180">
        <template #default="{ row }">
          <div class="picker-cell">
            <span v-if="row.childId" class="picker-name">{{ row.childName }}</span>
            <span v-else class="picker-empty">未选择</span>
          </div>
          <el-button link size="small" type="primary" @click="openChildPicker(row)">
            {{ row.childId ? '更换' : '选择' }}
          </el-button>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="130">
        <template #default="{ row }">
          <el-input v-model="row.quantity" size="small" placeholder="数量" />
        </template>
      </el-table-column>
      <el-table-column label="单位" width="90">
        <template #default="{ row }">
          <el-input v-model="row.unit" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="损耗率" width="130">
        <template #default="{ row }">
          <el-input v-model="row.lossRate" size="small" placeholder="损耗率" />
        </template>
      </el-table-column>
      <el-table-column label="工序" width="120">
        <template #default="{ row }">
          <el-input v-model="row.process" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70">
        <template #default="{ $index }">
          <el-button link type="danger" @click="form.items.splice($index, 1)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <!-- 明细查看 -->
  <el-dialog v-model="itemsVisible" title="BOM 明细" width="760px">
    <el-table :data="items" border size="small">
      <el-table-column prop="childType" label="类型" width="100" />
      <el-table-column prop="childId" label="子项ID" width="100" />
      <el-table-column prop="quantity" label="数量" width="120" />
      <el-table-column prop="unit" label="单位" width="90" />
      <el-table-column prop="lossRate" label="损耗率" width="100" />
      <el-table-column prop="process" label="工序" min-width="120" />
    </el-table>
  </el-dialog>

  <!-- 物料/产品选择弹窗 -->
  <MaterialPicker v-model="materialPickerVisible" @select="onMaterialSelected" />
  <ProductPicker v-model="productPickerVisible" @select="onProductSelected" />
</template>

<script setup lang="ts">
defineOptions({ name: 'Bom' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { bomList, bomCreate, bomItems } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'
import ProductPicker from '@/components/ProductPicker.vue'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const itemsVisible = ref(false)
const items = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const form = reactive<any>({ items: [] })

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'code', label: 'BOM编号' },
    { prop: 'name', label: '名称' },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '生效' : '失效') }
  ],
  'BOM'
)
function doExport() { handleExport(records.value) }

// 选择器
const materialPickerVisible = ref(false)
const productPickerVisible = ref(false)
const currentChildRow = ref<any>(null)
const isMainProduct = ref(false)

function openProductPickerForMain() {
  isMainProduct.value = true
  productPickerVisible.value = true
}

function openChildPicker(row: any) {
  isMainProduct.value = false
  currentChildRow.value = row
  if (row.childType === 'product') {
    productPickerVisible.value = true
  } else {
    materialPickerVisible.value = true
  }
}

function onProductSelected(product: any) {
  if (isMainProduct.value) {
    form.productId = product.id
    form.productName = product.name
  } else if (currentChildRow.value) {
    currentChildRow.value.childId = product.id
    currentChildRow.value.childName = product.name
    currentChildRow.value.unit = product.unit
  }
}

function onMaterialSelected(material: any) {
  if (currentChildRow.value) {
    currentChildRow.value.childId = material.id
    currentChildRow.value.childName = material.name
    currentChildRow.value.unit = material.unit
  }
}

async function load() {
  loading.value = true
  try {
    const res = await bomList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.page = 1; load() }

function openDialog() {
  form.id = undefined
  form.name = ''
  form.version = ''
  form.productId = undefined
  form.productName = ''
  form.items = []
  dialogVisible.value = true
}

function addItem() {
  form.items.push({ childType: 'material', childId: undefined, childName: '', quantity: 0, unit: '', lossRate: 0, process: '' })
}

async function handleSave() {
  if (!form.productId) {
    ElMessage.warning('请选择产品')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请至少添加一条明细')
    return
  }
  for (const item of form.items) {
    if (!item.childId) {
      ElMessage.warning('存在未选择子项的明细行')
      return
    }
    const qty = Number(item.quantity)
    if (!qty || qty <= 0) {
      ElMessage.warning('存在数量未填或为0的明细行')
      return
    }
    item.quantity = qty
    item.lossRate = Number(item.lossRate) || 0
  }
  await bomCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function viewItems(row: any) {
  items.value = await bomItems(row.id)
  itemsVisible.value = true
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
.item-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 8px 0;
}
.picker-cell { display: flex; flex-direction: column; margin-bottom: 2px; }
.picker-name { font-weight: 500; color: #333; }
.picker-empty { font-size: 12px; color: #c0c4cc; }
</style>
