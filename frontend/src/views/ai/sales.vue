<script setup lang="ts">
/**
 * AI 销售助手 · 智能销售订单。
 *
 * ## 布局
 *
 *   左：对话 —— 打字 / 传图 / 拖拽 / 粘贴截图
 *   右：销售订单表单 —— AI 填好的内容落在这里，**用户确认后手工点保存**
 *
 * ## 🔴 写入边界
 *
 * **AI 只填表，不提交。** 保存走 ERP 自己的 `POST /api/sales/order/create`
 * （权限、字段校验、单据编号规则全复用）。AI 侧全程只读。
 *
 * ## 两条入口，一条主干
 *
 *   文字 ────────────────┐
 *                         ├─ 后端 nlu.run() → matcher → 草稿
 *   图片 ─ 视觉转录 ───────┘
 *
 * ## 🔴 图片这块的两个关键设计
 *
 * **1. 转录原文必须摆给用户看，而且可编辑。**
 *    模型自报的「不确定」不可靠 —— 实测它把「盏」读成「盖」，还声称很有信心。
 *    所以唯一可靠的手段是把证据摆出来让人核对。
 *    用户改完点「重新解析」→ 只调文本接口（¥0.007），**不再花视觉模型的钱**。
 *
 * **2. 单位 / 规格不由图片决定，取产品档案。**
 *    就是为了让「盏→盖」这类误识**无害**。同理单号/金额/大写一律不取。
 *
 * ## 前端压缩
 *
 * 手机照片 3~5MB，直接传会让视觉 token 翻几倍。`utils/imageCompress.ts`
 * 用 canvas 压到长边 1600 / JPEG 0.85（约 200~400KB）——
 * 这是纯浏览器能力，绕开了「本机没有图像处理工具」的限制。
 */
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound, Paperclip, ShoppingBag, Files } from '@element-plus/icons-vue'
import { salesOrderCreate } from '@/api/sales'
import { customerList } from '@/api/base'
import {
  aiSalesParse,
  aiSalesParseImage,
  type SalesCustomerCandidate,
  type SalesParseResult,
  type SalesProductCandidate
} from '@/api/ai'
import ProductPicker from '@/components/ProductPicker.vue'
import { compressImage, humanSize } from '@/utils/imageCompress'

const router = useRouter()

// ==================== 左侧：对话 ====================

/** 需要用户点一下确认的候选组 */
interface PickGroup {
  keyword: string
  candidates: SalesProductCandidate[]
}

interface Msg {
  role: 'ai' | 'user'
  text: string
  loading?: boolean
  error?: boolean
  /** 用户上传的图片缩略图（data URL，直接给 <img> 用） */
  image?: string
  /** 图片入口的逐字转录（**可编辑**，改完点「重新解析」） */
  transcript?: string
  /** 转录里有模型标了 `[?]` 的拿不准的字 */
  transcriptUncertain?: boolean
  /** 客户有多个相近的 → 给出可点的候选 */
  custPicks?: SalesCustomerCandidate[]
  /** 明细有多个候选 → 按 keyword 分组 */
  itemPicks?: PickGroup[]
  /** 「重新解析」进行中 */
  busy?: boolean
}

const msgs = ref<Msg[]>([
  {
    role: 'ai',
    text:
      '您好，我是 AI 销售助手！\n' +
      '把要卖的货说清楚（客户、产品、数量、单价），我会把右边的销售订单填好。\n' +
      '也可以直接上传 / 拖入 / Ctrl+V 粘贴一张订单图片（截图、拍照、手写都行），我先认字再填。\n\n' +
      '例如：卖给东莞恒辉灯饰，LED平板灯 100 盏，单价 11.1，下周五要'
  }
])
const input = ref('')
const listRef = ref<HTMLElement | null>(null)
const fileRef = ref<HTMLInputElement | null>(null)
const sending = ref(false)
const dragging = ref(false)
let ctrl: AbortController | null = null

// ==================== 批量导入：多图 → 草稿队列 ====================
/**
 * 🔴 批量只做「逐张解析 + 逐张确认」，**绝不提供一键全部下单**。
 *    10 张图里错 1 张，一键保存就是 1 张错单进系统，比手工还慢。
 */
interface BatchDraft {
  id: number
  name: string
  thumb: string
  transcript: string
  uncertain: boolean
  result: SalesParseResult | null
  status: 'parsing' | 'ready' | 'loaded' | 'saved' | 'error'
  error?: string
}
const batchOpen = ref(false)
const batchDrafts = ref<BatchDraft[]>([])
const batchRunning = ref(false)
const batchInputRef = ref<HTMLInputElement | null>(null)
/** 当前载入到表单的是哪张草稿（保存成功时把它标记为已保存） */
const loadedDraftId = ref<number | null>(null)

function scrollEnd() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

// ==================== 右侧：销售订单 ====================

const form = reactive<any>({
  customerId: undefined,
  salesType: 'normal',
  deliveryDate: undefined,
  salesman: '',
  remark: '',
  items: []
})

const customers = ref<any[]>([])
const customerLoading = ref(false)
const productPickerVisible = ref(false)
const currentRow = ref<any>(null)
const saving = ref(false)

/** AI 填进来的行高亮多久（毫秒） */
const FLASH_MS = 1800

onMounted(() => {
  // 业务员默认带出当前登录人（和 ERP 新建订单的习惯一致）
  form.salesman = localStorage.getItem('realName') || localStorage.getItem('username') || ''
  searchCustomer('')
})

async function searchCustomer(keyword: string) {
  customerLoading.value = true
  try {
    const res = await customerList({ page: 1, pageSize: 20, keyword, status: 1 })
    const list = res.records || []
    // 把 AI 匹配进来的客户并进去，否则 el-select 只会显示一个 id
    for (const c of customers.value) {
      if (!list.some((x: any) => x.id === c.id)) list.push(c)
    }
    customers.value = list
  } finally {
    customerLoading.value = false
  }
}

/** 保证 el-select 有对应 option —— 否则 AI 填了 customerId 但下拉里没有这一项，会显示成数字 */
function ensureCustomerOption(c: SalesCustomerCandidate) {
  if (!customers.value.some((x: any) => x.id === c.id)) {
    customers.value = [{ id: c.id, code: c.code, name: c.name }, ...customers.value]
  }
}

/** 高亮一行。⚠️ 传进来的必须是**从 form.items 取回的响应式引用** */
function flash(row: any) {
  row._flash = true
  setTimeout(() => {
    row._flash = false
  }, FLASH_MS)
}

function fillRow(row: any, c: SalesProductCandidate) {
  row.productId = c.id
  row.productCode = c.code
  row.productName = c.name
  // 🔴 规格和单位取产品档案，不用输入里的 —— 见文件头「盏→盖」的说明
  row.spec = c.spec
  row.unit = c.unit
}

/**
 * 把后端结果落到表单上，并返回对话里要显示的候选。
 *
 * `status='none'`（含「把物料当成产品」）**不建行** —— 建空行比不建更糟：
 * 用户会以为系统认了这个产品，直接保存出一张错单。
 */
function applyResult(r: SalesParseResult): Pick<Msg, 'custPicks' | 'itemPicks'> {
  // ---- 客户 ----
  const c = r.header.customer
  if (c.status === 'matched' && c.best) {
    form.customerId = c.best.id
    ensureCustomerOption(c.best)
  }

  // ---- 单头（用户自己填过的不覆盖）----
  if (r.header.deliveryDate && !form.deliveryDate) form.deliveryDate = r.header.deliveryDate
  if (r.header.salesman && !form.salesman) form.salesman = r.header.salesman
  if (r.header.remark && !form.remark) form.remark = r.header.remark

  // ---- 明细 ----
  const newRows: any[] = []
  for (const it of r.items) {
    if (it.status === 'none' || !it.best) continue
    newRows.push({
      productId: it.best.id,
      productCode: it.best.code,
      productName: it.best.name,
      spec: it.best.spec,
      unit: it.best.unit,
      quantity: it.quantity,
      price: it.price,
      taxRate: 13,
      deliveryDate: undefined,
      remark: it.remark || undefined,
      // 下面几个是**前端自己用的**，保存时不会提交
      _keyword: it.keyword,
      _match: it.status
    })
  }
  const start = form.items.length
  form.items.push(...newRows)
  // 🔴 必须从数组里取回响应式引用再改 `_flash`（`newRows` 里是原始对象）
  for (let i = start; i < form.items.length; i++) flash(form.items[i])

  return {
    custPicks: c.status === 'ambiguous' ? c.candidates : undefined,
    itemPicks: r.items
      .filter((i) => i.status === 'ambiguous' && i.candidates.length)
      .map((i) => ({ keyword: i.keyword, candidates: i.candidates }))
  }
}

/** 重新解析前，先把上一次 AI 填的行清掉，避免重复堆叠 */
function clearAiRows() {
  form.items = form.items.filter((r: any) => !r._keyword)
}

// ==================== 发送：文字 / 图片 ====================

function applyToMsg(msg: Msg, r: SalesParseResult) {
  msg.loading = false
  msg.text = r.reply
  msg.transcript = r.transcript
  msg.transcriptUncertain = r.transcript_uncertain
  Object.assign(msg, applyResult(r))
}

async function send() {
  const t = input.value.trim()
  if (!t || sending.value) return

  msgs.value.push({ role: 'user', text: t })
  input.value = ''
  const aiId = msgs.value.length
  msgs.value.push({ role: 'ai', text: '正在理解…', loading: true })
  scrollEnd()

  sending.value = true
  ctrl = new AbortController()
  try {
    const r = await aiSalesParse(t, ctrl.signal)
    // ⚠️ 必须从数组里取回响应式引用再改
    applyToMsg(msgs.value[aiId], r)
  } catch (e: any) {
    const msg = msgs.value[aiId]
    msg.loading = false
    msg.error = true
    msg.text = e?.response?.data?.detail || e?.message || '解析失败，请稍后重试'
  } finally {
    sending.value = false
    ctrl = null
    scrollEnd()
  }
}

async function sendImage(file: File) {
  if (sending.value) return
  sending.value = true
  ctrl = new AbortController()
  try {
    // 先在浏览器里压 —— 视觉模型按图片 token 计费，原图 3MB 会让 token 翻几倍
    const c = await compressImage(file)
    const thumb = `data:${c.mediaType};base64,${c.base64}`

    msgs.value.push({
      role: 'user',
      text: `【图片】${humanSize(file.size)} → 压缩后 ${humanSize(c.bytes)}（${c.width}×${c.height}）`,
      image: thumb
    })
    const aiId = msgs.value.length
    msgs.value.push({ role: 'ai', text: '正在识别图片…（先认字，再解析）', loading: true })
    scrollEnd()

    const r = await aiSalesParseImage(c.base64, c.mediaType, ctrl.signal)
    applyToMsg(msgs.value[aiId], r)
  } catch (e: any) {
    const msg = msgs.value[msgs.value.length - 1]
    if (msg?.loading) {
      msg.loading = false
      msg.error = true
      msg.text = e?.response?.data?.detail || e?.message || '图片识别失败'
    } else {
      ElMessage.error(e?.message || '图片处理失败')
    }
  } finally {
    sending.value = false
    ctrl = null
    scrollEnd()
  }
}

// ==================== 批量导入：多图逐个解析 ====================

function openBatch() {
  batchOpen.value = true
}

function pickBatchFiles() {
  batchInputRef.value?.click()
}

async function onBatchFiles(e: Event) {
  const el = e.target as HTMLInputElement
  const files = Array.from(el.files || [])
  el.value = '' // 允许连续选同一批
  if (!files.length) return
  await runBatch(files)
}

/** 逐张串行解析（并发会撞限流，也让进度看起来乱） */
async function runBatch(files: File[]) {
  batchRunning.value = true
  try {
    for (const f of files) {
      batchDrafts.value.push({
        id: Date.now() + Math.floor(Math.random() * 100000),
        name: f.name,
        thumb: '',
        transcript: '',
        uncertain: false,
        result: null,
        status: 'parsing'
      })
      // ⚠️ 必须取回响应式引用再改
      const cur = batchDrafts.value[batchDrafts.value.length - 1]
      try {
        const c = await compressImage(f)
        cur.thumb = `data:${c.mediaType};base64,${c.base64}`
        const r = await aiSalesParseImage(c.base64, c.mediaType)
        cur.result = r
        cur.transcript = r.transcript || ''
        cur.uncertain = r.transcript_uncertain ?? false
        cur.status = 'ready'
      } catch (err: any) {
        cur.status = 'error'
        cur.error = err?.response?.data?.detail || err?.message || '解析失败'
      }
    }
  } finally {
    batchRunning.value = false
  }
}

function resetForm() {
  form.customerId = undefined
  form.salesType = 'normal'
  form.deliveryDate = undefined
  form.remark = ''
  form.items = []
}

/** 把某张草稿载入右侧表单（先清空，避免和上一张混在一起） */
function loadDraft(d: BatchDraft) {
  if (!d.result) return
  resetForm()
  applyResult(d.result)
  loadedDraftId.value = d.id
  d.status = 'loaded'
  batchOpen.value = false
  ElMessage.success('已载入表单，核对无误后点「保存销售订单」')
}

function batchStatusText(s: BatchDraft['status']) {
  return ({ parsing: '识别中', ready: '待确认', loaded: '已载入', saved: '已保存', error: '失败' } as const)[s]
}

function batchStatusType(s: BatchDraft['status']) {
  return ({ parsing: 'info', ready: 'warning', loaded: 'primary', saved: 'success', error: 'danger' } as const)[s]
}

/** 用户改完转录文字，只调文本接口重新解析（**不再花视觉模型的钱**） */
async function reparse(msg: Msg) {
  const t = (msg.transcript || '').trim()
  if (!t || msg.busy) return
  msg.busy = true
  try {
    clearAiRows()
    const r = await aiSalesParse(t)
    msg.text = r.reply
    msg.transcript = r.transcript ?? t
    msg.transcriptUncertain = r.transcript_uncertain ?? false
    Object.assign(msg, applyResult(r))
    ElMessage.success('已按修改后的文字重新解析')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.detail || e?.message || '重新解析失败')
  } finally {
    msg.busy = false
  }
}

function stop() {
  ctrl?.abort()
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

// ==================== 文件 / 拖拽 / 粘贴 ====================

function pickFile() {
  fileRef.value?.click()
}

function onFileChange(e: Event) {
  const el = e.target as HTMLInputElement
  const f = el.files?.[0]
  if (f) sendImage(f)
  el.value = '' // 允许连续选同一张
}

function onDrop(e: DragEvent) {
  dragging.value = false
  const f = e.dataTransfer?.files?.[0]
  if (f) sendImage(f)
}

/** 粘贴截图 —— 用户从微信/QQ 复制一张图直接 Ctrl+V，比存文件再选快得多 */
function onPaste(e: ClipboardEvent) {
  const items = e.clipboardData?.items
  if (!items) return
  for (let i = 0; i < items.length; i++) {
    if (items[i].type.startsWith('image/')) {
      const f = items[i].getAsFile()
      if (f) {
        e.preventDefault()
        sendImage(f)
        return
      }
    }
  }
}

// ==================== 候选点选 ====================

function chooseCustomer(msg: Msg, c: SalesCustomerCandidate) {
  form.customerId = c.id
  ensureCustomerOption(c)
  msg.custPicks = undefined
  ElMessage.success(`已选客户「${c.name}」`)
}

function chooseProduct(pick: PickGroup, c: SalesProductCandidate) {
  const row = form.items.find((r: any) => r._keyword === pick.keyword)
  if (!row) return
  fillRow(row, c)
  row._match = 'matched'
  flash(row)
  ElMessage.success(`已改为「${c.name}」`)
}

// ==================== 手工加明细 / 保存 ====================

function openProductPicker(row: any) {
  currentRow.value = row
  productPickerVisible.value = true
}

function onProductSelected(p: any) {
  if (currentRow.value) {
    fillRow(currentRow.value, { ...p, tier: 'name' } as SalesProductCandidate)
    currentRow.value._match = 'matched'
    flash(currentRow.value)
  }
}

function addRow() {
  form.items.push({
    productId: undefined,
    productCode: '',
    productName: '',
    spec: '',
    unit: '',
    quantity: 1,
    price: undefined,
    taxRate: 13,
    deliveryDate: undefined
  })
}

function rowAmount(row: any) {
  const q = Number(row.quantity) || 0
  const p = Number(row.price) || 0
  return (q * p).toFixed(2)
}

const totalAmount = computed(() =>
  form.items.reduce((s: number, r: any) => s + (Number(r.quantity) || 0) * (Number(r.price) || 0), 0)
)

async function save() {
  if (!form.customerId) {
    ElMessage.warning('请先选择客户')
    return
  }
  if (!form.items.length) {
    ElMessage.warning('请至少添加一条明细')
    return
  }
  for (const it of form.items) {
    if (!it.productId) {
      ElMessage.warning('有明细还没选产品')
      return
    }
    if (!it.quantity || Number(it.quantity) <= 0) {
      ElMessage.warning(`「${it.productName}」没填数量`)
      return
    }
    // 单价允许 0（赠品），但**不允许空** —— 后端 null 会当 0，保存出来是一张 0 元单
    if (it.price === undefined || it.price === null || it.price === '') {
      ElMessage.warning(`「${it.productName}」没填单价`)
      return
    }
  }

  saving.value = true
  try {
    // ⚠️ 只提交后端 DTO 要的字段（`_keyword` / `_match` / `_flash` 不传）
    const created: any = await salesOrderCreate({
      customerId: form.customerId,
      salesType: form.salesType || 'normal',
      deliveryDate: form.deliveryDate,
      salesman: form.salesman,
      remark: form.remark,
      items: form.items.map((i: any) => ({
        productId: i.productId,
        spec: i.spec,
        unit: i.unit,
        quantity: Number(i.quantity),
        price: Number(i.price),
        taxRate: Number(i.taxRate) || 13,
        deliveryDate: i.deliveryDate,
        remark: i.remark
      }))
    })

    const no = created?.orderNo ? ` ${created.orderNo}` : ''
    ElMessage.success(`销售订单已保存${no}（状态：待提交）`)

    // 批量队列里，把刚保存的那张标记为「已保存」
    if (loadedDraftId.value !== null) {
      const d = batchDrafts.value.find((x) => x.id === loadedDraftId.value)
      if (d) d.status = 'saved'
      loadedDraftId.value = null
    }

    // 🔴 保存成功后要把**整张表单清干净**。
    //    之前只清了 items 和 remark，客户和交货日期还留着 ——
    //    用户看到右边还是满的，第一反应就是「没重置」。
    form.customerId = undefined
    form.deliveryDate = undefined
    form.remark = ''
    form.items = []
    // 业务员**不清**：它是当前登录人，每单都要填，清掉反而是负担

    // 在对话里留一条凭证：否则聊天记录里那句「已填入右侧」还挂着，
    // 而右边已经空了，看着像出 bug。
    msgs.value.push({
      role: 'ai',
      text:
        `✅ 已保存为销售订单${no}，右侧表单已清空，可以接着开下一单。\n` +
        `去「销售管理 → 销售订单」就能看到这张单。`
    })
    scrollEnd()
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="ord">
    <!-- ===== 顶部条 ===== -->
    <header class="ord__head">
      <el-button link :icon="ArrowLeft" @click="router.push('/ai-brain')">返回</el-button>
      <el-icon class="ord__head-icon"><ShoppingBag /></el-icon>
      <span class="ord__head-title">AI 销售助手 · 智能销售订单</span>
      <span class="ord__head-hint">打字或传图，自动填单</span>
    </header>

    <div class="ord__body">
      <!-- ===== 左：对话 ===== -->
      <section
        class="chat"
        :class="{ 'chat--drag': dragging }"
        @dragover.prevent="dragging = true"
        @dragleave.prevent="dragging = false"
        @drop.prevent="onDrop"
      >
        <div class="chat__title">
          <el-icon><ChatDotRound /></el-icon>
          AI 销售助手
          <span class="chat__title-tip">可拖入图片 / Ctrl+V 粘贴截图</span>
        </div>

        <div ref="listRef" class="chat__list">
          <div v-for="(m, i) in msgs" :key="i" class="chat__row" :class="m.role">
            <div class="chat__bubble" :class="{ 'chat__bubble--err': m.error }">
              <img v-if="m.image" :src="m.image" class="chat__thumb" alt="上传的图片" />

              <span v-if="m.loading" class="dots">正在处理<em>.</em><em>.</em><em>.</em></span>
              <template v-else>{{ m.text }}</template>

              <!-- ===== 转录原文：可编辑 + 可重新解析 ===== -->
              <div v-if="m.transcript !== undefined" class="tr">
                <div class="tr__head">
                  <span class="tr__title">我把这张图读成了：</span>
                  <el-tag v-if="m.transcriptUncertain" size="small" type="warning" effect="plain">
                    有拿不准的字（标 [?] 的地方）
                  </el-tag>
                  <el-tag v-else size="small" type="info" effect="plain">请核对</el-tag>
                </div>
                <textarea v-model="m.transcript" class="tr__text" rows="6" />
                <div class="tr__foot">
                  <span class="tr__hint">
                    读错的地方直接改。改完点右边的按钮 —— 只会重新解析文字，不再调用视觉模型
                  </span>
                  <el-button size="small" :loading="m.busy" @click="reparse(m)">重新解析</el-button>
                </div>
              </div>

              <!-- ===== 候选：客户 ===== -->
              <div v-if="m.custPicks?.length" class="picks">
                <div class="pick__label">有几个相近的客户，点一个确认：</div>
                <div class="pick__opts">
                  <button
                    v-for="c in m.custPicks"
                    :key="c.id"
                    class="pick__opt"
                    :title="`${c.code} ${c.name}`"
                    @click="chooseCustomer(m, c)"
                  >
                    {{ c.name }}
                  </button>
                </div>
              </div>

              <!-- ===== 候选：明细产品 ===== -->
              <div v-if="m.itemPicks?.length" class="picks">
                <div v-for="g in m.itemPicks" :key="g.keyword" class="pick">
                  <div class="pick__label">「{{ g.keyword }}」是哪个？点一下替换</div>
                  <div class="pick__opts">
                    <button
                      v-for="c in g.candidates"
                      :key="c.id"
                      class="pick__opt"
                      :title="`${c.code} ${c.name}`"
                      @click="chooseProduct(g, c)"
                    >
                      {{ c.name }}
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="chat__foot">
          <input ref="fileRef" type="file" accept="image/*" hidden @change="onFileChange" />
          <!-- 输入区做成**一个圆角容器**：文字在上面，操作按钮在下方内嵌一排。
               和 ChatGPT / 飞书 这类主流对话输入一致 —— 比「按钮带边框浮在外面」干净得多 -->
          <div class="composer">
            <textarea
              v-model="input"
              class="composer__input"
              rows="2"
              placeholder="例：卖给东莞恒辉灯饰，LED平板灯 100 盏，单价 11.1，下周五要"
              :disabled="sending"
              @keydown="onKeydown"
              @paste="onPaste"
            />
            <div class="composer__bar">
              <el-tooltip content="上传订单图片（也支持拖入 / Ctrl+V 粘贴）" placement="top">
                <button
                  type="button"
                  class="composer__attach"
                  :disabled="sending"
                  @click="pickFile"
                >
                  <el-icon><Paperclip /></el-icon>
                </button>
              </el-tooltip>
              <el-tooltip content="批量导入多张图片（逐张确认，不会自动下单）" placement="top">
                <button type="button" class="composer__attach" :disabled="batchRunning" @click="openBatch">
                  <el-icon><Files /></el-icon>
                </button>
              </el-tooltip>
              <span class="composer__gap" />
              <el-button v-if="sending" size="small" @click="stop">停止</el-button>
              <el-button
                v-else
                type="primary"
                size="small"
                :disabled="!input.trim()"
                @click="send"
              >
                发送
              </el-button>
            </div>
          </div>
        </div>
      </section>

      <!-- ===== 右：单据表单 ===== -->
      <section class="doc">
        <div class="doc__title">
          新增销售订单
          <span class="doc__title-hint">与 ERP「销售订单」同一套字段</span>
        </div>

        <el-form label-width="76px" class="doc__form">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="客户" required>
                <el-select
                  v-model="form.customerId"
                  filterable
                  remote
                  :remote-method="searchCustomer"
                  :loading="customerLoading"
                  placeholder="搜索客户"
                  clearable
                  style="width: 100%"
                >
                  <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="销售类型">
                <el-select v-model="form.salesType" style="width: 100%">
                  <el-option label="普通销售" value="normal" />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="交货日期">
                <el-date-picker
                  v-model="form.deliveryDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="业务员">
                <el-input v-model="form.salesman" />
              </el-form-item>
            </el-col>
            <el-col :span="24">
              <el-form-item label="备注">
                <el-input v-model="form.remark" placeholder="选填" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="doc__bar">
          <span class="doc__bar-title">
            销售明细
            <span v-if="form.items.length" class="doc__bar-sum">
              合计 ¥{{ totalAmount.toFixed(2) }}（含税，以后端计算为准）
            </span>
          </span>
          <el-button size="small" @click="addRow">添加明细</el-button>
        </div>

        <el-table
          :data="form.items"
          border
          size="small"
          class="doc__table"
          empty-text="还没有明细 —— 左侧说一句 / 传张图，或点「添加明细」"
        >
          <el-table-column label="产品" min-width="190">
            <template #default="{ row }">
              <div class="mat" :class="{ 'mat--flash': row._flash }">
                <div class="mat__main">
                  <span class="mat__name">{{ row.productName || '未选择产品' }}</span>
                  <el-tag v-if="row._match === 'ambiguous'" size="small" type="warning" effect="plain">
                    待确认
                  </el-tag>
                </div>
                <el-button link size="small" type="primary" @click="openProductPicker(row)">
                  {{ row.productId ? '更换' : '选择' }}
                </el-button>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="规格" width="90" show-overflow-tooltip />
          <el-table-column label="数量" width="92">
            <template #default="{ row }">
              <el-input v-model="row.quantity" type="number" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="单价" width="100">
            <template #default="{ row }">
              <el-input v-model="row.price" type="number" size="small" placeholder="必填" />
            </template>
          </el-table-column>
          <el-table-column label="税率%" width="72">
            <template #default="{ row }">
              <el-input v-model="row.taxRate" type="number" size="small" />
            </template>
          </el-table-column>
          <el-table-column prop="unit" label="单位" width="62" />
          <el-table-column label="金额" width="92" align="right">
            <template #default="{ row }">{{ rowAmount(row) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="62">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.items.splice($index, 1)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="doc__foot">
          <!-- 🔴 AI 只填表、不提交：这里永远是**人来点**保存 -->
          <el-button type="primary" :loading="saving" @click="save">保存销售订单</el-button>
        </div>
      </section>
    </div>

    <ProductPicker v-model="productPickerVisible" @select="onProductSelected" />

    <!-- 批量导入：多图 → 草稿队列。逐张确认，绝不一键全下单 -->
    <el-dialog v-model="batchOpen" title="批量导入订单图片" width="760px" top="6vh">
      <div class="batch-bar">
        <el-button size="small" :disabled="batchRunning" @click="pickBatchFiles">选择图片（可多选）</el-button>
        <input ref="batchInputRef" type="file" accept="image/*" multiple hidden @change="onBatchFiles" />
        <span class="batch-tip">逐张解析 → 逐张核对 → 逐张保存。<b>不会自动下单。</b></span>
        <span v-if="batchRunning" class="batch-run">解析中…</span>
      </div>

      <div class="batch-list">
        <div v-for="d in batchDrafts" :key="d.id" class="batch-item">
          <img v-if="d.thumb" :src="d.thumb" class="batch-thumb" alt="" />
          <div v-else class="batch-thumb batch-thumb--empty" />
          <div class="batch-body">
            <div class="batch-name">{{ d.name }}</div>
            <div class="batch-tr">
              <span v-if="d.uncertain" class="batch-warn">[含不确定字]</span>
              {{ d.transcript || (d.status === 'parsing' ? '识别中…' : '—') }}
            </div>
            <div v-if="d.error" class="batch-err">{{ d.error }}</div>
          </div>
          <div class="batch-actions">
            <el-tag size="small" :type="batchStatusType(d.status)">{{ batchStatusText(d.status) }}</el-tag>
            <el-button
              size="small"
              type="primary"
              plain
              :disabled="d.status !== 'ready'"
              @click="loadDraft(d)"
            >
              载入表单
            </el-button>
          </div>
        </div>
        <div v-if="!batchDrafts.length" class="batch-empty">
          还没选图片。点「选择图片」一次选多张，会逐张识别。
        </div>
      </div>

      <template #footer>
        <span class="batch-foot">已保存 {{ batchDrafts.filter((x) => x.status === 'saved').length }} / {{ batchDrafts.length }}</span>
        <el-button @click="batchOpen = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.ord {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 132px);
  min-height: 460px;
  background: #f7f7f5;
}

/* ---------- 顶部条 ---------- */
.ord__head {
  flex: none;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e6e4de;
}
.ord__head-icon {
  color: #d32f2f;
  font-size: 18px;
}
.ord__head-title {
  font-size: 14px;
  font-weight: 500;
  color: #2c2c2a;
}
.ord__head-hint {
  font-size: 12px;
  color: #a8a69d;
}

/* ---------- 两栏 ---------- */
.ord__body {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: 12px;
  padding: 12px;
}

/* ---------- 左：对话 ---------- */
.chat {
  width: 380px;
  flex: none;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
  border: 1px solid #e6e4de;
  border-radius: 12px;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}
/* 拖拽悬停时整块高亮，给明确的落点提示 */
.chat--drag {
  border-color: #d32f2f;
  box-shadow: 0 0 0 2px rgba(211, 47, 47, 0.12) inset;
}
.chat__title {
  flex: none;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  font-size: 12.5px;
  font-weight: 500;
  color: #d32f2f;
  border-bottom: 1px solid #f0efe9;
}
.chat__title-tip {
  margin-left: auto;
  font-size: 11px;
  font-weight: 400;
  color: #b5b3aa;
}
.chat__list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chat__row {
  display: flex;
}
.chat__row.user {
  justify-content: flex-end;
}
.chat__bubble {
  max-width: 92%;
  padding: 9px 12px;
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
}
.chat__row.ai .chat__bubble {
  background: #f6f6f4;
  color: #2c2c2a;
}
.chat__row.user .chat__bubble {
  background: #d32f2f;
  color: #fff;
}
.chat__bubble--err {
  background: #fdecec !important;
  color: #c0392b !important;
}
.chat__thumb {
  display: block;
  max-width: 100%;
  max-height: 180px;
  border-radius: 6px;
  margin-bottom: 6px;
  background: #fff;
}
.chat__foot {
  flex: none;
  padding: 10px 12px;
  border-top: 1px solid #f0efe9;
}

/* ---------- 输入区：整块圆角容器，文字在上、操作按钮内嵌在下方一排 ---------- */
.composer {
  border: 1px solid #d3d1c7;
  border-radius: 10px;
  background: #fff;
  transition: border-color 0.15s ease;
}
/* 焦点态做到容器上 —— 输入框本身已经没有边框了 */
.composer:focus-within {
  border-color: #d32f2f;
}
.composer__input {
  display: block;
  width: 100%;
  box-sizing: border-box;
  resize: none;
  padding: 8px 10px 0;
  border: none;
  outline: none;
  background: transparent;
  font-size: 12.5px;
  font-family: inherit;
  line-height: 1.6;
  color: #2c2c2a;
}
.composer__input::placeholder {
  color: #b5b3aa;
}
.composer__input:disabled {
  background: transparent;
  cursor: not-allowed;
}
.composer__bar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 6px 6px;
}
/* 回形针：无边框、无背景，hover 才给浅底 —— 和微信/飞书/ChatGPT 一致 */
.composer__attach {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: #888780;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}
.composer__attach:hover:not(:disabled) {
  background: #f5f4f0;
  color: #d32f2f;
}
.composer__attach:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
/* 把「发送」推到最右 */
.composer__gap {
  flex: 1;
}

/* 等待动画 */
.dots em {
  font-style: normal;
  animation: blink 1.2s infinite;
}
.dots em:nth-child(2) {
  animation-delay: 0.2s;
}
.dots em:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes blink {
  0%, 60%, 100% { opacity: 0.25; }
  30% { opacity: 1; }
}

/* ---------- 转录可编辑区 ---------- */
.tr {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #dcdad2;
}
.tr__head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}
.tr__title {
  font-size: 12px;
  color: #5f5e5a;
}
.tr__text {
  width: 100%;
  box-sizing: border-box;
  resize: vertical;
  padding: 8px;
  border: 1px solid #d3d1c7;
  border-radius: 6px;
  font-family: ui-monospace, Consolas, monospace;
  font-size: 12px;
  line-height: 1.6;
  color: #2c2c2a;
  background: #fff;
  outline: none;
}
.tr__text:focus {
  border-color: #d32f2f;
}
.tr__foot {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
.tr__hint {
  flex: 1;
  font-size: 11px;
  line-height: 1.5;
  color: #a8a69d;
}

/* ---------- 候选点选 ---------- */
.picks {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #dcdad2;
}
.pick + .pick {
  margin-top: 8px;
}
.pick__label {
  font-size: 12px;
  color: #888780;
  margin-bottom: 6px;
}
.pick__opts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.pick__opt {
  padding: 3px 9px;
  border: 1px solid #e6c9c9;
  border-radius: 99px;
  background: #fff;
  color: #d32f2f;
  font-size: 12px;
  font-family: inherit;
  cursor: pointer;
}
.pick__opt:hover {
  background: #fdeaea;
  border-color: #d32f2f;
}

/* ---------- 右：表单 ---------- */
.doc {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
  border: 1px solid #e6e4de;
  border-radius: 12px;
  padding: 14px 16px;
}
.doc__title {
  flex: none;
  font-size: 14px;
  font-weight: 500;
  color: #2c2c2a;
  margin-bottom: 12px;
}
.doc__title-hint {
  margin-left: 10px;
  font-size: 12px;
  font-weight: 400;
  color: #a8a69d;
}
.doc__form {
  flex: none;
}
.doc__bar {
  flex: none;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 8px;
}
.doc__bar-title {
  font-size: 13px;
  color: #5f5e5a;
}
.doc__bar-sum {
  margin-left: 10px;
  font-size: 12px;
  color: #d32f2f;
}
.doc__table {
  flex: 1;
  min-height: 0;
}
.doc__foot {
  flex: none;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
}

/* 产品单元格 + 刚被填进来的高亮 */
.mat {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: -4px -6px;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background 0.4s ease;
}
.mat__main {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 5px;
}
.mat__name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mat--flash {
  background: #fff3cd;
  box-shadow: inset 2px 0 0 #e6a700;
  transition: none;
}

/* ---------- 批量导入草稿队列 ---------- */
.batch-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.batch-tip {
  font-size: 12px;
  color: #64748b;
}
.batch-run {
  font-size: 12px;
  color: #0057c2;
}
.batch-list {
  max-height: 56vh;
  overflow-y: auto;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
}
.batch-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 10px;
  border-bottom: 1px solid #f1f5f9;
}
.batch-item:last-child {
  border-bottom: none;
}
.batch-thumb {
  width: 64px;
  height: 64px;
  flex: none;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  background: #f8fafc;
}
.batch-thumb--empty {
  display: block;
}
.batch-body {
  flex: 1;
  min-width: 0;
}
.batch-name {
  font-size: 12px;
  color: #0f172a;
  margin-bottom: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.batch-tr {
  font-size: 12px;
  color: #475569;
  line-height: 1.5;
  max-height: 44px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.batch-warn {
  color: #d97706;
  font-size: 11px;
}
.batch-err {
  font-size: 12px;
  color: #dc2626;
  margin-top: 2px;
}
.batch-actions {
  flex: none;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 6px;
}
.batch-empty {
  padding: 30px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
}
.batch-foot {
  margin-right: 12px;
  font-size: 12px;
  color: #64748b;
}
</style>
