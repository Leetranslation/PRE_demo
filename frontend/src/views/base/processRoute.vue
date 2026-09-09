<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="名称/编号" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增</el-button>
      <el-button @click="doExport">导出</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="code" label="路线编号" width="160" />
      <el-table-column prop="name" label="路线名称" min-width="160" />
      <el-table-column prop="version" label="版本" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link type="primary" @click="openDetail(row)">查看工序</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="removeRow(row)">删除</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑工艺路线' : '新增工艺路线'" width="760px">
    <el-form :model="form" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="路线名称" required><el-input v-model="form.name" /></el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="版本"><el-input v-model="form.version" placeholder="如 V1.0" /></el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="产品" required>
            <div style="display: flex; gap: 8px; width: 100%">
              <el-input :model-value="productLabel" placeholder="点击右侧选择产品" readonly />
              <el-button @click="productPickerVisible = true">选择产品</el-button>
            </div>
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-divider content-position="left">工序明细</el-divider>
    <div class="item-toolbar">
      <el-button type="primary" size="small" @click="processPickerVisible = true">添加工序</el-button>
    </div>
    <el-table :data="form.items" border size="small">
      <el-table-column label="顺序" width="70">
        <template #default="{ $index }">{{ $index + 1 }}</template>
      </el-table-column>
      <el-table-column prop="processName" label="工序名称" min-width="140" />
      <el-table-column label="标准工时(小时)" width="140">
        <template #default="{ row }">
          <el-input v-model="row.standardHours" size="small" placeholder="标准工时" />
        </template>
      </el-table-column>
      <el-table-column label="关键工序" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.isKey" :active-value="1" :inactive-value="0" />
        </template>
      </el-table-column>
      <el-table-column label="检验工序" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.isQc" :active-value="1" :inactive-value="0" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ $index }">
          <el-button link type="danger" @click="removeItem($index)">移除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="detailVisible" title="工序明细" width="680px">
    <el-table :data="detailItems" border size="small">
      <el-table-column label="顺序" width="70">
        <template #default="{ $index }">{{ $index + 1 }}</template>
      </el-table-column>
      <el-table-column prop="processName" label="工序名称" min-width="140" />
      <el-table-column prop="standardHours" label="标准工时" width="120" />
      <el-table-column label="关键工序" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isKey === 1" type="warning" size="small">关键</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="检验工序" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.isQc === 1" type="danger" size="small">检验</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <ProductPicker v-model="productPickerVisible" @select="onProductSelect" />
  <ProcessPicker v-model="processPickerVisible" @select="onProcessSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'ProcessRoute' })
import { reactive, ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { processRouteList, processRouteItems, processRouteCreate, processRouteUpdate, processRouteStatus, processRouteDelete } from '@/api/base'
import ProductPicker from '@/components/ProductPicker.vue'
import ProcessPicker from '@/components/ProcessPicker.vue'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailItems = ref<any[]>([])
const productPickerVisible = ref(false)
const processPickerVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as number | undefined })
const form = reactive<any>({ items: [] })

const productLabel = computed(() => form.productName || '')

const { handleExport } = useExport(
  [
    { prop: 'code', label: '路线编号' },
    { prop: 'name', label: '路线名称' },
    { prop: 'version', label: '版本' },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '工艺路线'
)
function doExport() { handleExport(records.value) }

async function load() {
  loading.value = true
  try {
    const res = await processRouteList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.status = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  form.items = []
  if (row) {
    Object.assign(form, row)
    form.items = []
  }
  dialogVisible.value = true
}

async function openDetail(row: any) {
  detailItems.value = await processRouteItems(row.id)
  detailVisible.value = true
}

function onProductSelect(product: any) {
  form.productId = product.id
  form.productName = `${product.code} ${product.name}`
}

function onProcessSelect(process: any) {
  if (form.items.some((i: any) => i.processId === process.id)) {
    ElMessage.warning('该工序已添加')
    return
  }
  form.items.push({
    processId: process.id,
    processName: process.name,
    seqNo: form.items.length + 1,
    standardHours: undefined,
    isKey: 0,
    isQc: 0,
    remark: ''
  })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

async function handleSave() {
  if (!form.name || !form.productId) {
    ElMessage.warning('请填写名称并选择产品')
    return
  }
  const payload = {
    id: form.id,
    name: form.name,
    productId: form.productId,
    version: form.version,
    remark: form.remark,
    items: form.items.map((i: any, idx: number) => ({
      id: i.id,
      processId: i.processId,
      seqNo: idx + 1,
      standardHours: i.standardHours,
      isKey: i.isKey,
      isQc: i.isQc,
      remark: i.remark
    }))
  }
  if (form.id) {
    await processRouteUpdate(payload)
  } else {
    await processRouteCreate(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定要${action}该工艺路线吗？`, '提示', { type: 'warning' })
  await processRouteStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

async function removeRow(row: any) {
  await ElMessageBox.confirm(`确定要删除工艺路线「${row.name}」吗？`, '提示', { type: 'warning' })
  await processRouteDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.item-toolbar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
