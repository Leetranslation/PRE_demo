<template>
  <el-card shadow="never">
    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column
        v-for="col in columns"
        :key="col.prop"
        :prop="col.prop"
        :label="col.label"
        :width="col.width"
        :min-width="col.minWidth"
        :align="col.align"
      >
        <template v-if="col.formatter" #default="{ row }">
          {{ col.formatter(row) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">明细</el-button>
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
import { ref, reactive, onMounted, watch } from 'vue'
import type { PageResult } from '@/api/base'

const props = defineProps<{
  columns: Array<{ prop: string; label: string; width?: number; minWidth?: number; align?: string; formatter?: (row: any) => string }>
  fetchList: (params: any) => Promise<PageResult<any>>
  fetchItems: (id: number) => Promise<any[]>
  itemColumns?: Array<{ prop: string; label: string; width?: number; minWidth?: number; align?: string }>
}>()

const emit = defineEmits<{ (e: 'detail', items: any[]): void }>()

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10 })

async function load() {
  loading.value = true
  try {
    const res = await props.fetchList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function viewDetail(row: any) {
  const items = await props.fetchItems(row.id)
  emit('detail', items)
}

onMounted(load)
</script>

<style scoped>
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
