<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="供应商">
        <el-select v-model="query.supplierId" placeholder="全部" clearable filterable style="width: 200px">
          <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 130px">
          <el-option label="未结清" value="pending" />
          <el-option label="部分核销" value="partial" />
          <el-option label="已结清" value="settled" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="payableNo" label="应付单号" width="170" />
      <el-table-column prop="supplierName" label="供应商" min-width="160" />
      <el-table-column prop="invoiceNo" label="来源发票" width="170" />
      <el-table-column prop="amount" label="应付金额" width="120" align="right" />
      <el-table-column prop="writtenOffAmount" label="已核销" width="120" align="right" />
      <el-table-column prop="balance" label="余额" width="120" align="right" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
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
</template>

<script setup lang="ts">
defineOptions({ name: 'Payable' })
import { reactive, ref } from 'vue'
import { payableList } from '@/api/finance'
import { supplierList } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const suppliers = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, supplierId: undefined as number | undefined, status: undefined as string | undefined })

function statusText(v: string) { return { pending: '未结清', partial: '部分核销', settled: '已结清' }[v] || v }
function statusTagType(v: string) { return { pending: 'danger', partial: 'warning', settled: 'success' }[v] || 'info' }

async function loadSuppliers() {
  const res = await supplierList({ page: 1, pageSize: 200 })
  suppliers.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await payableList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.supplierId = undefined; query.status = undefined; query.page = 1; load() }

loadSuppliers()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
