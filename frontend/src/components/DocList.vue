<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button @click="doExport">导出</el-button>
    </div>
    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
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
      <el-table-column label="操作" :width="printTitle ? 150 : 100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="viewDetail(row)">明细</el-button>
          <el-button v-if="printTitle" link type="primary" @click="handlePrint(row)">打印</el-button>
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

  <PrintDialog
    v-model="printVisible"
    :title="printTitle || '单据'"
    :head-rows="printHead"
    :columns="printCols && printCols.length ? printCols : columns"
    :rows="printRows"
    :total-label="printTotalProp ? '合计金额' : ''"
    :total-value="printTotalValue"
    :remark="printRemark"
    :show-upper="printShowUpper"
    :total-amount="printTotalValue"
    :sign-labels="printSignLabels"
  />
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { exportExcel, type ExportColumn } from '@/utils/export'
import type { PageResult } from '@/api/base'
import PrintDialog from '@/components/PrintDialog.vue'

const props = defineProps<{
  columns: Array<{ prop: string; label: string; width?: number; minWidth?: number; align?: string; formatter?: (row: any) => string }>
  fetchList: (params: any) => Promise<PageResult<any>>
  fetchItems: (id: number) => Promise<any[]>
  filename?: string
  printTitle?: string
  printHeadFn?: (row: any) => any[][]
  printCols?: any[]
  printTotalProp?: string
  printShowUpper?: boolean
  printSignLabels?: string[]
  printRemarkProp?: string
}>()

const emit = defineEmits<{ (e: 'detail', items: any[]): void }>()

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const selection = ref<any[]>([])
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

function onSelectionChange(rows: any[]) {
  selection.value = rows
}

function doExport() {
  const data = selection.value.length ? selection.value : records.value
  if (!data.length) {
    ElMessage.warning('没有可导出的数据')
    return
  }
  const exportCols: ExportColumn[] = props.columns.map((c) => ({
    prop: c.prop,
    label: c.label,
    formatter: c.formatter
  }))
  exportExcel(exportCols, data, props.filename || '导出')
  ElMessage.success(`已导出 ${data.length} 条数据`)
}

const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printTotalValue = ref<any>('')

async function handlePrint(row: any) {
  printRows.value = await props.fetchItems(row.id)
  printHead.value = props.printHeadFn ? props.printHeadFn(row) : []
  printRemark.value = props.printRemarkProp ? row[props.printRemarkProp] : ''
  printTotalValue.value = props.printTotalProp ? row[props.printTotalProp] : ''
  printVisible.value = true
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
