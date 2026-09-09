<template>
  <el-card shadow="never">
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="月份">
        <el-date-picker v-model="yearMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" style="width: 160px" @change="load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openBatch">批量设置</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="workDate" label="日期" width="140" />
      <el-table-column label="星期" width="100">
        <template #default="{ row }">{{ weekText(row.workDate) }}</template>
      </el-table-column>
      <el-table-column label="是否工作日" width="120">
        <template #default="{ row }">
          <el-tag :type="row.isWorkday === 1 ? 'success' : 'danger'">{{ row.isWorkday === 1 ? '工作日' : '休息日' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[31, 62]"
      layout="total, sizes, prev, pager, next"
      @size-change="load"
      @current-change="load"
    />
  </el-card>

  <el-dialog v-model="batchVisible" title="批量设置工作日" width="460px">
    <el-form :model="batchForm" label-width="120px">
      <el-form-item label="月份" required>
        <el-date-picker v-model="batchForm.yearMonth" type="month" value-format="YYYY-MM" placeholder="选择月份" style="width: 100%" />
      </el-form-item>
      <el-form-item label="是否工作日" required>
        <el-radio-group v-model="batchForm.isWorkday">
          <el-radio :value="1">工作日</el-radio>
          <el-radio :value="0">休息日</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="batchForm.remark" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="batchVisible = false">取消</el-button>
      <el-button type="primary" @click="handleBatch">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'WorkCalendar' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { workCalendarList, workCalendarBatchSet } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const batchVisible = ref(false)
const yearMonth = ref('')
const query = reactive({ page: 1, pageSize: 31 })
const batchForm = reactive<any>({ yearMonth: '', isWorkday: 1, remark: '' })

function weekText(date: string) {
  const d = new Date(date)
  const weeks = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
  return weeks[d.getDay()]
}

async function load() {
  loading.value = true
  try {
    const res = await workCalendarList({ page: query.page, pageSize: query.pageSize, yearMonth: yearMonth.value || undefined })
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function openBatch() {
  batchForm.yearMonth = yearMonth.value || ''
  batchForm.isWorkday = 1
  batchForm.remark = ''
  batchVisible.value = true
}

async function handleBatch() {
  if (!batchForm.yearMonth) {
    ElMessage.warning('请选择月份')
    return
  }
  await workCalendarBatchSet({ yearMonth: batchForm.yearMonth, isWorkday: batchForm.isWorkday, remark: batchForm.remark })
  ElMessage.success('批量设置成功')
  batchVisible.value = false
  load()
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
