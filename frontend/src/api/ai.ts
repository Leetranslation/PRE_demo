import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/**
 * AI 服务（Python FastAPI，默认 127.0.0.1:8000）的调用层。
 *
 * 为什么不复用 `utils/request.ts`：
 *   1. 它的 baseURL 是 `/api`，而 `/api` 已被 vite 代理给 Java 后端（8080），会撞名 ——
 *      AI 服务统一走 `/ai` 前缀（vite.config.ts 里有对应代理）
 *   2. 它会按 `code === 0` 拆包、错误时弹 ElMessage。AI 服务的返回是
 *      `{answer, sources, hidden}`，没有 code 字段，协议不一样
 *   3. LLM 生成慢，超时要从 15s 放宽到 60s
 */
const ai = axios.create({
  baseURL: '/ai',
  timeout: 60000
})

/**
 * 取当前登录 token 拼成请求头。
 * 为什么每次现取、而不是模块加载时取一次：用户会退出再登录，token 是变的。
 */
function authHeader(): Record<string, string> {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/** 登录失效的统一处理 —— 行为对齐 `utils/request.ts`，避免两处表现不一致 */
function onUnauthorized() {
  ElMessage.error('登录已失效，请重新登录')
  localStorage.removeItem('token')
  router.push('/login')
}

/**
 * 请求拦截：注入 token。
 * AI 服务是**独立进程**（8000），Java 后端的门禁管不到它，
 * 所以必须由请求方自己把 JWT 带过去（后端在 `auth.py` 里用同一密钥验签）。
 */
ai.interceptors.request.use((config) => {
  Object.assign(config.headers, authHeader())
  return config
})

/**
 * 响应拦截：**只处理 401**，其余错误原样透出。
 * ⚠️ 不要在这里拆包（`r => r.data`）—— 调用方的 `.then((r) => r.data)` 依赖原始响应。
 */
ai.interceptors.response.use(
  (r) => r,
  (error) => {
    if (error?.response?.status === 401) onUnauthorized()
    return Promise.reject(error)
  }
)

export interface AiSource {
  file: string
  text: string
  /** 向量检索的相似度；精确查找命中时为 null */
  score: number | null
  /** vector = 语义检索；lookup = 结构化语料精确命中 */
  via: 'vector' | 'lookup'
  /** 精确命中时的词条键，如 `sales_order 销售订单` */
  key: string | null
}

export interface AiAnswer {
  answer: string
  sources: AiSource[]
  /** 被截断掉的低相关片段数 */
  hidden: number
}

export interface AiHealth {
  status: string
  chunks: number
  structured: number
  embed_model: string
  chat_model: string
  trim: string
  /** AI 服务是否要求鉴权（排障时肉眼确认跑的是哪版配置） */
  auth_enabled?: boolean
  jwt_secret_loaded?: boolean
}

export function aiHealth(): Promise<AiHealth> {
  return ai.get('/health').then((r) => r.data)
}

export function aiChat(question: string, topK = 5): Promise<AiAnswer> {
  return ai.post('/chat', { question, top_k: topK }).then((r) => r.data)
}

// ============================================================================
// 通用 SSE 消费（知识库问答 / 智能问数 共用）
// ============================================================================

/**
 * 通用 SSE：POST 一个 JSON，逐帧解析 `data:` 并回调。
 *
 * ## 为什么不用浏览器原生 `EventSource`
 *
 *   1. **它只支持 GET** —— 问题文本得塞进 URL（中文要 encode，长问题会被截断）
 *   2. **它会自动重连** —— 我们不想让一个失败的查询被悄悄重跑（**还要计费**）
 *   3. **它加不了自定义请求头** —— 而 AI 服务要校验 JWT（见后端 `auth.py`），
 *      必须能带 `Authorization`
 *
 * → 用 `fetch` 拿 `ReadableStream` 自己切帧。这也是生产里的主流做法。
 *
 * ## 解析要点（都是踩过的）
 *   - `\r\n\r\n` 和 `\n\n` **都要认**（不同服务端/代理换行不一致）
 *   - `decoder.decode(value, {stream:true})` —— 不传 `stream:true`，
 *     一个中文跨在两个 chunk 之间会被解成乱码
 *   - **单帧 JSON 坏掉只跳过它**，不能炸掉整条流
 *   - 流结束后缓冲区可能还剩最后一帧（服务端没以空行收尾）→ 兜底解一次
 */
async function postSse<T>(
  url: string,
  body: unknown,
  onEvent: (ev: T) => void,
  signal?: AbortSignal
): Promise<void> {
  const res = await fetch(url, {
    method: 'POST',
    // ⚠️ 这里走的是**原生 fetch**，axios 的拦截器管不到它 → token 必须手工带上。
    //    （这正是不用 `EventSource` 的又一个好处：原生 fetch 能带请求头）
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...authHeader()
    },
    body: JSON.stringify(body),
    signal
  })

  // 401 单独处理：给一句人话，而不是「HTTP 401」
  if (res.status === 401) {
    onUnauthorized()
    throw new Error('登录已失效，请重新登录')
  }
  if (!res.ok) {
    // 后端没起来 / 404 / 500 —— 尽量把响应体读出来，比「HTTP 500」有信息量
    const detail = await res.text().catch(() => '')
    throw new Error(`HTTP ${res.status}${detail ? '：' + detail.slice(0, 200) : ''}`)
  }
  if (!res.body) throw new Error('响应没有 body（该浏览器不支持 ReadableStream？）')

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''

  /** 从缓冲区里取出所有完整的 SSE 帧并回调 */
  const drain = (flush = false) => {
    for (;;) {
      const m = buf.match(/\r?\n\r?\n/)
      if (!m) break
      const idx = m.index as number
      const raw = buf.slice(0, idx)
      buf = buf.slice(idx + m[0].length)

      // 一个帧里可能有多行；我们只关心 `data:` 行（后端每帧只发一行 data）
      const dataLines = raw
        .split(/\r?\n/)
        .filter((l) => l.startsWith('data:'))
        .map((l) => l.slice(5).replace(/^ /, '')) // 去掉 `data:` 后的一个空格
      if (!dataLines.length) continue

      const payload = dataLines.join('\n')
      if (!payload.trim()) continue
      try {
        onEvent(JSON.parse(payload) as T)
      } catch {
        // 单帧 JSON 坏了不该炸掉整条流 —— 跳过它，后面的还能用
      }
    }
    // 流结束后缓冲区可能还剩最后一帧（服务端没以空行收尾）→ 兜底解一次
    if (flush && buf.trim()) {
      const dataLines = buf
        .split(/\r?\n/)
        .filter((l) => l.startsWith('data:'))
        .map((l) => l.slice(5).replace(/^ /, ''))
      if (dataLines.length) {
        try {
          onEvent(JSON.parse(dataLines.join('\n')) as T)
        } catch {
          /* 末尾半帧坏了就算了 */
        }
      }
      buf = ''
    }
  }

  try {
    for (;;) {
      const { done, value } = await reader.read()
      if (done) break
      // ⚠️ stream: true —— 否则一个中文/emoji 跨在两个 chunk 之间会被解成乱码
      buf += decoder.decode(value, { stream: true })
      drain()
    }
    buf += decoder.decode() // 冲掉解码器内部残留
    drain(true)
  } finally {
    // 无论是正常读完、还是用户点了「停止」，都要释放 reader
    reader.releaseLock?.()
  }
}

/**
 * 知识库问答的流式事件（后端 `rag_core.answer_stream()` 发出）。
 *
 * ⚠️ 顺序固定：`sources` 一定最先（检索只要 ~0.7s，比答案快得多），
 *    然后是 `delta`（逐块答案），最后 `done`。
 */
export type KbStreamEvent =
  | { type: 'sources'; sources: AiSource[]; hidden: number }
  /** 推理模型的「思考过程」。当前后端已关掉思考（见 `rag_core.DISABLE_THINKING`），
   *  正常不会出现；保留这个类型是为了「万一打开思考」时前端有得处理。 */
  | { type: 'reasoning'; text: string }
  | { type: 'delta'; text: string }
  | { type: 'done' }
  | { type: 'error'; message: string }

/**
 * 知识库问答 —— **SSE 流式**（打字机效果）。
 *
 * 和 `aiChat()` 的区别：那个等全部生成完一次性返回（约 2.5 秒后才看到字），
 * 这个边生成边推。**token 用量和费用完全一样**，差的只是「什么时候给你看」。
 *
 * @param onEvent 每个事件回调一次：`sources` 先到（前端可先存着不渲染）、
 *                随后 `delta` 逐块追加、最后 `done`
 */
export function aiChatStream(
  question: string,
  onEvent: (ev: KbStreamEvent) => void,
  signal?: AbortSignal,
  topK = 5
): Promise<void> {
  return postSse<KbStreamEvent>('/ai/chat/stream', { question, top_k: topK }, onEvent, signal)
}

// ============================================================================
// 智能问数（ChatBI）
// ============================================================================

/**
 * 表格。
 * ⚠️ `rows` 是**二维数组**（`[[名称, "¥1,234.00"]]`），不是对象数组 ——
 *    `columns` 与每行的列一一对应。
 * ⚠️ 数值**后端已格式化成字符串**（带 `¥`、千分位、`%`），前端**不要重复格式化**，
 *    否则 `¥` 会叠两个。直接渲染。
 */
export interface BiTable {
  columns: string[]
  rows: (string | number | null)[][]
  /** 比值型指标不可加总 → 这里为 null，前端据此不显示合计行 */
  total: number | null
  total_label?: string
}

/**
 * 图表。
 * ⚠️ 后端 `present.build_chart()` 返回的**就是 ECharts option 本身**（或 null），
 *    没有 `{type, option}` 这层包装。前端拿 `chart.series[0].type` 判类型。
 */
export type BiChart = Record<string, any> | null

export interface BiMeta {
  metric?: string
  unit?: string
  dimension?: string | null
  time_range?: string | null
  granularity?: string | null
  /** 比值型指标为 false —— 禁止「合计/占总额比例」的措辞 */
  additive?: boolean
  row_count?: number
  elapsed_ms?: number
  truncated?: boolean
  /**
   * 统计口径（指标库里的 `description`）。
   * 在结论下面显示成一行小字 —— 同一句话可能有多种口径，光看数字分不清。
   */
  description?: string | null
  /**
   * 单据类型筛选（跨类型汇总指标专用）。
   * ⚠️ 必须和 `description` 一起显示：口径描述写的是「含销售/采购/生产/委外」，
   *    但筛选后实际只统计了其中几种，只显示口径会让人误以为那是全量。
   */
  doc_types?: string[] | null
}

/**
 * SSE 事件（v2：7 步 + 双进度）。
 *
 * 后端 `chatbi/pipeline.py` 的步骤顺序固定：
 *   plan → metric → intent → sql → data → visual → conclusion → done
 *
 * 每步会先发一个 `status='running'`（卡片置为「进行中」），
 * 再发一个 `status='ok'`（打勾）。`plan` 是瞬时的，只发一次 ok。
 *
 * 🔴 两个百分比，别混：
 *   - `percent`      **总进度**（顶部大进度条）—— 按步数平均推进，7 步每步 ≈14.3
 *   - `step_percent` **该卡片自己的完成度** —— running=0，ok/rejected/error=100
 *
 * 🔴 错误分级（前端要分开显示）：
 *   - `status='rejected'`：**「问不了」** —— 指标不存在 / 维度不支持。
 *     这是**正常的业务结果**，不是故障，用黄色提示。
 *   - `status='error'`：**「程序坏了」** —— SQL 报错 / 连不上库，用红色。
 */
export type BiStepKey =
  | 'plan'
  | 'metric'
  | 'intent'
  | 'sql'
  | 'data'
  | 'visual'
  | 'conclusion'
  | 'done'

export interface BiEvent {
  step: BiStepKey
  status: 'ok' | 'running' | 'rejected' | 'error'
  /** 第几步（1-based） */
  step_index?: number
  step_total?: number
  /** 总进度 0~100（顶部进度条） */
  percent?: number
  /** 该步骤卡片自己的完成度 0 / 100 */
  step_percent?: number
  /** 卡片标题，如「用户意图理解」 */
  step_name?: string
  /** 卡片主文案（一句话） */
  message?: string
  /** 卡片内的「字段名：值」列表（意图理解那一步用） */
  lines?: string[]
  /** 意图理解结果（原始 JSON，调试用） */
  definition?: Record<string, any>
  problems?: string[]
  sql?: string
  params?: any[]
  row_count?: number
  elapsed_ms?: number
  truncated?: boolean
  /** 第 ⑥ 步一次性给表格 + 图表 + meta */
  table?: BiTable
  chart?: BiChart
  meta?: BiMeta
  /** 第 ⑦ 步的结论文字 */
  conclusion?: string
  ok?: boolean
}

/**
 * 智能问数 —— **SSE 流式**（7 步事件模型，逐条推思考过程）。
 *
 * 解析逻辑走通用的 `postSse()`（和知识库问答共用）；这里只负责
 * 「端点 + 请求体 + 事件类型」这三件与业务相关的事。
 *
 * @param question 自然语言问题
 * @param onEvent  每收到一个事件回调一次（逐条驱动 UI）
 * @param signal   用于「停止生成」—— 传 AbortController.signal
 */
export function aiChatBiStream(
  question: string,
  onEvent: (ev: BiEvent) => void,
  signal?: AbortSignal
): Promise<void> {
  return postSse<BiEvent>('/ai/chatbi/ask', { question }, onEvent, signal)
}

// ============================================================================
// AI 采购助手（智能下单）
// ============================================================================

/**
 * 物料候选。
 *
 * 🔴 `code` / `id` 来自**后端查库**（`purchase/matcher.py`），不是模型编的 ——
 *    这是整个方案最核心的一条：模型只交出口语说法，编码由代码查。
 */
export interface PurchaseCandidate {
  id: number
  code: string
  name: string
  spec: string | null
  unit: string | null
  category: string | null
  /** 命中方式。越靠前越确定：code > name > name_like > spec_like > token_all > token */
  match_type: 'code' | 'name' | 'name_like' | 'spec_like' | 'token_all' | 'token'
  /** 关键词里有几个「字母数字片段」命中（只有 token* 级才有意义） */
  hits: number
}

/** 一句话里解析出的**一条**采购需求 + 它的匹配结论 */
export interface PurchaseParsedItem {
  /** 用户原话里的物料说法，如「M3螺丝」 */
  keyword: string
  quantity: number | null
  requiredDate: string | null
  remark: string | null
  /**
   * `matched`  唯一解（或精确命中）→ 可以直接填
   * `ambiguous` 多个候选      → 已按第 1 名填了，但要让用户确认
   * `none`      库里没有      → **不填**，让用户补说法或手工选
   */
  status: 'matched' | 'ambiguous' | 'none'
  /** 第 1 名候选（`none` 时为 null） */
  best: PurchaseCandidate | null
  candidates: PurchaseCandidate[]
}

export interface PurchaseParseResult {
  /** 给用户看的说明（**后端模板拼的**，不是模型写的） */
  reply: string
  header: { remark: string | null }
  items: PurchaseParsedItem[]
  elapsed_ms: number
  confidence: number
}

/**
 * AI 采购助手 —— 把一句话变成采购申请单草稿。
 *
 * ⚠️ 同步返回（不是 SSE）：整条链路只有 1 次模型调用 + 1 次 SQL，实测 1.6~2.5 秒。
 *    加流式只是在分片上做文章，用户感受不到，却要多维护一套解析逻辑。
 *
 * ⚠️ **这个接口不写库** —— 返回的只是草稿，保存走 ERP 自己的采购申请单接口。
 */
export function aiPurchaseParse(
  text: string,
  signal?: AbortSignal
): Promise<PurchaseParseResult> {
  return ai.post('/purchase/parse', { text }, { signal }).then((r) => r.data)
}

// ============================================================================
// AI 销售助手（销售订单 · 文字 + 图片）
// ============================================================================

/** 匹配结论三态（客户和产品共用） */
export type AiMatchStatus = 'matched' | 'ambiguous' | 'none'

/**
 * 客户候选。
 * `tier` 是命中方式，越靠前越确定：
 * `code`（编号）> `name`（全称）> `core`（去掉行政区和行业通用词后相等）
 * > `name_like`（全称包含）> `core_like`（归一化后部分包含）
 */
export interface SalesCustomerCandidate {
  id: number
  code: string
  name: string
  contact: string | null
  phone: string | null
  tier: 'code' | 'name' | 'core' | 'name_like' | 'core_like'
}

/** 产品候选 */
export interface SalesProductCandidate {
  id: number
  code: string
  name: string
  spec: string | null
  unit: string | null
  tier: 'code' | 'name' | 'name_like'
}

export interface SalesCustomerResult {
  /** 模型从输入里理解出来的客户说法（可能为 null） */
  keyword: string | null
  status: AiMatchStatus
  best: SalesCustomerCandidate | null
  candidates: SalesCustomerCandidate[]
  tier: string | null
}

/** 一句话/一张图里解析出的**一条**销售明细 */
export interface SalesParsedItem {
  keyword: string
  quantity: number | null
  price: number | null
  remark: string | null
  status: AiMatchStatus
  best: SalesProductCandidate | null
  candidates: SalesProductCandidate[]
  /**
   * 形如 `material:锂粉` —— 用户把**物料**当成产品说了。
   * 销售订单只能卖产品，前端要把这句话如实转告。
   */
  wrong_master: string | null
}

export interface SalesParseResult {
  /** 给用户看的说明（**后端模板拼的**，不是模型写的） */
  reply: string
  header: {
    customer: SalesCustomerResult
    deliveryDate: string | null
    salesman: string | null
    remark: string | null
  }
  items: SalesParsedItem[]
  elapsed_ms: number
  confidence: number
  /**
   * 仅图片入口有：**逐字转录原文**。
   * 🔴 必须展示给用户 —— 这是唯一能让人发现「模型读错了」的手段
   *    （模型自己报的"不确定"不可靠：实测它把「盏」读成「盖」还很自信）。
   */
  transcript?: string
  /** 转录里有模型标了 `[?]` 的拿不准的字 */
  transcript_uncertain?: boolean
  vision?: { model: string; elapsed_ms: number; usage: Record<string, any> }
}

/**
 * 文字 → 销售订单草稿。
 *
 * 图片入口也走这个（转录文本当作 text 传进来）——「改完转录重新解析」用的就是它，
 * **不会再调一次视觉模型**，只花 ¥0.007。
 */
export function aiSalesParse(text: string, signal?: AbortSignal): Promise<SalesParseResult> {
  return ai
    .post('/sales/parse', { text }, { signal, timeout: 90000 })
    .then((r) => r.data)
}

/**
 * 图片 → 销售订单草稿（后端两段式：先视觉转录，再结构化）。
 *
 * @param imageBase64 不含 `data:` 前缀的 base64
 */
export function aiSalesParseImage(
  imageBase64: string,
  mediaType: string,
  signal?: AbortSignal
): Promise<SalesParseResult> {
  return ai
    .post(
      '/sales/parse-image',
      { image_base64: imageBase64, media_type: mediaType },
      { signal, timeout: 180000 } // 视觉 + 文本两跳，实测 20s，留足余量
    )
    .then((r) => r.data)
}

export default ai
