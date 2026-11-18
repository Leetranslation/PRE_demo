import request from '@/utils/request'

/** token 存在 localStorage 的 'token'，每次现取（用户可能退出再登录） */
function authHeader(): Record<string, string> {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
}

/** 一行评估结果（对齐「AI交期风险智能预警」弹窗表格） */
export interface RiskRow {
  itemId: number
  productId: number
  productCode: string
  productName: string
  spec?: string
  unit?: string
  quantity: number
  deliveryDate?: string
  riskLevel: 'on_time' | 'at_risk' | 'late'
  riskText: string
  suggestDate?: string | null
  earliestActionDate?: string | null
  stockQty: number
  shortageQty: number
  cycleDays: number
  detail?: Record<string, any>
}

/** 分析过程的单条推送 */
export interface AnalysisStep {
  key: string
  name: string
  status: 'running' | 'ok' | 'error'
  message: string
}

export interface ReportSection {
  title: string
  body: string
}

export type BizType = 'sales_order' | 'quotation' | 'contract'

/** 评估（非流式，弹窗首次打开时用） */
export function evaluateRisk(bizType: BizType, bizId: number) {
  return request.post('/sales/delivery-risk/evaluate', null, {
    params: { bizType, bizId }
  }) as Promise<RiskRow[]>
}

/** 明细项（新建单据还没有 id，直接传内存数据） */
export interface InlineItem {
  productId?: number
  quantity?: number
  deliveryDate?: string | null
  spec?: string
  unit?: string
}

/**
 * 内存明细评估 —— 新建单据未保存时用。
 * 返回的 `itemId` 是行下标（从 1 开始），不是数据库 id。
 */
export function evaluateRiskInline(items: InlineItem[]) {
  return request.post('/sales/delivery-risk/evaluate-inline', items) as Promise<RiskRow[]>
}

/** 内存明细流式分析 */
export async function analyzeRiskInlineStream(
  items: InlineItem[],
  onEvent: (type: string, data: any) => void,
  signal?: AbortSignal
) {
  return sseRequest('/api/sales/delivery-risk/analyze-inline', {
    method: 'POST',
    body: JSON.stringify(items)
  }, onEvent, signal)
}

/** 采纳建议：把建议交付日期写回明细行 */
export function applyRiskSuggest(bizType: BizType, bizId: number, itemIds?: number[]) {
  return request.post('/sales/delivery-risk/apply', { bizType, bizId, itemIds }) as Promise<number>
}

/**
 * 流式分析（「查看分析」用）。
 *
 * ⚠️ 不能用 EventSource —— 它没法带 Authorization 头。
 *    和 `api/ai.ts` 里的 SSE 一样，用 fetch + ReadableStream 自己拆帧。
 */
export async function analyzeRiskStream(
  bizType: BizType,
  bizId: number,
  onEvent: (type: string, data: any) => void,
  signal?: AbortSignal
) {
  return sseRequest(
    `/api/sales/delivery-risk/analyze-stream?bizType=${bizType}&bizId=${bizId}`,
    { method: 'GET' },
    onEvent,
    signal
  )
}

/**
 * 通用 SSE 读流。
 *
 * ⚠️ 不能用 EventSource —— 它没法带 Authorization 头。
 *    和 `api/ai.ts` 里的 SSE 一样，用 fetch + ReadableStream 自己拆帧。
 */
async function sseRequest(
  url: string,
  init: { method: string; body?: string },
  onEvent: (type: string, data: any) => void,
  signal?: AbortSignal
) {
  const res = await fetch(url, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...authHeader()
    },
    signal
  })
  if (!res.ok) throw new Error(`HTTP ${res.status}`)
  if (!res.body) throw new Error('响应没有 body')

  const reader = res.body.getReader()
  const decoder = new TextDecoder('utf-8')
  let buf = ''

  const drain = (flush = false) => {
    for (;;) {
      const m = buf.match(/\r?\n\r?\n/)
      if (!m) break
      const idx = m.index as number
      const frame = buf.slice(0, idx)
      buf = buf.slice(idx + m[0].length)
      let name = 'message'
      const dataLines: string[] = []
      for (const line of frame.split(/\r?\n/)) {
        if (line.startsWith('event:')) name = line.slice(6).trim()
        else if (line.startsWith('data:')) dataLines.push(line.slice(5).trim())
      }
      if (!dataLines.length) continue
      const raw = dataLines.join('\n')
      let payload: any = raw
      try {
        payload = JSON.parse(raw)
      } catch {
        /* 非 JSON 原样给出去 */
      }
      onEvent(name, payload)
    }
    if (flush && buf.trim()) buf = ''
  }

  for (;;) {
    const { done, value } = await reader.read()
    if (done) break
    buf += decoder.decode(value, { stream: true })
    drain()
  }
  drain(true)
}
