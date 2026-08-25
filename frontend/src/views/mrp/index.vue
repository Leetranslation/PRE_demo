<template>
  <el-card shadow="never">
    <!-- 计算参数 -->
    <el-form :inline="true" class="filter-bar">
      <el-form-item label="计划开始日期">
        <el-date-picker v-model="range" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始" end-placeholder="结束" style="width: 260px" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleCalculate" :loading="calculating">执行 MRP 计算</el-button>
      </el-form-item>
    </el-form>

    <!-- 计划列表 -->
    <el-divider content-position="left">计划列表</el-divider>
    <el-table :data="plans" border stripe size="small" @row-click="selectPlan" highlight-current-row>
      <el-table-column prop="planNo" label="计划号" width="170" />
      <el-table-column prop="planDate" label="计划日期" width="120" />
      <el-table-column prop="startDate" label="开始" width="110" />
      <el-table-column prop="endDate" label="结束" width="110" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'confirmed' ? 'success' : 'info'">{{ row.status === 'confirmed' ? '已确认' : '已计算' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click.stop="selectPlan(row)">查看建议</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 建议列表 -->
    <template v-if="currentPlanId">
      <el-divider content-position="left">MRP 建议（计划 {{ currentPlanId }}）</el-divider>
      <div class="toolbar">
        <el-button type="primary" :disabled="!selectedReqs.length" @click="handleConfirm">确认生成采购/生产单（{{ selectedReqs.length }}）</el-button>
      </div>
      <el-table :data="requirements" border stripe size="small" @selection-change="onSelectionChange">
        <el-table-column type="selection" width="50" :selectable="canSelect" />
        <el-table-column prop="itemId" label="物料/产品ID" width="110" />
        <el-table-column label="类型" width="80">
          <template #default="{ row }">{{ row.itemType === 'material' ? '物料' : '产品' }}</template>
        </el-table-column>
        <el-table-column prop="grossQty" label="毛需求" width="100" align="right" />
        <el-table-column prop="availableQty" label="可用库存" width="100" align="right" />
        <el-table-column prop="inTransitQty" label="在途供应" width="100" align="right" />
        <el-table-column label="净需求" width="100" align="right">
          <template #default="{ row }">
            <span :style="{ color: Number(row.netQty) > 0 ? '#d32f2f' : '#999', fontWeight: Number(row.netQty) > 0 ? 'bold' : 'normal' }">{{ row.netQty }}</span>
          </template>
        </el-table-column>
        <el-table-column label="供应方式" width="110">
          <template #default="{ row }">{{ supplyText(row.supplyType) }}</template>
        </el-table-column>
        <el-table-column prop="suggestDate" label="建议日期" width="110" />
        <el-table-column prop="sourceNo" label="来源单据" width="160" />
        <el-table-column label="转换状态" width="100">
          <template #default="{ row }">{{ convertedText(row.convertedStatus) }}</template>
        </el-table-column>
      </el-table>
    </template>
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { mrpCalculate, mrpPlans, mrpRequirements, mrpConfirm } from '@/api/mrp'

const range = ref<[string, string] | null>(null)
const calculating = ref(false)
const plans = ref<any[]>([])
const requirements = ref<any[]>([])
const currentPlanId = ref<number | null>(null)
const selectedReqs = ref<any[]>([])

function supplyText(v: string) {
  return { purchase: '采购', make: '自制', purchase_make: '采购+自制' }[v] || v
}
function convertedText(v: string) {
  return { none: '未转换', purchase: '已转采购', make: '已转生产' }[v] || v
}
function canSelect(row: any) {
  return Number(row.netQty) > 0 && row.convertedStatus === 'none'
}
function onSelectionChange(rows: any[]) {
  selectedReqs.value = rows
}

async function loadPlans() {
  plans.value = await mrpPlans()
}

async function handleCalculate() {
  if (!range.value) { ElMessage.warning('请选择计划日期范围'); return }
  calculating.value = true
  try {
    const plan = await mrpCalculate({ startDate: range.value[0], endDate: range.value[1] })
    ElMessage.success('MRP 计算完成')
    await loadPlans()
    selectPlan(plan)
  } finally {
    calculating.value = false
  }
}

async function selectPlan(row: any) {
  currentPlanId.value = row.id
  requirements.value = await mrpRequirements(row.id)
}

async function handleConfirm() {
  await ElMessageBox.confirm(`确定生成 ${selectedReqs.value.length} 条采购/生产单吗？`, '确认', { type: 'warning' })
  await mrpConfirm({
    planId: currentPlanId.value,
    requirementIds: selectedReqs.value.map((r) => r.id)
  })
  ElMessage.success('已生成采购/生产单')
  requirements.value = await mrpRequirements(currentPlanId.value!)
  await loadPlans()
}

loadPlans()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
</style>
