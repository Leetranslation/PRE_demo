<template>
  <el-dialog v-model="visible" title="选择工序" width="680px" @open="load">
    <el-form :inline="true">
      <el-form-item label="关键字">
        <el-input v-model="keyword" placeholder="工序名称/编号" clearable style="width: 220px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" border size="small" @row-click="select" highlight-current-row style="cursor: pointer">
      <el-table-column prop="code" label="工序编号" width="150" />
      <el-table-column prop="name" label="工序名称" min-width="150" />
      <el-table-column prop="category" label="类别" width="100" />
      <el-table-column label="计价方式" width="100">
        <template #default="{ row }">{{ priceTypeText(row.priceType) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="70">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20]"
      layout="total, prev, pager, next"
      @size-change="load"
      @current-change="load"
    />
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { processList } from '@/api/base'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'select', process: any): void
}>()

const visible = ref(false)
const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)

watch(() => props.modelValue, (v) => { visible.value = v })

function priceTypeText(v: string) {
  return { piece: '计件', time: '计时', fixed: '固定价' }[v] || v
}

async function load() {
  loading.value = true
  try {
    const res = await processList({ page: page.value, pageSize: pageSize.value, keyword: keyword.value, status: 1 })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function select(row: any) {
  emit('select', row)
  visible.value = false
  emit('update:modelValue', false)
}
</script>

<style scoped>
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
