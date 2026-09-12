<template>
  <div>
    <!-- 9 个 KPI -->
    <el-row :gutter="12">
      <el-col :span="8" v-for="k in kpis" :key="k.label" class="kpi-col">
        <el-card shadow="hover" class="kpi-card">
          <div class="kpi-label">{{ k.label }}</div>
          <div class="kpi-value">{{ k.value }}<span class="kpi-unit">{{ k.unit }}</span></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办中心 + 图表 -->
    <el-row :gutter="16" class="section">
      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>待办中心</span></template>
          <div v-for="t in stats.todos || []" :key="t.name" class="todo-row" @click="goTo(t.path)">
            <span class="todo-name">{{ t.name }}</span>
            <el-badge :value="t.count" :type="t.count > 0 ? 'danger' : 'info'" :show-zero="true" />
          </div>
          <el-empty v-if="!(stats.todos || []).length" description="暂无待办" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>销售趋势（近 6 月）</span></template>
          <div class="chart">
            <div v-for="t in stats.salesTrend || []" :key="t.month" class="bar-wrap">
              <div class="bar" :style="{ height: barHeight(t.amount) + 'px' }"></div>
              <div class="bar-label">{{ t.month.slice(5) }}月</div>
            </div>
            <el-empty v-if="!(stats.salesTrend || []).length" description="暂无数据" :image-size="60" />
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never">
          <template #header><span>库存分布</span></template>
          <div v-for="s in stats.stockDistribution || []" :key="s.name" class="stock-row">
            <span class="stock-name">{{ s.name }}</span>
            <span class="stock-value">{{ Number(s.quantity).toFixed(2) }}</span>
          </div>
          <el-empty v-if="!(stats.stockDistribution || []).length" description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Cockpit' })
import { reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardCockpit } from '@/api/dashboard'

const router = useRouter()
const stats = reactive<any>({})

const maxTrend = computed(() => {
  const arr = (stats.salesTrend || []).map((t: any) => Number(t.amount) || 0)
  return Math.max(1, ...arr)
})

function barHeight(amount: any) {
  const v = Number(amount) || 0
  return Math.round((v / maxTrend.value) * 120) + 8
}

const kpis = computed(() => [
  { label: '销售订单总金额', value: fmtAmount(stats.salesOrderAmount), unit: '元' },
  { label: '销售订单准时率', value: fmtRate(stats.salesOnTimeRate), unit: '%' },
  { label: '工单生产准时率', value: fmtRate(stats.productionOnTimeRate), unit: '%' },
  { label: '生产总产值（完工数）', value: fmtAmount(stats.productionOutput), unit: '' },
  { label: '采购订单总金额', value: fmtAmount(stats.purchaseOrderAmount), unit: '元' },
  { label: '应付 / 已付金额', value: `${fmtAmount(stats.payableAmount)} / ${fmtAmount(stats.paidAmount)}`, unit: '元' },
  { label: '库存周转率', value: fmtRate(stats.inventoryTurnover), unit: '' },
  { label: '物料库存金额（数量）', value: fmtAmount(stats.materialStockAmount), unit: '' },
  { label: '成品库存金额（数量）', value: fmtAmount(stats.productStockAmount), unit: '' }
])

function fmtAmount(v: any) {
  if (v === undefined || v === null) return '0'
  return Number(v).toLocaleString('zh-CN', { maximumFractionDigits: 2 })
}
function fmtRate(v: any) {
  if (v === undefined || v === null) return '0'
  return Number(v).toFixed(2)
}

function goTo(path: string) {
  if (path) router.push(path)
}

async function load() {
  const res = await dashboardCockpit()
  Object.assign(stats, res)
}

onMounted(load)
</script>

<style scoped>
.kpi-col { margin-bottom: 12px; }
.kpi-card { border-left: 3px solid #d32f2f; }
.kpi-label { font-size: 13px; color: #909399; }
.kpi-value { font-size: 22px; font-weight: bold; color: #303133; margin-top: 6px; }
.kpi-unit { font-size: 12px; font-weight: normal; color: #909399; margin-left: 4px; }
.section { margin-top: 4px; }
.todo-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 4px; border-bottom: 1px solid #f0f0f0; cursor: pointer; }
.todo-row:hover { background: #fafafa; }
.todo-name { font-size: 14px; color: #303133; }
.chart { display: flex; align-items: flex-end; gap: 12px; height: 170px; padding-top: 10px; }
.bar-wrap { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: flex-end; height: 100%; }
.bar { width: 60%; background: linear-gradient(180deg, #e57373, #d32f2f); border-radius: 3px 3px 0 0; min-height: 8px; }
.bar-label { font-size: 12px; color: #909399; margin-top: 6px; }
.stock-row { display: flex; justify-content: space-between; padding: 12px 4px; border-bottom: 1px solid #f0f0f0; }
.stock-name { font-size: 14px; color: #303133; }
.stock-value { font-size: 16px; font-weight: bold; color: #d32f2f; }
</style>
