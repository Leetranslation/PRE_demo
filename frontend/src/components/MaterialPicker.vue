<template>
  <el-dialog v-model="visible" title="选择物料" width="680px" @open="load">
    <el-form :inline="true">
      <el-form-item label="关键字">
        <el-input v-model="keyword" placeholder="物料名称/编号" clearable style="width: 220px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">搜索</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" border size="small" @row-click="select" highlight-current-row style="cursor: pointer">
      <el-table-column prop="code" label="物料编号" width="150" />
      <el-table-column prop="name" label="物料名称" min-width="150" />
      <el-table-column prop="spec" label="规格" width="120" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column label="供应属性" width="100">
        <template #default="{ row }">{{ supplyText(row.supplyType) }}</template>
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
import { computed, ref } from 'vue'
import { materialList } from '@/api/base'

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'select', material: any): void
}>()

/**
 * 🔴 单一数据源：直接代理父组件的值，**不在内部另存一份 visible**。
 * 之前的写法是本地 ref + 单向 watch —— 用户点右上角 X 关闭时，
 * 内部 visible 变 false 但从未通知父组件，父组件的 pickerVisible 仍是 true；
 * 之后再点「选择物料」传进来的还是 true，watch 不触发 → 弹窗再也打不开（已踩，严重）。
 */
const visible = computed({
  get: () => props.modelValue,
  set: (v: boolean) => emit('update:modelValue', v)
})

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)

function supplyText(v: string) {
  return { purchase: '采购', make: '自制', purchase_make: '采购+自制' }[v] || v
}

async function load() {
  loading.value = true
  try {
    const res = await materialList({ page: page.value, pageSize: pageSize.value, keyword: keyword.value, status: 1 })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function select(row: any) {
  emit('select', row)
  visible.value = false // 通过 setter 通知父组件，父子状态不会再分叉
}
</script>

<style scoped>
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
