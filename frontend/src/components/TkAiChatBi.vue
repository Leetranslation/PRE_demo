<script setup lang="ts">
/**
 * 智能问数（ChatBI）**展示组件** —— 只负责把「一次问数的过程」画出来。
 *
 * ## 为什么是「展示组件」而不是「独立面板」
 *
 * 原来它自己管输入框、自己管对话数组，是抽屉里的第二个页签。
 * 但产品要求「知识库问答 / 智能问数」**在同一条对话流里**（参考业界同类产品），
 * 所以数据（`turns`）和输入框都上交给 `TkAiDrawer` 统一管，
 * 本组件只做一件事：**给定 turn，渲染它的进度条 / 卡片 / 表格 / 图表 / 结论**。
 *
 * ⚠️ 这样也顺带绕开一个响应式坑：`turn` 由父组件的 `ref([])` 持有，
 *    父组件改 `turn.percent = …` 时拿到的是 **Proxy**，能正常触发渲染。
 *    （若在本组件内部 `push` 原始对象再用原始引用改，视图不会更新。）
 *
 * ## 渲染结构
 *
 *   ┌──────────────────────────────────────┐
 *   │ ⟳ 智能体开始思考…           32.7%    │  ← 总进度（按步数推进）
 *   ├──────────────────────────────────────┤
 *   │ ✅ 开始                        100%  │  ← 一张卡片=一步
 *   │ ⟳ 用户意图理解                        │  ← 进行中（无对勾）
 *   │    指标名称：销售订单金额              │  ← 「字段名：值」列表
 *   └──────────────────────────────────────┘
 *   [数据表格]  [柱状图]  [分析结论]  👍👎
 *
 * 7 步顺序由后端决定（`plan/metric/intent/sql/data/visual/conclusion`）。
 */
import { nextTick, onBeforeUnmount, reactive } from 'vue'
import * as echarts from 'echarts'
import type { BiTable, BiStepKey } from '@/api/ai'

/** 一张步骤卡片（和后端事件一一对应） */
export interface StepCard {
  key: BiStepKey
  name: string
  /** 卡片主文案 */
  message: string
  /** 「字段名：值」列表 */
  lines: string[]
  status: 'running' | 'ok' | 'rejected' | 'error'
  /** 该卡片自己的完成度（0 / 100） */
  percent: number
  /** SQL 全文（仅「查询语句生成」那步有） */
  sql?: string
}

/** 一次问数 = 一条记录 */
export interface BiTurn {
  id: number
  question: string
  /** 总进度 0~100 */
  percent: number
  /** 步骤卡片（按到达顺序） */
  cards: StepCard[]
  table?: BiTable
  chart?: Record<string, any> | null
  conclusion?: string
  meta?: Record<string, any>
  done: boolean
  /** 「问不了」（正常业务结果，用黄色） */
  rejected?: boolean
  errorMsg?: string
  aborted?: boolean
  rating?: boolean | null
  elapsedMs?: number
}

const props = defineProps<{
  /** 要渲染的**单条**问数记录。父组件在对话流里按时间顺序逐个使用本组件。 */
  turn: BiTurn
}>()

// ---------------- ECharts ----------------
/** 本组件只管**一条**记录 → 一个图表实例即可（但保留 Map 以便将来复用） */
let chart: echarts.ECharts | null = null

const ro = new ResizeObserver(() => {
  if (chart) chart.resize()
})

onBeforeUnmount(() => {
  ro.disconnect()
  chart?.dispose()
  chart = null
})

/** 元素挂上来之后才 setOption（否则容器尺寸是 0，图会画成一条线） */
function ensureChart(el: any) {
  if (!el || !props.turn.chart) return
  const dom = el as HTMLElement

  if (chart) {
    if ((chart as any).getDom?.() !== dom) {
      chart.dispose()
      chart = null
    } else {
      requestAnimationFrame(() => chart?.resize())
      return
    }
  }
  nextTick(() => {
    if (!dom.isConnected) return
    chart = echarts.init(dom)
    chart.setOption(props.turn.chart as any)
    ro.observe(dom)
    requestAnimationFrame(() => chart?.resize())
  })
}

function chartRef() {
  return (el: any) => ensureChart(el)
}

/** 抽屉被 v-show 隐藏后恢复显示时，图表尺寸是 0，需要补一次 resize */
function resizeAll() {
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      const dom = chart ? (chart as any).getDom?.() : null
      if (dom && dom.isConnected && dom.offsetParent !== null) chart?.resize()
    })
  })
}
defineExpose({ resizeAll })

/** 图表类型：从 series[0].type 推 —— 后端 chart 就是裸 option，没有 type 字段 */
function chartKind(chart: Record<string, any> | null | undefined): string {
  const t = chart?.series?.[0]?.type
  return t === 'line' ? '折线图' : t === 'pie' ? '饼图' : t === 'bar' ? '柱状图' : '图表'
}

/** 默认展开 SQL？默认收起（普通用户不关心，面试/调试才看）。key = 步骤 key */
const showSql = reactive<Record<string, boolean>>({})

/** 卡片状态对应的图标（running 态由模板单独渲染小转圈） */
function cardIcon(s: StepCard['status']) {
  return s === 'ok' ? '✓' : s === 'running' ? '' : s === 'rejected' ? '!' : '×'
}
function cardClass(s: StepCard['status']) {
  return s
}

/** 有任何结果可展示吗（决定要不要显示点赞/点踩） */
function hasResult(t: BiTurn): boolean {
  return !!(t.table || t.chart || t.conclusion)
}

/**
 * 口径文案渲染。
 *
 * 指标库的 `description` 里写了 `**重点**`（为了让口径里的关键限定词更醒目，
 * 比如「不含已删除，**含停用**」）。直接用 `{{ }}` 插值会**把星号原样显示出来**，
 * 看着像 bug —— 所以这里转成 `<strong>`。
 *
 * ⚠️ 必须先转义 HTML 再替换（同 `TkAiDrawer.format()` 的做法），
 *    否则口径文案里的尖括号会被当成标签。
 */
function caliberHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}
</script>

<template>
  <!-- 单条记录：一次问数 -->
  <div class="bi__turn">
    <!-- question 由父组件渲染气泡（和 kb 的气泡保持一致），这里只画过程与结果 -->
    <div v-if="props.turn.question" class="bi__q">{{ props.turn.question }}</div>

    <!-- ===== 总进度条（顶部，7 步平均推进）===== -->
    <div
      class="bi__progress"
      :class="{ done: props.turn.done }"
    >
      <div class="bi__progress-head">
        <span class="bi__spin" :class="{ off: props.turn.done }" />
        <span class="bi__progress-title">
          {{ props.turn.done ? '分析完成' : '智能体开始思考…' }}
        </span>
        <span class="bi__progress-pct">{{ props.turn.percent.toFixed(1) }}%</span>
      </div>
      <div class="bi__progress-track">
        <div class="bi__progress-bar" :style="{ width: props.turn.percent + '%' }" />
      </div>
    </div>

    <!-- ===== 步骤卡片 ===== -->
    <div class="bi__cards">
      <div
        v-for="c in props.turn.cards"
        :key="c.key"
        class="bi__card"
        :class="cardClass(c.status)"
      >
        <div class="bi__card-head">
          <span class="bi__card-badge" :class="cardClass(c.status)">
            <template v-if="c.status === 'running'"><i class="bi__mini-spin" /></template>
            <template v-else>{{ cardIcon(c.status) }}</template>
          </span>
          <span class="bi__card-name">{{ c.name }}</span>
          <span class="bi__card-pct">{{ c.percent }}%</span>
        </div>

        <div v-if="c.message" class="bi__card-msg">{{ c.message }}</div>

        <!-- 意图理解那一步的「字段名：值」列表 -->
        <div v-if="c.lines.length" class="bi__card-lines">
          <div v-for="(ln, i) in c.lines" :key="i" class="bi__card-line">{{ ln }}</div>
        </div>

        <!-- SQL 折叠 -->
        <div v-if="c.sql" class="bi__sql-toggle">
          <button @click="showSql[c.key] = !showSql[c.key]">
            {{ showSql[c.key] ? '▾ 收起查询语句' : '▸ 查看查询语句' }}
          </button>
          <pre v-if="showSql[c.key]" class="bi__sql">{{ c.sql }}</pre>
        </div>
      </div>
    </div>

    <!-- ===== 被拒绝（正常业务结果）===== -->
    <div v-if="props.turn.rejected && props.turn.done" class="bi__reject">
      这个问题当前指标库还不支持。可以换个说法，或者问我销售额、采购金额、产量、库存相关的。
    </div>

    <!-- ===== 出错 ===== -->
    <div v-if="props.turn.errorMsg" class="bi__error">{{ props.turn.errorMsg }}</div>

    <!-- ===== 表格 ===== -->
    <div v-if="props.turn.table" class="bi__block">
      <div class="bi__block-title">数据表格</div>
      <div class="bi__table-wrap">
        <table class="bi__table">
          <thead>
            <tr>
              <th v-for="(c, i) in props.turn.table.columns" :key="i" :class="{ num: i > 0 }">
                {{ c }}
              </th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, ri) in props.turn.table.rows" :key="ri">
              <td v-for="(cell, ci) in row" :key="ci" :class="{ num: ci > 0 }">
                {{ cell }}
              </td>
            </tr>
            <tr
              v-if="props.turn.table.total !== null && props.turn.table.total !== undefined"
              class="total"
            >
              <td>合计</td>
              <td class="num">
                {{ typeof props.turn.table.total === 'number'
                  ? props.turn.table.total.toLocaleString('zh-CN', { maximumFractionDigits: 2 })
                  : props.turn.table.total }}
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- ===== 图表 ===== -->
    <div v-if="props.turn.chart" class="bi__block">
      <div class="bi__block-title">{{ chartKind(props.turn.chart) }}</div>
      <div :ref="chartRef()" class="bi__chart" />
    </div>

    <!-- ===== 结论（放最后：表格/图表之后收尾）===== -->
    <div v-if="props.turn.conclusion" class="bi__conclusion">
      <div class="bi__block-title">分析结论</div>
      {{ props.turn.conclusion }}
      <!--
        统计口径 —— 来自指标库的 `description`（后端 meta 里带过来）。
        为什么要露出来：同一句话可能有多种口径，光看数字分不清。
        最典型的是「多少个客户」——档案总数 / 启用数 / 有交易数在演示库上**都是 15**，
        不写口径根本不知道算的是哪个。
        ⚠️ 有单据类型筛选时必须一并显示：口径写的是「含销售/采购/生产/委外」，
           但筛选后实际只统计了其中几种 —— 只显示口径会让人误以为那是全量。
      -->
      <div v-if="props.turn.meta?.description" class="bi__caliber">
        口径：<span v-html="caliberHtml(props.turn.meta.description)" /><template
          v-if="props.turn.meta.doc_types && props.turn.meta.doc_types.length"
        >
          <span class="bi__caliber-filter"
            >（本次仅统计：{{ props.turn.meta.doc_types.join('、') }}）</span
          >
        </template>
      </div>
    </div>

    <!-- ===== 元信息 + 反馈 ===== -->
    <div v-if="props.turn.done && !props.turn.errorMsg" class="bi__foot-row">
      <span v-if="props.turn.meta?.row_count != null" class="bi__meta">
        {{ props.turn.meta.row_count }} 行
        <template v-if="props.turn.meta.elapsed_ms">
          · {{ Math.round(props.turn.meta.elapsed_ms) }}ms</template
        >
        <template v-if="props.turn.meta.truncated"> · 已截断</template>
      </span>
      <span v-else-if="props.turn.elapsedMs" class="bi__meta">
        总耗时 {{ (props.turn.elapsedMs / 1000).toFixed(1) }}s
      </span>
      <span v-if="props.turn.aborted" class="bi__meta">已停止</span>

      <span v-if="hasResult(props.turn)" class="bi__rate">
        <button
          :class="{ on: props.turn.rating === true }"
          title="有帮助"
          @click="props.turn.rating = props.turn.rating === true ? null : true"
        >
          👍
        </button>
        <button
          :class="{ on: props.turn.rating === false }"
          title="没帮上"
          @click="props.turn.rating = props.turn.rating === false ? null : false"
        >
          👎
        </button>
      </span>
    </div>
  </div>
</template>

<style scoped>
.bi__turn {
  display: flex;
  flex-direction: column;
  gap: 10px;
  /* 每条记录之间拉开距离，和父组件的 kb 气泡交错时不会粘在一起 */
  margin-bottom: 18px;
}

.bi__q {
  align-self: flex-end;
  max-width: 85%;
  padding: 8px 13px;
  background: #d32f2f;
  color: #fff;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.6;
  word-break: break-word;
}

/* ---------- 总进度条 ---------- */
.bi__progress {
  background: #fff;
  border: 1px solid #f0dede;
  border-radius: 10px;
  padding: 10px 12px;
}
.bi__progress:not(.done) {
  animation: bi-pulse 2s ease-in-out infinite;
}
.bi__progress-head {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-bottom: 8px;
}
.bi__progress-title {
  flex: 1;
  font-size: 12.5px;
  color: #5f5e5a;
}
.bi__progress-pct {
  font-size: 12.5px;
  color: #d32f2f;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}
.bi__progress-track {
  height: 5px;
  background: #f3e6e6;
  border-radius: 3px;
  overflow: hidden;
}
.bi__progress-bar {
  height: 100%;
  background: linear-gradient(90deg, #e57373, #d32f2f);
  border-radius: 3px;
  transition: width 0.45s cubic-bezier(0.22, 0.61, 0.36, 1);
}
.bi__spin {
  flex: none;
  width: 12px;
  height: 12px;
  border: 2px solid #f3d6d6;
  border-top-color: #d32f2f;
  border-radius: 50%;
  animation: bi-rotate 0.8s linear infinite;
}
.bi__spin.off {
  animation: none;
  border-color: #cfe8dc;
  border-top-color: #2e9e6b;
}
@keyframes bi-rotate {
  to {
    transform: rotate(360deg);
  }
}
@keyframes bi-pulse {
  0%,
  100% {
    box-shadow: 0 0 0 0 rgba(211, 47, 47, 0.12);
  }
  50% {
    box-shadow: 0 0 0 4px rgba(211, 47, 47, 0.04);
  }
}

/* ---------- 步骤卡片 ---------- */
.bi__cards {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.bi__card {
  background: #fff;
  border: 1px solid #ecebe5;
  border-radius: 10px;
  padding: 10px 12px;
  animation: bi-card-in 0.32s ease-out;
}
.bi__card.running {
  border-color: #f0c8c8;
  background: #fffafa;
}
.bi__card.rejected {
  border-color: #e8d9a8;
  background: #fffdf5;
}
.bi__card.error {
  border-color: #efb0b0;
  background: #fdf6f6;
}
@keyframes bi-card-in {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
.bi__card-head {
  display: flex;
  align-items: center;
  gap: 7px;
}
.bi__card-badge {
  flex: none;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #2e9e6b;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  line-height: 1;
}
.bi__card-badge.running {
  background: #fbeaea;
}
.bi__card-badge.rejected {
  background: #d9a825;
}
.bi__card-badge.error {
  background: #d32f2f;
}
.bi__mini-spin {
  width: 7px;
  height: 7px;
  border: 1.5px solid #f0c8c8;
  border-top-color: #d32f2f;
  border-radius: 50%;
  animation: bi-rotate 0.7s linear infinite;
}
.bi__card-name {
  flex: 1;
  font-size: 13px;
  font-weight: 500;
  color: #2c2c2a;
}
.bi__card-pct {
  font-size: 11.5px;
  color: #a8a69d;
  font-variant-numeric: tabular-nums;
}
.bi__card.ok .bi__card-pct {
  color: #2e9e6b;
}
.bi__card-msg {
  margin-top: 6px;
  font-size: 12.5px;
  color: #5f5e5a;
  line-height: 1.7;
  word-break: break-word;
}
.bi__card-lines {
  margin-top: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.bi__card-line {
  font-size: 12px;
  color: #6b6a64;
  line-height: 1.75;
  padding-left: 2px;
}
.bi__sql-toggle {
  margin-top: 7px;
}
.bi__sql-toggle button {
  border: none;
  background: none;
  padding: 0;
  font-size: 11.5px;
  color: #a8a69d;
  cursor: pointer;
  font-family: inherit;
}
.bi__sql-toggle button:hover {
  color: #d32f2f;
}
.bi__sql {
  margin: 6px 0 0;
  padding: 8px 10px;
  background: #fbfbf9;
  border: 1px solid #ecebe5;
  border-radius: 6px;
  font-size: 11px;
  line-height: 1.6;
  color: #5f5e5a;
  overflow-x: auto;
  white-space: pre-wrap;
  word-break: break-all;
}

/* ---------- 拒绝 / 出错 ---------- */
.bi__reject {
  background: #fffdf5;
  border: 1px solid #e8d9a8;
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 12.5px;
  color: #8a6d1f;
  line-height: 1.75;
}
.bi__error {
  background: #fdf6f6;
  border: 1px solid #efb0b0;
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 12.5px;
  color: #a12b2b;
  line-height: 1.75;
}

/* ---------- 结论 / 表格 / 图表 ---------- */
.bi__block {
  background: #fff;
  border: 1px solid #ecebe5;
  border-radius: 10px;
  padding: 12px;
}
.bi__block-title {
  font-size: 12.5px;
  font-weight: 600;
  color: #2c2c2a;
  margin-bottom: 8px;
}
.bi__conclusion {
  background: #fff;
  border: 1px solid #ecebe5;
  border-radius: 10px;
  padding: 12px;
  font-size: 13px;
  color: #2c2c2a;
  line-height: 1.85;
}
.bi__conclusion .bi__block-title {
  color: #d32f2f;
}

/* ---------- 统计口径（结论下面那行小字）---------- */
.bi__caliber {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #ecebe5;
  font-size: 12px;
  color: #9a9890;
  line-height: 1.7;
}
/* 单据类型筛选单独标色 —— 它是**对口径的收窄**，比口径本身更需要被看见 */
.bi__caliber-filter {
  color: #b07a3a;
}
.bi__table-wrap {
  overflow-x: auto;
}
.bi__table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12.5px;
}
.bi__table th,
.bi__table td {
  padding: 6px 9px;
  text-align: left;
  border-bottom: 1px solid #f0efe9;
  white-space: nowrap;
}
.bi__table th {
  color: #888780;
  font-weight: 500;
  background: #fbfbf9;
}
.bi__table td.num,
.bi__table th.num {
  text-align: right;
  font-variant-numeric: tabular-nums;
}
.bi__table tr.total td {
  font-weight: 600;
  color: #d32f2f;
  border-top: 1px solid #ecebe5;
}
.bi__table tr:last-child td {
  border-bottom: none;
}
.bi__chart {
  width: 100%;
  height: 250px;
}

/* ---------- 底部：行数 / 点赞 ---------- */
.bi__foot-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bi__meta {
  font-size: 11.5px;
  color: #a8a69d;
}
.bi__rate {
  display: inline-flex;
  gap: 2px;
  margin-left: auto;
}
.bi__rate button {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 13px;
  padding: 2px 3px;
  border-radius: 5px;
  opacity: 0.4;
  line-height: 1.6;
}
.bi__rate button:hover {
  opacity: 0.8;
}
.bi__rate button.on {
  opacity: 1;
  background: #fbeaea;
}
</style>
