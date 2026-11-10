<script setup lang="ts">
/**
 * AI 采购助手 · 智能下单。
 *
 * ## 布局（对齐参考产品）
 *
 *   左：对话 —— 用大白话说要采购什么
 *   右：采购申请单表单 —— AI 填好的内容落在这里，**用户确认后手工点保存**
 *
 * ## 🔴 写入边界的约定（重要）
 *
 * **AI 只填表，不提交。** 提交走 ERP 自己的「采购申请单」接口。
 * 这么定有三个理由：
 *   1. AI 侧**保持全只读** —— 三层纵深防御（模型拿不到 SQL 出口 / 只生成 SELECT /
 *      连接级只读事务）一行都不用改
 *   2. 提交复用 ERP 已有接口 → **权限、字段校验、单据编号规则全部复用**
 *   3. **用户始终是最终确认者** —— AI 不能自己下单，这也是企业软件的合规要求
 *
 * ## 这条链路怎么工作（`POST /ai/purchase/parse`）
 *
 *   用户：「采购 200 个 M3 螺丝，下周五之前要到」
 *     ↓ ① 模型（1 次调用，temp=0.0）—— 只输出结构化意图
 *   { "items": [{ "keyword": "M3螺丝", "quantity": 200, "requiredDate": "2026-10-16" }] }
 *     ↓ ② 代码查库（`purchase/matcher.py`）—— 把 keyword 落到物料编码
 *   → status=matched（唯一解）→ 自动填
 *   → status=ambiguous（多个候选）→ 先填第 1 名，同时在对话里给出可点的候选
 *   → status=none（库里没有）→ **不填**，让用户补说法或手工选
 *
 * 🔴 **物料编码永远是数据库给的**，不是模型编的 —— 模型编编码 = 幻觉下单。
 */
import { nextTick, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, ChatDotRound, ShoppingCart } from '@element-plus/icons-vue'
import { purchaseRequisitionCreate } from '@/api/purchase'
import { aiPurchaseParse, type PurchaseCandidate, type PurchaseParseResult } from '@/api/ai'
import MaterialPicker from '@/components/MaterialPicker.vue'

const router = useRouter()

// ==================== 左侧：对话 ====================

/** 一组需要用户点一下确认的候选 */
interface PickGroup {
  keyword: string
  candidates: PurchaseCandidate[]
}

interface Msg {
  role: 'ai' | 'user'
  text: string
  /** 正在等后端（显示「正在理解…」）*/
  loading?: boolean
  /** 出错 —— 用红气泡，别和正常回复混在一起 */
  error?: boolean
  /** 需要用户点一下确认的候选组 */
  picks?: PickGroup[]
}

const msgs = ref<Msg[]>([
  {
    role: 'ai',
    text:
      '您好，我是 AI 采购助手！\n' +
      '把要买的东西说清楚（物料、数量、什么时候要），我会把右边的采购申请单填好。\n\n' +
      '例如：采购 200 个 M3 螺丝和 300 个 LED灯珠2835，下周五之前要到'
  }
])
const input = ref('')
const listRef = ref<HTMLElement | null>(null)
const sending = ref(false)
let ctrl: AbortController | null = null

function scrollEnd() {
  nextTick(() => {
    const el = listRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function send() {
  const t = input.value.trim()
  if (!t || sending.value) return

  msgs.value.push({ role: 'user', text: t })
  input.value = ''
  const aiId = msgs.value.length // 即将 push 的那条 AI 消息的下标
  msgs.value.push({ role: 'ai', text: '正在理解…', loading: true })
  scrollEnd()

  sending.value = true
  ctrl = new AbortController()
  try {
    const r = await aiPurchaseParse(t, ctrl.signal)
    // ⚠️ 必须**从数组里取回响应式引用**再改 ——
    //    push 进去的是原始对象，直接改原始引用不经过 Proxy 的 set trap，视图不更新。
    //    （这个坑在智能问数那边反复踩过）
    const msg = msgs.value[aiId]
    msg.loading = false
    msg.text = r.reply
    msg.picks = applyResult(r)
  } catch (e: any) {
    const msg = msgs.value[aiId]
    msg.loading = false
    msg.error = true
    // 后端 500 时 detail 里有具体原因（如「模型输出不是合法 JSON」），比「请求失败」有用得多
    msg.text = e?.response?.data?.detail || e?.message || '解析失败，请稍后重试'
  } finally {
    sending.value = false
    ctrl = null
    scrollEnd()
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

// ==================== 右侧：采购申请单 ====================

const materialPickerVisible = ref(false)
const saving = ref(false)

/** AI 填进来的行高亮多久（毫秒）——够看清是哪几行，又不至于一直在闪 */
const FLASH_MS = 1800

const form = reactive<any>({ applicant: '', remark: '', items: [] })

onMounted(() => {
  // 申请人默认带出当前登录人（登录时写进 localStorage 的）
  form.applicant = localStorage.getItem('realName') || localStorage.getItem('username') || ''
})

/**
 * 高亮一行。
 * ⚠️ 传进来的必须是**从 `form.items` 取回的响应式引用**，不能是 push 进去的原始对象。
 */
function flash(row: any) {
  row._flash = true
  setTimeout(() => {
    row._flash = false
  }, FLASH_MS)
}

/** 把一条候选写进行对象 —— 手工选和候选点选共用这一个动作 */
function fillRow(row: any, c: PurchaseCandidate) {
  row.materialId = c.id
  row.materialName = `${c.code} ${c.name}`
  row.spec = c.spec
  row.unit = c.unit
}

/**
 * 把后端结果落到表单上，并返回「需要用户确认的候选组」。
 *
 * `status='none'` 的项**不建行** —— 建一条空行比不建更糟：
 * 用户会以为系统认识这个物料，直接保存出一张错单。
 */
function applyResult(r: PurchaseParseResult): PickGroup[] {
  // 整单备注：用户自己填过就不覆盖
  if (r.header?.remark && !form.remark) form.remark = r.header.remark

  const picks: PickGroup[] = []
  const newRows: any[] = []

  for (const it of r.items) {
    if (it.status === 'none' || !it.best) continue
    newRows.push({
      materialId: it.best.id,
      materialName: `${it.best.code} ${it.best.name}`,
      spec: it.best.spec,
      unit: it.best.unit,
      quantity: it.quantity,
      requiredDate: it.requiredDate || undefined,
      remark: it.remark || undefined,
      // 下面三个是**前端自己用的**，保存时不会提交（`save()` 里只挑后端 DTO 要的字段）
      _keyword: it.keyword,
      _match: it.status
    })
    if (it.status === 'ambiguous') {
      picks.push({ keyword: it.keyword, candidates: it.candidates })
    }
  }

  const start = form.items.length
  form.items.push(...newRows)

  // 🔴 必须**从数组里取回响应式引用**再改 `_flash`（`newRows` 里是原始对象）
  for (let i = start; i < form.items.length; i++) {
    flash(form.items[i])
  }
  return picks
}

/** 用户在候选里点了一个 —— 替换对应那行 */
function chooseCandidate(g: PickGroup, c: PurchaseCandidate) {
  const row = form.items.find((r: any) => r._keyword === g.keyword)
  if (!row) return
  fillRow(row, c)
  row._match = 'matched'
  flash(row)
  ElMessage.success(`已改为「${c.name}」`)
}

function onMaterialSelect(m: any) {
  form.items.push({
    materialId: m.id,
    materialName: `${m.code} ${m.name}`,
    spec: m.spec,
    unit: m.unit,
    quantity: 1,
    requiredDate: undefined
  })
  // 取回响应式引用再高亮
  flash(form.items[form.items.length - 1])
}

async function save() {
  if (!form.items.length) {
    ElMessage.warning('请先添加申请明细')
    return
  }
  // AI 可能没解析出数量 → 那种行不能直接提交（后端 BigDecimal 收到 0 会报错且难懂）
  const noQty = form.items.find((i: any) => !i.quantity || Number(i.quantity) <= 0)
  if (noQty) {
    ElMessage.warning(`「${noQty.materialName}」还没填需求数量`)
    return
  }
  saving.value = true
  try {
    // ⚠️ 只提交后端 DTO 要的字段（`_keyword` / `_match` / `_flash` 这类前端标记不传）
    await purchaseRequisitionCreate({
      applicant: form.applicant,
      remark: form.remark,
      items: form.items.map((i: any) => ({
        materialId: i.materialId,
        spec: i.spec,
        unit: i.unit,
        quantity: Number(i.quantity),
        requiredDate: i.requiredDate,
        remark: i.remark
      }))
    })
    ElMessage.success('采购申请单已保存（状态：待提交）')
    form.items = []
    form.remark = ''
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
      <el-icon class="ord__head-icon"><ShoppingCart /></el-icon>
      <span class="ord__head-title">AI 采购助手 · 智能下单</span>
      <span class="ord__head-hint">说清楚要买什么，自动填单</span>
    </header>

    <div class="ord__body">
      <!-- ===== 左：对话 ===== -->
      <section class="chat">
        <div class="chat__title">
          <el-icon><ChatDotRound /></el-icon>
          AI 采购助手
        </div>
        <div ref="listRef" class="chat__list">
          <div v-for="(m, i) in msgs" :key="i" class="chat__row" :class="m.role">
            <div class="chat__bubble" :class="{ 'chat__bubble--err': m.error }">
              <!-- 等待后端时给个明确的「在做事情」，不要留空白让人以为卡死了 -->
              <span v-if="m.loading" class="dots">正在理解<em>.</em><em>.</em><em>.</em></span>
              <template v-else>{{ m.text }}</template>

              <!-- 多个候选时：点一下就填进右边，不用去开「选择物料」弹窗 -->
              <div v-if="m.picks?.length" class="picks">
                <div v-for="g in m.picks" :key="g.keyword" class="pick">
                  <div class="pick__label">「{{ g.keyword }}」是哪个？点一下替换</div>
                  <div class="pick__opts">
                    <button
                      v-for="c in g.candidates"
                      :key="c.id"
                      class="pick__opt"
                      :title="`${c.code} ${c.name}`"
                      @click="chooseCandidate(g, c)"
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
          <textarea
            v-model="input"
            rows="2"
            placeholder="例：采购 200 个 M3 螺丝和 300 个 LED灯珠2835，下周五之前要到"
            :disabled="sending"
            @keydown="onKeydown"
          />
          <el-button v-if="sending" @click="stop">停止</el-button>
          <el-button v-else type="primary" :disabled="!input.trim()" @click="send">发送</el-button>
        </div>
      </section>

      <!-- ===== 右：单据表单 ===== -->
      <section class="doc">
        <div class="doc__title">
          新增采购申请单
          <span class="doc__title-hint">与 ERP「采购申请单」同一套字段</span>
        </div>

        <el-form label-width="76px" class="doc__form">
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="申请人">
                <el-input v-model="form.applicant" placeholder="申请人" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="备注">
                <el-input v-model="form.remark" placeholder="选填" />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>

        <div class="doc__bar">
          <span class="doc__bar-title">申请明细</span>
          <el-button size="small" @click="materialPickerVisible = true">添加物料</el-button>
        </div>

        <el-table
          :data="form.items"
          border
          size="small"
          class="doc__table"
          empty-text="还没有明细 —— 左侧说一句话，或点「添加物料」"
        >
          <el-table-column label="物料" min-width="210">
            <template #default="{ row }">
              <!-- 新填的行闪一下底色，让人看清 AI 动了哪几行 -->
              <div class="mat" :class="{ 'mat--flash': row._flash }">
                <span class="mat__name">{{ row.materialName }}</span>
                <!-- ⚠️ 多候选时**故意标记出来**：AI 已经替它选了，但没把握，要人看一眼 -->
                <el-tag
                  v-if="row._match === 'ambiguous'"
                  size="small"
                  type="warning"
                  effect="plain"
                >
                  待确认
                </el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="规格" width="110" />
          <el-table-column label="需求数量" width="120">
            <template #default="{ row }">
              <el-input v-model="row.quantity" type="number" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="需求日期" width="160">
            <template #default="{ row }">
              <el-date-picker
                v-model="row.requiredDate"
                type="date"
                value-format="YYYY-MM-DD"
                size="small"
                style="width: 100%"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="70">
            <template #default="{ $index }">
              <el-button link type="danger" @click="form.items.splice($index, 1)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="doc__foot">
          <!-- 🔴 AI 只填表、不提交：这里永远是**人来点**保存 -->
          <el-button type="primary" :loading="saving" @click="save">保存申请单</el-button>
        </div>
      </section>
    </div>

    <MaterialPicker v-model="materialPickerVisible" @select="onMaterialSelect" />
  </div>
</template>

<style scoped>
.ord {
  display: flex;
  flex-direction: column;
  /* 布局壳已经给了 padding，这里减去顶部条和间隙，让两栏各自内部滚动 */
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
  width: 340px;
  flex: none;
  display: flex;
  flex-direction: column;
  min-height: 0;
  background: #fff;
  border: 1px solid #e6e4de;
  border-radius: 12px;
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
  max-width: 88%;
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
.chat__foot {
  flex: none;
  display: flex;
  gap: 8px;
  align-items: flex-end;
  padding: 10px 12px;
  border-top: 1px solid #f0efe9;
}
.chat__foot textarea {
  flex: 1;
  resize: none;
  padding: 8px 10px;
  border: 1px solid #d3d1c7;
  border-radius: 8px;
  font-size: 12.5px;
  font-family: inherit;
  line-height: 1.6;
  outline: none;
}
.chat__foot textarea:focus {
  border-color: #d32f2f;
}

/* 等待时的三个点 */
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
  transition: background 0.15s ease, border-color 0.15s ease;
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

/* 物料单元格 + 「刚被填进来」的高亮 */
.mat {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: -4px -6px;
  padding: 4px 6px;
  border-radius: 4px;
  transition: background 0.4s ease;
}
.mat__name {
  flex: 1;
  min-width: 0;
}
.mat--flash {
  background: #fff3cd;
  box-shadow: inset 2px 0 0 #e6a700;
  transition: none;
}
</style>
