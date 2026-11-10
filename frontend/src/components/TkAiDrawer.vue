<script setup lang="ts">
/**
 * TK Ai 助手抽屉 —— 「知识库问答」与「智能问数」**合并在同一条对话流里**。
 *
 * ## 布局（对齐业界同类产品）
 *
 *   ┌────────────────────────────────────────┐
 *   │  [TK Ai 助手]                      ✕  │  ← header：只有标题 + 关闭
 *   ├────────────────────────────────────────┤
 *   │  欢迎区 / 推荐问题 / 对话记录（混排）    │  ← 可滚动区（kb 气泡 + bi 卡片交错）
 *   ├────────────────────────────────────────┤
 *   │  [ 输入你想问的问题…            ][发送] │  ← 共用输入框（忙碌时变「停止」）
 *   │  (知识库问答) (智能问数 beta)          │  ← 模式胶囊
 *   └────────────────────────────────────────┘
 *
 * ## 两个模式的区别
 *
 *   - **知识库问答**：问「**怎么用**」—— 系统操作流程、字段含义、业务规则。走 RAG（`/ai/chat`）
 *   - **智能问数**：问「**是多少**」—— 销售额、库存量这类统计数据。走 ChatBI（`/ai/chatbi/ask` SSE）
 *
 * 模式由底部胶囊**手动切换**（不自动猜），因为「两种都答得上来」的问题只有用户自己知道要哪个。
 *
 * ## 🔴 关键实现：为什么对话流能「混排」
 *
 * `items` 是一个**按时间排序的混合数组**，每条记录带 `mode`：
 *   - `{ mode: 'kb', msg }` → 渲染成聊天气泡
 *   - `{ mode: 'bi', turn }` → 交给 `TkAiChatBi` 渲染（进度条 + 卡片 + 表格 + 图表）
 *
 * 这样「先问怎么下单、再问本月销售额」会在**同一条流里挨着往下长**，
 * 而不是两个互相独立的页签。
 */
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import {
  aiChatStream,
  aiChatBiStream,
  type AiSource,
  type BiEvent
} from '@/api/ai'
import TkAiIcon from './TkAiIcon.vue'
import TkAiChatBi, { type BiTurn, type StepCard } from './TkAiChatBi.vue'
import { useAiStore } from '@/stores/ai'
import robotUrl from '@/assets/robot.png'

const open = defineModel<boolean>({ default: false })

const aiStore = useAiStore()

/** 对话模式：知识库问答（怎么用）/ 智能问数（是多少） */
type Mode = 'kb' | 'bi'
const mode = ref<Mode>('kb')

// ==================== 知识库问答 ====================

interface Msg {
  role: 'user' | 'ai'
  text: string
  sources?: AiSource[]
  hidden?: number
  error?: boolean
  /** 引用列表是否展开。默认收起，不占视觉 */
  showSrc?: boolean
  /** 答案真正引用到的片段序号（已做越界过滤），push 时算一次，模板里只读 */
  cited?: Set<number>
  /**
   * 正在流式生成中。
   * `text` 还是空的 → 显示「正在查阅系统文档…」；有内容了 → 显示文字 + 光标。
   */
  streaming?: boolean
}

/**
 * 引用的**文件数**（去重）。
 *
 * ⚠️ 不能用 `sources.length` —— 那是**片段**数，3 个片段可能来自同一份文件。
 *    用户理解的是「几份资料」，所以必须按文件去重。
 */
function fileCount(sources: AiSource[]): number {
  return new Set(sources.map((s) => s.file)).size
}

// ==================== 混合对话流 ====================

/** 一条对话记录：要么是知识库问答，要么是智能问数 */
type Item =
  | { id: number; mode: 'kb'; msg: Msg }
  | { id: number; mode: 'bi'; turn: BiTurn }

const items = ref<Item[]>([])
/** 所有 bi 子组件的实例（按记录 id 存），切页签/抽屉时统一 resize */
const biRefs = new Map<number, InstanceType<typeof TkAiChatBi>>()
function setBiRef(id: number, el: any) {
  if (el) biRefs.set(id, el)
  else biRefs.delete(id)
}

let seq = 0

/** 引导问题 —— 以后可以改成从真实提问语料按频次取，现在先手工挑几条「一定能答对」的 */
const KB_SUGGESTIONS = [
  '工单的完工数量是怎么算的？',
  '系统能不能批量导入客户信息？',
  '销售毛利的报表在哪里看？',
  '怎么下单销售订单？',
  'producing 是什么意思？',
  'sales_order 有哪些字段？',
  '库存是怎么被占用的？',
  '报工和审工有什么区别？',
  '采购订单审批通过后怎么入库？',
  '客户档案在哪里维护联系人？',
  '订单的编号规则是什么？',
  '系统支持生产报表吗？'
]
const BI_SUGGESTIONS = [
  '本月销售额是多少？',
  '今年销售额按客户排名前5',
  '按月份看今年的销售趋势',
  '库存按仓库分布',
  '采购订单金额按供应商排名前5',
  '生产工单里按期完成的比例是多少？'
]

/** 推荐问题跟着模式走 —— 切到问数就别推「怎么下单」了 */
const suggestions = computed(() => (mode.value === 'kb' ? KB_SUGGESTIONS : BI_SUGGESTIONS))

const PAGE = 6
const page = ref(0)
const shownSuggestions = computed(() =>
  Array.from(
    { length: PAGE },
    (_, i) => suggestions.value[(page.value * PAGE + i) % suggestions.value.length]
  )
)

// ==================== 输入框 ====================

const question = ref('')
const loading = ref(false)
const listRef = ref<HTMLElement | null>(null)
let ctrl: AbortController | null = null

// ==================== 抽屉宽度：左边缘可拖拽 ====================
//
// 宽度存在 `aiStore` 里 —— 因为 `layout` 要按这个宽度给主区域**让位**
//（抽屉不是盖在 ERP 上，而是把 ERP 挤到左边，否则右侧的「保存/提交」按钮点不到）。

const resizing = ref(false)
const drag = reactive({ x0: 0, w0: 0 })

onMounted(() => window.addEventListener('resize', onWinResize))
onBeforeUnmount(() => {
  window.removeEventListener('resize', onWinResize)
  ctrl?.abort()
})

function onWinResize() {
  aiStore.setWidth(aiStore.width)
}

function onGripDown(e: PointerEvent) {
  if (e.button !== 0) return
  const el = e.currentTarget as HTMLElement
  el.setPointerCapture(e.pointerId)
  resizing.value = true
  drag.x0 = e.clientX
  drag.w0 = aiStore.width
}

function onGripMove(e: PointerEvent) {
  if (!resizing.value) return
  // 抽屉贴着屏幕右边缘，所以鼠标往左拖 = 变宽
  aiStore.setWidth(drag.w0 + (drag.x0 - e.clientX))
}

function onGripUp(e: PointerEvent) {
  if (!resizing.value) return
  const el = e.currentTarget as HTMLElement
  if (el.hasPointerCapture(e.pointerId)) el.releasePointerCapture(e.pointerId)
  resizing.value = false
}

// ==================== 滚动 ====================

/**
 * 滚到底。用 smooth 而不是直接赋 scrollTop：
 * 欢迎区现在是常驻的，瞬时跳到底会让人以为「内容被清空了」，
 * 平滑滚动能让人看到对话是「从下面长出来」的。
 */
function scrollToEnd(smooth = true) {
  nextTick(() => {
    const el = listRef.value
    if (!el) return
    el.scrollTo({ top: el.scrollHeight, behavior: smooth ? 'smooth' : 'auto' })
  })
}

// 打开抽屉时若已有历史对话，直接定位到底部，不要播动画
watch(open, (v) => {
  if (!v) return
  // 「数据知识库助手」卡片点名要落在「知识库问答」——
  // ⚠️ 一次性指令，用完立刻清空，否则下次从顶栏打开也会被它劫持到 kb。
  if (aiStore.pendingMode) {
    mode.value = aiStore.pendingMode
    aiStore.pendingMode = null
  }
  scrollToEnd(false)
})

/** 抽屉从隐藏恢复时，告诉所有问数卡片重新量一次图表尺寸 */
function resizeAllCharts() {
  nextTick(() => {
    for (const inst of biRefs.values()) inst?.resizeAll()
  })
}
watch(open, () => resizeAllCharts())
watch(mode, () => resizeAllCharts())

/**
 * 抽屉打开时把宽度写到 `<body>` 上：
 *   - `layout` 用它给主区域**让出**这块宽度（不是盖住 ERP）
 *   - 全局 CSS 用它把 `el-dialog` 的居中区域往左挪，弹窗不会被抽屉压一半
 */
watch(
  [open, () => aiStore.width],
  () => {
    const b = document.body
    if (open.value) {
      b.classList.add('tkai-open')
      b.style.setProperty('--tkai-w', aiStore.width + 'px')
    } else {
      b.classList.remove('tkai-open')
      b.style.setProperty('--tkai-w', '0px')
    }
  },
  { immediate: true }
)

// ==================== 知识库问答的文本格式化 ====================

/**
 * 极简格式化：先转义，再把 **粗体** 变成 <strong>。
 * ⚠️ 顺手把答案末尾的 `[片段N]` 标注剥掉 —— 引用信息统一收到折叠区里，
 * 正文保持干净。注意 citedIndexes() 读的仍是原始 text，所以「哪条被引用过」的信息没丢。
 * 完整 Markdown 渲染留到后面做。
 */
function format(text: string) {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/[ \t]*\[片段\s*\d+\]/g, '')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}

/**
 * 答案里标注的 [片段N] —— 只有这些是真的被引用到的。
 *
 * ⚠️ 必须丢掉越界编号：模型偶尔会写出资料里根本没有的编号
 * （实测：只给了 3 条资料，却标了 [片段4]），不过滤就会出现
 * 「引用 3 条（用到 4 条）」这种自相矛盾的显示。
 */
function citedIndexes(text: string, total: number) {
  const set = new Set<number>()
  for (const m of text.matchAll(/\[片段\s*(\d+)\]/g)) {
    const n = Number(m[1])
    if (n >= 1 && n <= total) set.add(n)
  }
  return set
}

// ==================== 发送 ====================

async function ask(q?: string) {
  const text = (q ?? question.value).trim()
  if (!text || loading.value) return
  question.value = ''

  loading.value = true
  scrollToEnd()

  if (mode.value === 'kb') {
    await askKb(text)
  } else {
    await askBi(text)
  }
}

/**
 * 知识库问答：**SSE 流式**（打字机效果）。
 *
 * 之前是一次性返回 —— 用户要干等两秒多，然后整段答案「啪」地弹出来。
 * 现在：检索完（约 0.7 秒）就先拿到引用，随后答案逐块追加。
 *
 * ⚠️ 引用**先拿到但不渲染** —— 等流结束后才显示。
 *    否则引用条会先跳出来、等答案写完位置又变。用户定的节奏是：
 *    「正在查阅系统文档…」→ 文字逐字出现 → 完成后在下方显示引用。
 */
async function askKb(text: string) {
  items.value.push({ id: ++seq, mode: 'kb', msg: { role: 'user', text } })

  // 先插一个空的 AI 气泡（立刻就能显示「正在查阅系统文档…」），
  // 然后**从数组取回响应式引用**再改它 —— 不能用 push 时那个字面量（同 askBi 的坑）。
  const aiId = ++seq
  items.value.push({ id: aiId, mode: 'kb', msg: { role: 'ai', text: '', streaming: true } })
  const msg = (items.value[items.value.findIndex((it) => it.id === aiId)] as {
    mode: 'kb'
    msg: Msg
  }).msg
  scrollToEnd()

  ctrl = new AbortController()

  try {
    await aiChatStream(
      text,
      (ev) => {
        if (ev.type === 'sources') {
          // 先存着、不渲染（见函数头说明）
          msg.sources = ev.sources
          msg.hidden = ev.hidden
        } else if (ev.type === 'delta') {
          msg.text += ev.text
          scrollToEnd()
        } else if (ev.type === 'reasoning') {
          // 当前后端已关掉模型的思考（`rag_core.DISABLE_THINKING`），正常不会走到这。
          // 万一以后打开思考，这里**刻意不展示** —— 与「非流式」时的行为保持一致。
        } else if (ev.type === 'error') {
          msg.error = true
          msg.text += (msg.text ? '\n\n' : '') + '出错了：' + ev.message
        }
      },
      ctrl.signal
    )
  } catch (e: any) {
    if (e?.name === 'AbortError') {
      msg.text += (msg.text ? '\n\n' : '') + '（已停止生成）'
    } else {
      msg.error = true
      msg.text +=
        (msg.text ? '\n\n' : '') +
        '调用失败：' +
        (e?.message || '未知错误') +
        '\n（检查 AI 服务是否已在 127.0.0.1:8000 启动）'
    }
  } finally {
    msg.streaming = false
    // ⚠️ `cited` 必须等答案**完整**后再算 —— 流式过程中算没意义（后面的片段标注还没到）
    if (msg.sources) msg.cited = citedIndexes(msg.text, msg.sources.length)
    loading.value = false
    ctrl = null
    scrollToEnd()
  }
}

/** 智能问数：SSE 流式，逐事件更新那条 turn */
async function askBi(text: string) {
  // 用户提问气泡：用和 kb 一样的样式，父组件统一渲染（子组件不再重复画气泡）
  const userItem: Item = { id: ++seq, mode: 'kb', msg: { role: 'user', text } }
  items.value.push(userItem)

  // ⚠️⚠️ 关键：必须用「从数组取回来」的引用改它（下面 push 后立即取回）。
  //   `items` 是 `ref([])`，`push` 存的是**原始对象**；访问 `items.value[i]`
  //   时才被包成 Proxy。若继续用原始引用改属性，**不经过 Proxy 的 set trap**
  //   → 依赖收集不到 → 视图不更新（实测症状：进度条/卡片/表格全都「数据在但渲染不出来」）。
  items.value.push({
    id: ++seq,
    mode: 'bi',
    turn: {
      id: seq,
      question: '',
      percent: 0,
      cards: [],
      done: false,
      rating: null
    }
  })

  const userIdx = items.value.findIndex((it) => it.id === userItem.id)
  // 👇 取回响应式引用（不要用 push 时的那个字面量）
  const turn = (items.value[userIdx + 1] as { mode: 'bi'; turn: BiTurn }).turn
  scrollToEnd()

  ctrl = new AbortController()

  try {
    await aiChatBiStream(
      text,
      (ev: BiEvent) => {
        applyBiEvent(turn, ev)
        scrollToEnd()
      },
      ctrl.signal
    )
  } catch (e: any) {
    if (e?.name === 'AbortError') {
      turn.aborted = true
    } else {
      turn.errorMsg =
        (e?.message || '未知错误') + '（检查 AI 服务是否已在 127.0.0.1:8000 启动）'
    }
    turn.done = true
  } finally {
    turn.done = true
    turn.percent = 100
    loading.value = false
    ctrl = null
    scrollToEnd()
  }
}

/** 把后端事件落到 turn 上 */
function applyBiEvent(turn: BiTurn, ev: BiEvent) {
  // done：总进度拉满、收尾
  if (ev.step === 'done') {
    turn.percent = 100
    turn.done = true
    if (ev.elapsed_ms) turn.elapsedMs = ev.elapsed_ms
    return
  }

  if (typeof ev.percent === 'number') turn.percent = ev.percent

  // 表格 / 图表（visual 那步带的）
  if (ev.table) turn.table = ev.table
  if (ev.chart !== undefined) turn.chart = ev.chart
  if (ev.meta) turn.meta = ev.meta

  // 结论
  if (ev.conclusion) turn.conclusion = ev.conclusion

  // 步骤卡片
  upsertCard(turn, ev)

  if (ev.status === 'rejected') turn.rejected = true
  if (ev.status === 'error') turn.errorMsg = ev.message || '出错了'
}

/**
 * 把事件落成一张卡片。
 *
 * - `running`：**新建**一张卡片（如果还没有），状态=进行中
 * - `ok/rejected/error`：**更新**已有卡片；若还没有（`plan` 只发一次 ok），就新建
 *
 * ⚠️ 用 `key` 而不是 `step_index` 匹配 —— 后端 `done` 步的 index 也是 7，
 *    会撞上 `conclusion`。key 是唯一的。
 */
function upsertCard(turn: BiTurn, ev: BiEvent) {
  if (!ev.step_name) return
  const key = ev.step
  let card = turn.cards.find((c) => c.key === key)

  if (!card) {
    card = {
      key,
      name: ev.step_name,
      message: '',
      lines: [],
      status: ev.status === 'running' ? 'running' : (ev.status as StepCard['status']),
      percent: 0
    }
    turn.cards.push(card)
  }

  if (ev.message) card.message = ev.message
  if (ev.lines && ev.lines.length) card.lines = ev.lines
  if (ev.sql) card.sql = ev.sql

  // running → 保持进行中；终态 → 打勾
  if (ev.status !== 'running') {
    card.status = ev.status as StepCard['status']
    card.percent = 100
  }
}

/** 「停止」——中止流式请求（知识库问答是短请求，等它自然结束即可） */
function stop() {
  ctrl?.abort()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    ask()
  }
}

/** 切换模式时把推荐问题翻回第一页，避免页码越界显示空白 */
watch(mode, () => {
  page.value = 0
})
</script>

<template>
  <!--
    :modal="false" 是关键：去掉遮罩层。
    ERP 是个多页签的后台，用户经常要「一边问 AI 一边照着操作」，
    有遮罩的话页面会变暗且点不动，助手就废了。
  -->
  <el-drawer
    v-model="open"
    direction="rtl"
    :size="aiStore.width + 'px'"
    :with-header="false"
    :modal="false"
    :lock-scroll="false"
    :class="['tkai-drawer', { 'is-resizing': resizing }]"
    append-to-body
  >
    <div class="tkai">
      <!-- 左边缘拖拽手柄：按住往左拉宽、往右收窄 -->
      <div
        class="tkai__grip"
        title="拖动调整宽度"
        @pointerdown="onGripDown"
        @pointermove="onGripMove"
        @pointerup="onGripUp"
        @pointercancel="onGripUp"
      />

      <header class="tkai__head">
        <TkAiIcon variant="brand" :size="26" />
        <div class="tkai__title">
          <h3>TK Ai 助手</h3>
          <span>{{ mode === 'kb' ? '问「怎么用」' : '问「是多少」' }}</span>
        </div>
        <button class="tkai__close" title="关闭" @click="open = false">
          <el-icon><Close /></el-icon>
        </button>
      </header>

      <div ref="listRef" class="tkai__body">
        <!--
          欢迎区【常驻】：问完第一个问题后不清空，对话直接从它下面往下长。
          以前这里挂了 v-if="!messages.length"，一提问整块被销毁，
          看起来像"页面被清空"。
        -->
        <div class="tkai__welcome">
          <img class="tkai__robot" :src="robotUrl" alt="TK Ai 助手" />
          <h3>你好，我是 TK Ai 助手</h3>
          <p>
            我知道 Token 制造 ERP 的操作流程、字段含义和业务规则，<br />
            也能直接查销售、采购、生产、库存的统计数据。
          </p>
        </div>

        <!-- 推荐问题同样常驻，聊到一半想换问题随时还能点 -->
        <div class="tkai__rec">
          <span class="tkai__rec-title">试试这些</span>
          <button class="tkai__rec-more" @click="page++">换一批</button>
        </div>
        <button
          v-for="(s, i) in shownSuggestions"
          :key="s"
          class="tkai__rec-item"
          @click="ask(s)"
        >
          <em>{{ i + 1 }}</em>{{ s }}
        </button>

        <!-- 有了对话之后拉一条细线，把「引导区」和「真实对话」隔开，免得误点上面的推荐问题 -->
        <div v-if="items.length" class="tkai__divider" />

        <!-- ===== 混合对话流：kb 气泡 与 bi 卡片 按时间交错 ===== -->
        <template v-for="it in items" :key="it.id">
          <!-- 知识库问答 -->
          <div v-if="it.mode === 'kb'" class="tkai__msg" :class="it.msg.role">
            <div class="tkai__bubble" :class="{ err: it.msg.error }">
              <!-- 流式中且一个字都还没有 → 显示检索提示（约 0.7~1.5 秒） -->
              <span v-if="it.msg.streaming && !it.msg.text" class="tkai__waiting">
                <i class="tkai__wait-spin" />正在查阅系统文档…
              </span>
              <template v-else>
                <span v-html="format(it.msg.text)" />
                <!-- 打字机光标：流式过程中闪，结束后消失 -->
                <span v-if="it.msg.streaming" class="tkai__cursor" />
              </template>
            </div>

            <!--
              引用默认收起来，而且这块在气泡【外面】—— 答案里不再出现任何引用信息。
              ⚠️ 显示时机是**答案写完**（`!it.msg.streaming`）—— 数据其实早就到了，
                 但提前渲染会让引用条「先跳出来、等答案写完位置又变」。
              溯源能力一点没丢，点一下就能展开，平时完全不占视觉。
            -->
            <template v-if="!it.msg.streaming && it.msg.sources && it.msg.sources.length">
              <button class="tkai__cite" @click="it.msg.showSrc = !it.msg.showSrc">
                <span>来源于系统知识库 · {{ fileCount(it.msg.sources) }} 份文档</span>
                <span class="tkai__caret" :class="{ up: it.msg.showSrc }">▾</span>
              </button>
              <div v-if="it.msg.showSrc" class="tkai__src">
                <div
                  v-for="(s, j) in it.msg.sources"
                  :key="j"
                  :class="{ dim: it.msg.cited && it.msg.cited.size && !it.msg.cited.has(j + 1) }"
                >
                  <span v-if="s.via === 'lookup'" class="tkai__badge look">精确命中</span>
                  <span v-else class="tkai__badge">{{ (s.score ?? 0).toFixed(3) }}</span>
                  [{{ j + 1 }}] {{ s.key || s.file }}
                </div>
                <div v-if="it.msg.hidden" class="tkai__hidden">
                  已隐藏 {{ it.msg.hidden }} 条低相关片段
                </div>
              </div>
            </template>
          </div>

          <!-- 智能问数：一次问数 = 一个子组件实例（它自己管图表） -->
          <TkAiChatBi v-else :ref="(el: any) => setBiRef(it.id, el)" :turn="it.turn" />
        </template>
      </div>

      <!-- ===== 底部：一个共用输入框 + 模式胶囊 ===== -->
      <footer class="tkai__foot">
        <div class="tkai__input-row">
          <textarea
            v-model="question"
            rows="1"
            :placeholder="
              mode === 'kb'
                ? '问系统怎么用…例：工单的完工数量怎么算？'
                : '问数据多少…例：本月销售额是多少？'
            "
            @keydown="onKeydown"
          />
          <button v-if="loading" class="tkai__send stop" @click="stop">停止</button>
          <button v-else class="tkai__send" :disabled="!question.trim()" @click="ask()">
            发送
          </button>
        </div>

        <!-- 模式胶囊：手动切换，选哪个就走哪条链路 -->
        <div class="tkai__modes">
          <button :class="{ on: mode === 'kb' }" @click="mode = 'kb'">知识库问答</button>
          <button :class="{ on: mode === 'bi' }" @click="mode = 'bi'">智能问数 <em>beta</em></button>
        </div>
      </footer>
    </div>
  </el-drawer>
</template>

<style scoped>
:global(.tkai-drawer .el-drawer__body) {
  padding: 0;
  overflow: hidden;
}

/*
  拖动宽度时必须跟手：Element Plus 给抽屉挂了 transition（含 width），
  不关掉的话拖动会滞后约 300ms，完全跟不上鼠标。
  两个选择器都写，因为不同版本的 EP 把用户 class 挂的层级不一样（可能在 overlay 上，也可能在 .el-drawer 上）。
*/
:global(.tkai-drawer.is-resizing),
:global(.tkai-drawer.is-resizing .el-drawer) {
  transition: none !important;
}
.tkai {
  position: relative; /* 拖拽手柄靠它做定位基准 */
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f6f6f4;
}

/* 左边缘拖拽手柄：本体几乎不可见，鼠标移上去 / 拖动中才亮一条红线 */
.tkai__grip {
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 6px;
  z-index: 10;
  cursor: col-resize;
  touch-action: none;
}
.tkai__grip::after {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  width: 2px;
  background: transparent;
  transition: background 0.15s ease;
}
.tkai__grip:hover::after,
:global(.tkai-drawer.is-resizing) .tkai__grip::after {
  background: #d32f2f;
}
.tkai__head {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 13px 18px;
  background: #fff;
  border-bottom: 1px solid #e6e4de;
}
.tkai__title {
  flex: 1;
  min-width: 0;
}
.tkai__title h3 {
  margin: 0;
  font-size: 14px;
  font-weight: 500;
  color: #2c2c2a;
}
.tkai__title span {
  font-size: 11.5px;
  color: #a8a69d;
}
.tkai__close {
  border: none;
  background: none;
  cursor: pointer;
  color: #888780;
  font-size: 16px;
  line-height: 1;
  padding: 4px;
}
.tkai__close:hover {
  color: #d32f2f;
}

.tkai__body {
  flex: 1;
  overflow-y: auto;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.tkai__welcome {
  text-align: center;
  padding: 8px 0 16px;
}
.tkai__robot {
  width: 96px;
  height: 96px;
  display: block;
  margin: 0 auto 6px;
}
.tkai__welcome h3 {
  margin: 0 0 6px;
  font-size: 15px;
  font-weight: 500;
  color: #2c2c2a;
}
.tkai__welcome p {
  margin: 0;
  font-size: 12px;
  line-height: 1.7;
  color: #888780;
}

.tkai__rec {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 2px 0 -4px;
}
.tkai__rec-title {
  font-size: 12px;
  color: #5f5e5a;
}
.tkai__rec-more {
  border: none;
  background: none;
  cursor: pointer;
  font-size: 12px;
  color: #d32f2f;
  padding: 0;
}
.tkai__rec-item {
  text-align: left;
  border: 1px solid #e6e4de;
  background: #fff;
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 13px;
  color: #444441;
  cursor: pointer;
  line-height: 1.5;
}
.tkai__rec-item:hover {
  border-color: #d32f2f;
  color: #d32f2f;
}
.tkai__rec-item em {
  font-style: normal;
  color: #b4b2a9;
  margin-right: 7px;
}

/* 引导区（机器人 + 推荐问题）与真实对话之间的分隔线 */
.tkai__divider {
  border-top: 1px dashed #dedcd4;
  margin: 6px 0 0;
}

/* 纵向：气泡在上（AI 靠左 / 用户靠右），引用折叠条挂在气泡下面 */
.tkai__msg {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}
.tkai__msg.user {
  align-items: flex-end;
}

/* 引用折叠按钮：刻意做得极轻，平时几乎不抢注意力 */
.tkai__cite {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 1px 2px;
  border: none;
  background: none;
  font-size: 11.5px;
  color: #a8a69d;
  cursor: pointer;
}
.tkai__cite:hover {
  color: #d32f2f;
}
.tkai__caret {
  display: inline-block;
  font-size: 10px;
  line-height: 1;
  transition: transform 0.18s ease;
}
.tkai__caret.up {
  transform: rotate(180deg);
}
.tkai__bubble {
  max-width: 88%;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 13.5px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}
.tkai__msg.ai .tkai__bubble {
  background: #fff;
  border: 1px solid #e6e4de;
  color: #2c2c2a;
}
.tkai__msg.user .tkai__bubble {
  background: #d32f2f;
  color: #fff;
}
.tkai__bubble.err {
  background: #fcebeb;
  border-color: #f09595;
  color: #791f1f;
}

/* 已从气泡里挪出来，独立成块，展开时才出现 */
.tkai__src {
  max-width: 88%;
  padding: 8px 10px;
  background: #fbfbf9;
  border: 1px solid #ecebe5;
  border-radius: 8px;
  font-size: 11.5px;
  line-height: 1.9;
  color: #5f5e5a;
  white-space: normal;
}
.tkai__src .dim {
  opacity: 0.42;
}
.tkai__badge {
  display: inline-block;
  min-width: 40px;
  margin-right: 5px;
  color: #888780;
  font-variant-numeric: tabular-nums;
}
.tkai__badge.look {
  min-width: 52px;
  color: #0f6e56;
}
.tkai__hidden {
  margin-top: 4px;
  color: #9a9890;
  font-style: italic;
}

/* ---------- 流式：等待提示 + 打字机光标 ---------- */
.tkai__waiting {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: #888780;
}
.tkai__wait-spin {
  flex: none;
  width: 11px;
  height: 11px;
  border: 2px solid #e6e4de;
  border-top-color: #d32f2f;
  border-radius: 50%;
  animation: tkai-rotate 0.8s linear infinite;
}
@keyframes tkai-rotate {
  to {
    transform: rotate(360deg);
  }
}

/*
  打字机光标。⚠️ 必须 inline-block —— 气泡是 `white-space: pre-wrap`，
  行内元素在这里不好控制宽高，会画不出来。
*/
.tkai__cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  background: #d32f2f;
  animation: tkai-caret 1s steps(1, end) infinite;
}
@keyframes tkai-caret {
  0%,
  50% {
    opacity: 1;
  }
  51%,
  100% {
    opacity: 0;
  }
}

/* ---------- 底部：输入框 + 模式胶囊 ---------- */
.tkai__foot {
  flex: none;
  background: #fff;
  border-top: 1px solid #e6e4de;
  padding: 10px 14px 12px;
}
.tkai__input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.tkai__input-row textarea {
  flex: 1;
  resize: none;
  max-height: 96px;
  padding: 9px 12px;
  border: 1px solid #d3d1c7;
  border-radius: 8px;
  font-size: 13px;
  font-family: inherit;
  line-height: 1.5;
  outline: none;
}
.tkai__input-row textarea:focus {
  border-color: #d32f2f;
}

/* 发送 / 停止：参考图里是右下一个红色实心按钮 */
.tkai__send {
  flex: none;
  min-width: 62px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #d32f2f;
  color: #fff;
  font-size: 13px;
  font-family: inherit;
  cursor: pointer;
  transition: background 0.15s ease;
}
.tkai__send:hover:not(:disabled) {
  background: #b71c1c;
}
.tkai__send:disabled {
  background: #e5d5d5;
  color: #fff;
  cursor: default;
}
.tkai__send.stop {
  background: #e53935;
}
.tkai__send.stop:hover {
  background: #c62828;
}

/* 模式胶囊：输入框下方一排，选中的高亮描红 */
.tkai__modes {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}
.tkai__modes button {
  border: 1px solid #e2dfd8;
  background: #fff;
  border-radius: 14px;
  padding: 4px 11px;
  font-size: 12px;
  color: #6b6a64;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s ease;
}
.tkai__modes button:hover {
  border-color: #d32f2f;
  color: #d32f2f;
}
.tkai__modes button.on {
  border-color: #d32f2f;
  color: #d32f2f;
  background: #fdf2f2;
  font-weight: 500;
}
.tkai__modes button em {
  font-style: normal;
  font-size: 10.5px;
  color: #b4b2a9;
}
.tkai__modes button.on em {
  color: #d98a8a;
}
</style>
