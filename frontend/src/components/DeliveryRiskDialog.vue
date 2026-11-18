<script setup lang="ts">
/**
 * AI 交期风险智能预警 —— 评估结果弹窗（对应参考图 A1）。
 *
 * 职责边界：
 *   - 这里只负责「列出每行的风险 + 建议日期」，以及「采纳建议」
 *   - 点「查看分析」把行抛给父组件，由 DeliveryRiskAnalysis 抽屉跑流式分析
 *     （弹窗不销毁，抽屉叠在上面，和参考图一致）
 */
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { evaluateRiskInline, type RiskRow, type InlineItem } from '@/api/deliveryRisk'

const props = defineProps<{
  /** 当前单据的明细（内存数据；新建时还没入库，所以走无状态评估） */
  items: InlineItem[]
}>()

const visible = defineModel<boolean>({ default: false })
const emit = defineEmits<{
  (e: 'analyze', row: RiskRow): void
  (e: 'apply', rows: RiskRow[]): void
}>()

const loading = ref(false)
const rows = ref<RiskRow[]>([])
const checked = ref<number[]>([])

const riskyCount = computed(() => rows.value.filter((r) => r.riskLevel !== 'on_time').length)
const allChecked = computed(
  () => rows.value.length > 0 && checked.value.length === rows.value.length
)

const LEVEL_TYPE: Record<string, 'success' | 'warning' | 'danger'> = {
  on_time: 'success',
  at_risk: 'warning',
  late: 'danger'
}

async function load() {
  loading.value = true
  try {
    rows.value = await evaluateRiskInline(props.items)
    // 默认勾选有风险的行 —— 用户点「确定」多半就是想改这些
    checked.value = rows.value.filter((r) => r.riskLevel !== 'on_time').map((r) => r.itemId)
  } catch (e: any) {
    ElMessage.error('交期评估失败：' + (e?.message || '未知错误'))
    rows.value = []
  } finally {
    loading.value = false
  }
}

watch(visible, (v) => {
  if (v) load()
})

function toggleAll(v: any) {
  checked.value = v ? rows.value.map((r) => r.itemId) : []
}

function toggleRow(id: number) {
  const s = new Set(checked.value)
  if (s.has(id)) s.delete(id)
  else s.add(id)
  checked.value = [...s]
}

/**
 * 采纳建议：把勾选行的建议日期交给父组件写回表单明细。
 * ⚠️ 这里不调后端 —— 新建单据还没入库，直接改内存表单即可。
 */
function confirmApply() {
  const picked = rows.value.filter((r) => checked.value.includes(r.itemId) && r.suggestDate)
  if (!picked.length) {
    ElMessage.warning('所选明细没有可采纳的建议日期')
    return
  }
  emit('apply', picked)
  visible.value = false
}
</script>

<template>
  <el-dialog v-model="visible" title="AI交期风险智能预警" width="900px" :close-on-click-modal="false">
    <p class="dr-intro">
      AI 销售助手基于历史数据与外部因素，实时跟踪生产与物流动态，经过智能交期预警大模型的实时运算分析，
      针对本次订单中产品的逾期风险评估如下，请问是否采纳 AI 建议交付日期并替换？
    </p>

    <el-table :data="rows" v-loading="loading" border size="small" class="dr-table">
      <el-table-column width="46" align="center">
        <template #header>
          <el-checkbox
            :model-value="allChecked"
            :indeterminate="checked.length > 0 && !allChecked"
            @change="toggleAll"
          />
        </template>
        <template #default="{ row }">
          <el-checkbox
            :model-value="checked.includes(row.itemId)"
            @change="toggleRow(row.itemId)"
          />
        </template>
      </el-table-column>
      <el-table-column type="index" label="序号" width="56" align="center" />
      <el-table-column prop="productName" label="产品名称" min-width="150" show-overflow-tooltip />
      <el-table-column prop="productCode" label="产品编码" width="140" show-overflow-tooltip />
      <el-table-column prop="quantity" label="基本数量" width="96" align="right" />
      <el-table-column label="交付日期" width="110">
        <template #default="{ row }">{{ row.deliveryDate || '未填' }}</template>
      </el-table-column>
      <el-table-column label="智能交期风险评估" width="130" align="center">
        <template #default="{ row }">
          <el-tag :type="LEVEL_TYPE[row.riskLevel] || 'info'" size="small" effect="light">
            {{ row.riskText }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="AI建议交付日期" width="130" align="center">
        <template #default="{ row }">
          <span :class="{ 'dr-suggest': row.suggestDate }">{{ row.suggestDate || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="AI分析" width="96" align="center">
        <template #default="{ row }">
          <el-button link type="danger" size="small" @click="emit('analyze', row)">
            查看分析
          </el-button>
        </template>
      </el-table-column>
      <template #empty>
        <div class="dr-empty">没有可评估的明细（需「数量 > 0」且「已填交付日期」）</div>
      </template>
    </el-table>

    <div v-if="rows.length" class="dr-sum">
      共 {{ rows.length }} 行，其中 <b>{{ riskyCount }}</b> 行存在交付风险
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :disabled="!rows.length" @click="confirmApply">确定</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dr-intro {
  margin: 0 0 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
}
.dr-table :deep(th.el-table__cell) {
  background: #fdf2f2;
  color: #8c4a4a;
  font-weight: 500;
}
.dr-suggest {
  color: #d32f2f;
  font-weight: 600;
}
.dr-sum {
  margin-top: 10px;
  font-size: 12px;
  color: #909399;
}
.dr-empty {
  padding: 18px 0;
  color: #909399;
}
</style>
