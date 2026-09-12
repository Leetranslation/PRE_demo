<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="单号">
        <el-input v-model="query.keyword" placeholder="设计单号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="设计中" value="designing" />
          <el-option label="已完成" value="finished" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增设计单</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="designNo" label="设计单号" width="160" />
      <el-table-column prop="productCode" label="产品编号" width="150" show-overflow-tooltip />
      <el-table-column prop="productName" label="产品名称" min-width="140" show-overflow-tooltip />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'finished' ? 'success' : 'warning'" size="small">
            {{ row.status === 'finished' ? '已完成' : '设计中' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'designing'" link type="success" @click="changeStatus(row, 'finished')">设计完成</el-button>
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

  <el-dialog v-model="dialogVisible" title="新增设计单" width="520px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="产品" required>
        <div style="display: flex; gap: 8px; width: 100%">
          <el-input :model-value="productLabel" placeholder="点击右侧选择产品" readonly />
          <el-button @click="productPickerVisible = true">选择产品</el-button>
        </div>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <ProductPicker v-model="productPickerVisible" @select="onProductSelect" />
</template>

<script setup lang="ts">
defineOptions({ name: 'DesignCenter' })
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { designCenterList, designCenterCreate, designCenterStatus } from '@/api/sales'
import ProductPicker from '@/components/ProductPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const productPickerVisible = ref(false)
const query = reactive({ page: 1, pageSize: 10, keyword: '', status: undefined as string | undefined })
const form = reactive<any>({})

const productLabel = computed(() => form.productName || '')

function onProductSelect(product: any) {
  form.productId = product.id
  form.productName = `${product.code} ${product.name}`
}

async function load() {
  loading.value = true
  try {
    const res = await designCenterList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.status = undefined; query.page = 1; load() }

function openDialog() {
  Object.keys(form).forEach((k) => delete form[k])
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.productId) { ElMessage.warning('请选择产品'); return }
  await designCenterCreate({ productId: form.productId, remark: form.remark })
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function changeStatus(row: any, status: string) {
  await designCenterStatus(row.id, status)
  ElMessage.success('状态已更新')
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
