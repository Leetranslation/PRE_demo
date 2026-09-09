<template>
  <el-card shadow="never">
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="跟踪方式">
        <el-radio-group v-model="mode" @change="handleSearch">
          <el-radio-button value="batch">批号跟踪</el-radio-button>
          <el-radio-button value="serial">序列号跟踪</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="mode === 'batch' ? '批号' : '序列号'">
        <el-input v-model="keyword" placeholder="请输入" clearable style="width: 200px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="itemId" label="物料/产品ID" width="120" />
      <el-table-column prop="itemType" label="类型" width="90">
        <template #default="{ row }">{{ row.itemType === 'material' ? '物料' : '产品' }}</template>
      </el-table-column>
      <el-table-column prop="batchNo" label="批号" width="120" />
      <el-table-column prop="serialNo" label="序列号" width="140" />
      <el-table-column label="方向" width="80">
        <template #default="{ row }">
          <el-tag :type="row.direction === 'in' ? 'success' : 'warning'" size="small">
            {{ row.direction === 'in' ? '入库' : '出库' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="quantity" label="数量" width="100" align="right" />
      <el-table-column prop="balanceQty" label="结存" width="100" align="right" />
      <el-table-column prop="bizType" label="业务类型" width="140" />
      <el-table-column prop="bizNo" label="业务单号" width="160" />
      <el-table-column prop="createTime" label="时间" width="170" />
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
</template>

<script setup lang="ts">
defineOptions({ name: 'InventoryTrack' })
import { reactive, ref } from 'vue'
import { batchTrack, serialTrack } from '@/api/inventory'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const mode = ref('batch')
const keyword = ref('')
const query = reactive({ page: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, pageSize: query.pageSize, batchNo: undefined as string | undefined, serialNo: undefined as string | undefined }
    const res = mode.value === 'batch'
      ? await batchTrack({ ...params, batchNo: keyword.value || undefined })
      : await serialTrack({ ...params, serialNo: keyword.value || undefined })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
