<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="结果">
        <el-select v-model="query.result" placeholder="全部" clearable style="width: 140px">
          <el-option label="待判定" value="" />
          <el-option label="合格" value="qualified" />
          <el-option label="不合格" value="unqualified" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="doExport">导出</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="inspectionNo" label="质检单号" width="170" />
      <el-table-column prop="itemName" label="物料/产品" min-width="160" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="quantity" label="质检数量" width="110" align="right" />
      <el-table-column prop="inspectionDate" label="质检日期" width="120" />
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag v-if="row.result === 'qualified'" type="success">合格</el-tag>
          <el-tag v-else-if="row.result === 'unqualified'" type="danger">不合格</el-tag>
          <el-tag v-else type="info">待判定</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210" fixed="right">
        <template #default="{ row }">
          <template v-if="!row.result">
            <el-button link type="success" @click="judge(row, 'qualified')">合格</el-button>
            <el-button link type="danger" @click="judgeUnqualified(row)">不合格</el-button>
          </template>
          <el-button link type="primary" @click="handlePrint(row)">打印</el-button>
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
    title="质量检验单"
    :head-rows="printHead"
    :columns="printCols"
    :rows="printRows"
    :remark="printRemark"
    :sign-labels="['检验员', '复核人', '审核人']"
  />
</template>

<script setup lang="ts">
defineOptions({ name: 'Quality' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { qualityList, qualityJudge } from '@/api/purchase'
import { useExport } from '@/composables/useExport'
import PrintDialog from '@/components/PrintDialog.vue'

const RESULT_TEXT: Record<string, string> = {
  qualified: '合格',
  unqualified: '不合格'
}

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, pageSize: 10, result: undefined as string | undefined })

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'inspectionNo', label: '质检单号' },
    { prop: 'itemId', label: '物料/产品ID' },
    { prop: 'warehouseId', label: '仓库ID' },
    { prop: 'quantity', label: '质检数量' },
    { prop: 'inspectionDate', label: '质检日期' },
    { prop: 'result', label: '结果', formatter: (row: any) => RESULT_TEXT[row.result] || '待判定' }
  ],
  '质检单'
)
function doExport() { handleExport(records.value) }

async function load() {
  loading.value = true
  try {
    const res = await qualityList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }

async function judge(row: any, result: string) {
  await ElMessageBox.confirm(`确定判定为${result === 'qualified' ? '合格' : '不合格'}吗？`, '提示', { type: 'warning' })
  await qualityJudge(row.id, result)
  ElMessage.success('判定完成')
  load()
}

async function judgeUnqualified(row: any) {
  const { value } = await ElMessageBox.prompt('请输入不合格原因', '不合格判定', {
    confirmButtonText: '判定',
    cancelButtonText: '取消',
    inputPlaceholder: '如：尺寸不符'
  })
  const handleType = await chooseHandleType()
  if (!handleType) return
  await qualityJudge(row.id, 'unqualified', handleType, value)
  ElMessage.success('已判定不合格')
  load()
}

function chooseHandleType(): Promise<string | null> {
  return new Promise((resolve) => {
    ElMessageBox.confirm('选择处理方式：确定=退货，取消=报废', '不合格处理', {
      confirmButtonText: '退货',
      cancelButtonText: '报废',
      distinguishCancelAndClose: true,
      type: 'warning'
    }).then(() => resolve('return')).catch(() => resolve('scrap'))
  })
}

// ===== 打印 =====
const printVisible = ref(false)
const printHead = ref<any[][]>([])
const printRows = ref<any[]>([])
const printRemark = ref('')
const printCols = [
  { label: '项目', prop: 'item' },
  { label: '内容', prop: 'val' }
]

function handlePrint(row: any) {
  printHead.value = [
    [{ label: '质检单号', value: row.inspectionNo }, { label: '质检日期', value: row.inspectionDate }],
    [{ label: '物料/产品', value: row.itemName }, { label: '仓库', value: row.warehouseName }],
    [{ label: '质检数量', value: row.quantity }]
  ]
  printRows.value = [
    { item: '质检结果', val: row.result === 'qualified' ? '合格' : (row.result === 'unqualified' ? '不合格' : '待判定') }
  ]
  printRemark.value = row.remark
  printVisible.value = true
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
