<script setup lang="ts">
/**
 * 单据表格选择器（关联上下游单据用）。
 * 不要用只有编号的下拉 —— 选工单的时候要看得见产品、数量、状态。
 *
 *   <DocPicker v-model="visible" title="选择生产工单"
 *     :columns="[{prop:'orderNo',label:'工单号',width:'170'},...]"
 *     :fetcher="fetchOrders" @select="onPicked" />
 */
import { computed, ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

export interface DocPickColumn {
  prop: string
  label: string
  width?: string
  minWidth?: string
  align?: 'left' | 'right' | 'center'
  /** 自定义显示（状态中文化等） */
  format?: (row: any) => string
}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title?: string
    columns: DocPickColumn[]
    /** 分页查询，返回 { records, total } */
    fetcher: (p: { page: number; pageSize: number; keyword: string }) => Promise<{ records: any[]; total: number }>
    keywordPlaceholder?: string
    width?: string
  }>(),
  { title: '选择单据', keywordPlaceholder: '输入关键字搜索', width: '900px' }
)

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'select', row: any): void
}>()

/** 单一数据源（同 MaterialPicker，别在内部另存一份） */
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

async function load() {
  loading.value = true
  try {
    const res = await props.fetcher({ page: page.value, pageSize: pageSize.value, keyword: keyword.value })
    records.value = res.records || []
    total.value = res.total || 0
  } catch {
    records.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onOpen() {
  keyword.value = ''
  page.value = 1
  load()
}

function search() {
  page.value = 1
  load()
}

function choose(row: any) {
  emit('select', row)
  visible.value = false
}

function cellText(c: DocPickColumn, row: any) {
  return c.format ? c.format(row) : row[c.prop] ?? ''
}
</script>

<template>
  <el-dialog v-model="visible" :title="title" :width="width" append-to-body @open="onOpen">
    <div class="pick-bar">
      <el-input
        v-model="keyword"
        :placeholder="keywordPlaceholder"
        clearable
        style="width: 300px"
        @keyup.enter="search"
        @clear="search"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="search">查询</el-button>
      <span class="pick-tip">双击行或点右侧「选择」</span>
    </div>

    <el-table
      :data="records"
      v-loading="loading"
      border
      size="small"
      height="380"
      highlight-current-row
      @row-dblclick="choose"
    >
      <el-table-column type="index" label="序号" width="56" align="center" />
      <el-table-column
        v-for="c in columns"
        :key="c.prop"
        :label="c.label"
        :width="c.width"
        :min-width="c.minWidth || (c.width ? undefined : 120)"
        :align="c.align || 'left'"
        show-overflow-tooltip
      >
        <template #default="{ row }">{{ cellText(c, row) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="72" align="center" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="choose(row)">选择</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pick-page"
      v-model:current-page="page"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="load"
      @current-change="load"
    />
  </el-dialog>
</template>

<style scoped>
.pick-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}
.pick-tip {
  font-size: 12px;
  color: #909399;
  margin-left: auto;
}
.pick-page {
  margin-top: 10px;
  justify-content: flex-end;
}
</style>
