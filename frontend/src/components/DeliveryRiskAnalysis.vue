<script setup lang="ts">
/**
 * 交期风险分析抽屉（对应参考图 A2→A4）。
 *
 * 关键点：**逐步加载**，不是一次性全跳出来。
 * 后端 `/analyze-stream` 按阶段推 SSE（规划任务 → 意图理解 → 输出清单 → BOM → MRP
 * → 库存 → 周期 → 风险判定 → 报告），这里来一条渲染一条。
 * 每条里的数字都是后端真实算的，不是假进度。
 */
import { onBeforeUnmount, ref, watch } from 'vue'
import {
  analyzeRiskInlineStream,
  type AnalysisStep,
  type ReportSection,
  type InlineItem
} from '@/api/deliveryRisk'

const props = defineProps<{
  /** 与弹窗同一份明细数据（内存） */
  items: InlineItem[]
}>()

const visible = defineModel<boolean>({ default: false })

const steps = ref<AnalysisStep[]>([])
const report = ref<{ sections: ReportSection[]; summary?: any } | null>(null)
const running = ref(false)
const errorMsg = ref('')
let ctrl: AbortController | null = null

function reset() {
  steps.value = []
  report.value = null
  errorMsg.value = ''
  running.value = false
}

async function start() {
  reset()
  running.value = true
  ctrl = new AbortController()
  try {
    await analyzeRiskInlineStream(
      props.items,
      (type, data) => {
        if (type === 'step') {
          const i = steps.value.findIndex((s) => s.key === data.key)
          const next: AnalysisStep = {
            key: data.key,
            name: data.name,
            status: data.status,
            message: data.message
          }
          // 同一阶段先 running 后 ok：原地替换，不新增一行
          if (i >= 0) steps.value.splice(i, 1, next)
          else steps.value.push(next)
        } else if (type === 'report') {
          report.value = data
        } else if (type === 'error') {
          errorMsg.value = data?.message || '分析失败'
        }
      },
      ctrl.signal
    )
  } catch (e: any) {
    if (e?.name !== 'AbortError') {
      errorMsg.value = e?.message || '分析失败'
    }
  } finally {
    running.value = false
    ctrl = null
  }
}

watch(visible, (v) => {
  if (v) start()
  else {
    ctrl?.abort()
    ctrl = null
  }
})

onBeforeUnmount(() => ctrl?.abort())
</script>

<template>
  <el-drawer
    v-model="visible"
    direction="rtl"
    size="640px"
    :with-header="false"
    append-to-body
    class="dra"
  >
    <div class="dra__page">
      <header class="dra__head">
        <span class="dra__brand">时光星AI</span>
        <button class="dra__close" @click="visible = false">✕</button>
      </header>

      <div class="dra__body">
        <div class="dra__title">
          <span>AI Agent 开始规划任务</span>
          <span v-if="running" class="dra__badge">运行中…</span>
        </div>

        <!-- 逐步出现的阶段 -->
        <div v-for="s in steps" :key="s.key" class="step">
          <div class="step__head">
            <span class="step__dot" :class="s.status" />
            <span class="step__name">{{ s.name }}</span>
          </div>
          <div v-if="s.message" class="step__msg">{{ s.message }}</div>
        </div>

        <div v-if="errorMsg" class="dra__err">分析失败：{{ errorMsg }}</div>

        <!-- 报告 -->
        <div v-if="report" class="report">
          <h3 class="report__title">交期风险分析报告</h3>
          <section v-for="(sec, i) in report.sections" :key="i" class="report__sec">
            <h4>{{ sec.title }}</h4>
            <p>{{ sec.body }}</p>
          </section>
        </div>

        <div v-if="running && !report" class="dra__loading">
          <i class="dot" /><i class="dot" /><i class="dot" />
        </div>
      </div>
    </div>
  </el-drawer>
</template>

<style scoped>
.dra__page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fdf8f8;
}
.dra__head {
  flex: none;
  height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 18px;
  border-bottom: 1px solid #f0e4e4;
  background: #fff;
}
.dra__brand {
  font-size: 15px;
  font-weight: 700;
  color: #d32f2f;
  letter-spacing: 0.5px;
}
.dra__close {
  border: none;
  background: none;
  font-size: 15px;
  color: #999;
  cursor: pointer;
}
.dra__body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 18px 28px;
}
.dra__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #2c2c2a;
  margin-bottom: 14px;
}
.dra__badge {
  font-size: 11px;
  font-weight: 400;
  color: #d32f2f;
  background: #fdeaea;
  border-radius: 10px;
  padding: 2px 8px;
}

/* ---------- 阶段 ---------- */
.step {
  margin-bottom: 12px;
}
.step__head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.step__dot {
  flex: none;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c8c9cc;
}
.step__dot.running {
  background: #d32f2f;
  box-shadow: 0 0 0 3px rgba(211, 47, 47, 0.15);
  animation: pulse 1.1s ease-in-out infinite;
}
.step__dot.ok {
  background: #52c41a;
}
.step__dot.error {
  background: #f56c6c;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}
.step__name {
  font-size: 13.5px;
  font-weight: 600;
  color: #2c2c2a;
}
.step__msg {
  margin: 6px 0 0 16px;
  padding: 10px 12px;
  background: #fff;
  border: 1px solid #f0e4e4;
  border-radius: 6px;
  font-size: 12.5px;
  line-height: 1.75;
  color: #55534e;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ---------- 报告 ---------- */
.report {
  margin-top: 18px;
  padding-top: 14px;
  border-top: 1px dashed #e8d7d7;
}
.report__title {
  margin: 0 0 12px;
  font-size: 15px;
  font-weight: 700;
  color: #2c2c2a;
}
.report__sec {
  margin-bottom: 16px;
}
.report__sec h4 {
  margin: 0 0 6px;
  font-size: 13.5px;
  font-weight: 600;
  color: #d32f2f;
}
.report__sec p {
  margin: 0;
  font-size: 13px;
  line-height: 1.85;
  color: #444441;
  white-space: pre-wrap;
}

.dra__err {
  margin-top: 10px;
  padding: 10px 12px;
  background: #fcebeb;
  border: 1px solid #f09595;
  border-radius: 6px;
  font-size: 12.5px;
  color: #791f1f;
}
.dra__loading {
  display: flex;
  gap: 5px;
  padding: 10px 0 0 16px;
}
.dra__loading .dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #d9b9b9;
  animation: blink 1.2s infinite;
}
.dra__loading .dot:nth-child(2) { animation-delay: 0.2s; }
.dra__loading .dot:nth-child(3) { animation-delay: 0.4s; }
@keyframes blink {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}
</style>
