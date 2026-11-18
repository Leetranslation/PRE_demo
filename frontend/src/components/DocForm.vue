<script setup lang="ts">
/**
 * 整页单据表单（对齐金蝶风格，见参考图）：
 *   顶部操作栏：标题 + 取消 / 自定义按钮 / 保存 / 提交
 *   「基本信息」：红色竖条标题 + 5 列字段网格
 *   「物料列表」：红色竖条标题 + 工具条 + 表格 + 合计行
 *
 * ⚠️ 只负责布局，字段与校验由各单据页提供。
 */
import { computed, ref } from 'vue'
import { ArrowUpBold } from '@element-plus/icons-vue'
import type { DocColumn, DocField } from './docForm'

const props = withDefaults(
  defineProps<{
    title: string
    modelValue: Record<string, any>
    items: any[]
    fields: DocField[]
    columns: DocColumn[]
    basicTitle?: string
    itemsTitle?: string
    /** 明细只能通过业务自己的方式添加（如物料选择器）时，隐藏通用「添加明细」 */
    hideAdd?: boolean
    /** 单据没有提交/审批流时（如采购退货），隐藏「提交」 */
    hideSubmit?: boolean
    /**
     * type: 'pick' 字段的显示文本。键是字段 key，值是给人看的完整描述
     * （如「PO001 深圳XX 铝合金灯体 ￥12,000」），不是裸编号。
     */
    pickLabels?: Record<string, string>
  }>(),
  { basicTitle: '基本信息', itemsTitle: '物料列表', hideAdd: false, hideSubmit: false, pickLabels: () => ({}) }
)

const emit = defineEmits<{
  (e: 'update:items', v: any[]): void
  (e: 'save'): void
  (e: 'submit'): void
  (e: 'cancel'): void
  (e: 'add'): void
  (e: 'pick', key: string): void
}>()

const selectedIdx = ref<number[]>([])
const allSelected = computed(
  () => props.items.length > 0 && selectedIdx.value.length === props.items.length
)
const someSelected = computed(() => selectedIdx.value.length > 0)

function addRow() {
  emit('add')
}

function toggleRow(i: number) {
  const s = new Set(selectedIdx.value)
  if (s.has(i)) s.delete(i)
  else s.add(i)
  selectedIdx.value = [...s].sort((a, b) => a - b)
}

function toggleAll(val: any) {
  selectedIdx.value = val ? props.items.map((_, i) => i) : []
}

function removeRow(i: number) {
  const next = [...props.items]
  next.splice(i, 1)
  emit('update:items', next)
  selectedIdx.value = []
}

function removeSelected() {
  const keep = props.items.filter((_, i) => !selectedIdx.value.includes(i))
  emit('update:items', keep)
  selectedIdx.value = []
}

function money(v: any) {
  return Number(v || 0).toFixed(2)
}

/** 列宽：docColumns 里写的是 `'260'` 这种纯数字串，直接用会被 CSS 忽略，得补 px */
function colWidth(c: DocColumn) {
  if (!c.width) return undefined
  const n = Number(c.width)
  return Number.isFinite(n) ? `${n}px` : c.width
}

// ==================== 关联产品/物料（type: 'picker'） ====================
//
// 🔴 为什么写回要放在组件里：
//    页面拿插槽里的 row 直接改属性，实测**不会重渲染** —— el-table 的 data 是
//    父组件传进来的数组，行对象不是本组件持有的响应式引用，改属性依赖收集不到。
//    所以这里记住「在选第几行」，由页面选完后调 applyPick()，本组件整体换数组
//    （新对象 + 新数组引用）→ 父级 @update:items 重新赋值 → 一定重渲染。

const pickIndex = ref(-1)
const pickKey = ref('')

/** 点第 i 行的「选择」—— 行号来自原生 v-for，可靠 */
function onPickAt(i: number, c: DocColumn) {
  pickIndex.value = i
  pickKey.value = c.key
  emit('pick', c.key)
}

function pickLabel(c: DocColumn, row: any) {
  if (c.pickText) return c.pickText(row)
  return c.display && c.display(row) ? '更换' : '选择'
}

/**
 * 页面在选好产品/物料后调用，把字段写回刚才点的那一行。
 * @param fields 要合并进该行的字段，如 { productId, productCode, productName, spec, unit }
 */
function applyPick(fields: Record<string, any>) {
  const i = pickIndex.value
  pickIndex.value = -1
  pickKey.value = ''
  if (i < 0 || i >= props.items.length) return
  // 换新对象 + 新数组，父级重新赋值后表格一定重渲染
  emit(
    'update:items',
    props.items.map((r: any, idx: number) => (idx === i ? { ...r, ...fields } : r))
  )
}

/** 字段按 `group` 分组成卡片，卡片顺序 = 字段首次出现的顺序 */
const groupList = computed(() => {
  const out: { title: string; fields: DocField[] }[] = []
  for (const f of props.fields) {
    const title = f.group || props.basicTitle
    let g = out.find((x) => x.title === title)
    if (!g) {
      g = { title, fields: [] }
      out.push(g)
    }
    g.fields.push(f)
  }
  return out
})

defineExpose({ applyPick })
</script>

<template>
  <div class="doc-page">
    <!-- 顶部操作栏：独立卡片，滚动时吸顶 -->
    <div class="doc-card doc-card--top">
      <div class="doc-top">
        <div class="doc-top__title">{{ title }}</div>
        <div class="doc-top__actions">
          <el-button size="small" @click="emit('cancel')">取消</el-button>
          <slot name="actions" />
          <el-button size="small" @click="emit('save')">保存</el-button>
          <el-button v-if="!hideSubmit" size="small" type="danger" @click="emit('submit')">提交</el-button>
        </div>
      </div>
    </div>

    <!-- 基本信息：按 group 分成多张卡片，纵向排列 -->
    <section v-for="g in groupList" :key="g.title" class="doc-card">
      <div class="doc-card__head">
        <span class="doc-card__bar" />
        <span class="doc-card__title">{{ g.title }}</span>
        <span class="doc-card__fold"><el-icon><ArrowUpBold /></el-icon></span>
      </div>
      <div class="doc-grid">
        <div
          v-for="f in g.fields"
          :key="f.key"
          class="doc-field"
          :style="{ gridColumn: `span ${f.span || 1}` }"
        >
          <label class="doc-field__label">
            <span v-if="f.required" class="doc-req">*</span>{{ f.label }}
          </label>

          <el-select
            v-if="f.type === 'select'"
            v-model="modelValue[f.key]"
            size="small"
            clearable
            :placeholder="f.placeholder || '请选择'"
          >
            <el-option v-for="o in f.options || []" :key="o.value" :label="o.label" :value="o.value" />
          </el-select>

          <el-date-picker
            v-else-if="f.type === 'date'"
            v-model="modelValue[f.key]"
            type="date"
            value-format="YYYY-MM-DD"
            size="small"
            style="width: 100%"
          />

          <el-date-picker
            v-else-if="f.type === 'datetime'"
            v-model="modelValue[f.key]"
            type="datetime"
            value-format="YYYY-MM-DD HH:mm:ss"
            size="small"
            style="width: 100%"
          />

          <el-input
            v-else-if="f.type === 'textarea'"
            v-model="modelValue[f.key]"
            type="textarea"
            :rows="1"
            size="small"
            :placeholder="f.placeholder || ''"
          />

          <el-input
            v-else-if="f.type === 'readonly'"
            :model-value="modelValue[f.key]"
            size="small"
            readonly
            class="doc-ro"
          />

          <slot v-else-if="f.type === 'slot'" :name="'field-' + f.key" />

          <!-- 关联单据：只读展示 + 「选择」按钮（点开是表格选择器） -->
          <div v-else-if="f.type === 'pick'" class="doc-pick">
            <el-input
              :model-value="pickLabels[f.key] || ''"
              readonly
              size="small"
              :placeholder="f.placeholder || '点右侧选择'"
            />
            <el-button size="small" @click="emit('pick', f.key)">选择</el-button>
          </div>

          <el-input
            v-else
            v-model="modelValue[f.key]"
            size="small"
            :placeholder="f.placeholder || '请输入'"
          />
        </div>
      </div>
    </section>

    <!-- 明细列表：独立卡片（columns 为空则不显示，如生产工单只有表头） -->
    <section v-if="columns.length" class="doc-card">
      <div class="doc-card__head">
        <span class="doc-card__bar" />
        <span class="doc-card__title">{{ itemsTitle }}</span>
        <div class="doc-sec__tools">
          <slot name="tools" />
          <el-button v-if="!hideAdd" size="small" @click="addRow">添加明细</el-button>
          <el-button size="small" :disabled="!selectedIdx.length" @click="removeSelected">删除</el-button>
        </div>
      </div>

      <!--
        🔴 明细表格用原生 table，不用 el-table。
        原因：el-table 传给列插槽的 row 是**副本**（渲染期 Object.keys 有值，
        但 `props.items.indexOf(row)` 恒为 -1，点击时甚至变成空对象），
        `scope.$index` 也恒为 -1，导致「选完产品回填不到行」。
        原生 v-for 能拿到可靠的下标，这个问题从根上没了。
      -->
      <div class="doc-items">
        <table>
          <thead>
            <tr>
              <th class="c-sel">
                <el-checkbox
                  :model-value="allSelected"
                  :indeterminate="someSelected && !allSelected"
                  @change="toggleAll"
                />
              </th>
              <th class="c-idx">序号</th>
              <th
                v-for="c in columns"
                :key="c.key"
                :style="{ width: colWidth(c), textAlign: c.align || 'left' }"
              >
                {{ c.label }}
              </th>
              <th class="c-op">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, i) in items" :key="i">
              <td class="c-sel">
                <el-checkbox :model-value="selectedIdx.includes(i)" @change="toggleRow(i)" />
              </td>
              <td class="c-idx">{{ i + 1 }}</td>
              <td
                v-for="c in columns"
                :key="c.key"
                :style="{ textAlign: c.align || 'left' }"
              >
                <el-input
                  v-if="c.type === 'input'"
                  v-model="row[c.key]"
                  size="small"
                  :placeholder="c.placeholder || ''"
                />
                <el-date-picker
                  v-else-if="c.type === 'date'"
                  v-model="row[c.key]"
                  type="date"
                  value-format="YYYY-MM-DD"
                  size="small"
                  style="width: 100%"
                />
                <span v-else-if="c.type === 'money'">{{ money(row[c.key]) }}</span>
                <!-- 关联产品/物料：显示 + 选择按钮，写回由 DocForm 负责（见 applyPick） -->
                <div v-else-if="c.type === 'picker'" class="doc-pick-cell">
                  <div class="doc-pick-cell__main">
                    <span v-if="c.display && c.display(row)" class="doc-pick-cell__name">
                      {{ c.display(row) }}
                    </span>
                    <span v-else class="doc-pick-cell__empty">{{ c.emptyText || '未选择' }}</span>
                    <span v-if="c.sub && c.sub(row)" class="doc-pick-cell__sub">
                      {{ c.sub(row) }}
                    </span>
                  </div>
                  <el-button link type="primary" size="small" @click="onPickAt(i, c)">
                    {{ pickLabel(c, row) }}
                  </el-button>
                </div>
                <slot v-else :name="'col-' + c.key" :row="row" :index="i">{{ row[c.key] }}</slot>
              </td>
              <td class="c-op">
                <el-button link type="danger" size="small" @click="removeRow(i)">删除</el-button>
              </td>
            </tr>
            <tr v-if="!items.length">
              <td :colspan="columns.length + 3" class="doc-items__empty">
                暂无明细，点右上角「添加明细」
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="doc-sum">
        <span class="doc-sum__label">合计</span>
        <div class="doc-sum__body"><slot name="summary" /></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.doc-page {
  /*
   * 铺满 .main 的整块浅灰底（负 margin 抵消 .main 的 16px 内边距）。
   * 白卡片放在白底上看不出层级，必须有底色差才「醒目」。
   */
  margin: -16px;
  padding: 16px;
  background: #f4f5f7;
  min-height: calc(100vh - 88px);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

/* ---------- 卡片：每个区块一张，靠边框 + 淡阴影和页面底色区分 ---------- */
.doc-card {
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}
/* 顶部操作栏吸顶，滚长表单时保存/提交始终可见 */
.doc-card--top {
  position: sticky;
  top: 0;
  z-index: 5;
}
.doc-card__head {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 46px;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  border-radius: 6px 6px 0 0;
}
.doc-card__bar {
  width: 3px;
  height: 14px;
  background: #d9363e;
  border-radius: 2px;
}
.doc-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.doc-card__fold {
  margin-left: 2px;
  color: #c0c4cc;
  font-size: 12px;
  line-height: 1;
}
.doc-card .doc-grid {
  padding: 14px 16px 16px;
}
.doc-card .doc-items {
  padding: 0 16px;
}
.doc-card .doc-sum {
  padding: 8px 16px 14px;
}
.doc-sec__tools {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ---------- 顶部操作栏 ---------- */
.doc-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 46px;
  padding: 0 16px;
  border-bottom: 1px solid #e5e7eb;
}
.doc-top__title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2329;
}
.doc-top__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* ---------- 区块（旧 .doc-sec 已由 .doc-card 取代，保留折叠图标样式） ---------- */
.doc-sec__toggle {
  width: 22px;
  height: 22px;
  border: 1px solid #dcdfe6;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
  cursor: pointer;
}
.doc-sec__tools {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ---------- 字段网格：5 列 ---------- */
.doc-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 8px 24px;
}
.doc-field__label {
  display: block;
  font-size: 12px;
  color: #606266;
  margin-bottom: 4px;
  line-height: 16px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.doc-req {
  color: #f56c6c;
  margin-right: 2px;
}
.doc-field :deep(.el-input__wrapper),
.doc-field :deep(.el-select__wrapper) {
  border-radius: 2px;
  min-height: 30px;
}
.doc-field :deep(.el-input__inner),
.doc-field :deep(.el-select__placeholder) {
  font-size: 13px;
}
.doc-ro :deep(.el-input__wrapper) {
  background: #f5f7fa;
}

/* ---------- 明细表格（原生 table，样式对齐 Element Plus） ----------
   ⚠️ 类名不能叫 .doc-grid —— 那个名字被上面「基本信息 5 列网格」占用了，
   复用会导致明细表被塞进 1/5 列的格子里（表格被挤成一条）。 */
.doc-items {
  overflow-x: auto;
}
.doc-items table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  table-layout: auto;
}
.doc-items th,
.doc-items td {
  border: 1px solid #ebeef5;
  padding: 4px 8px;
  text-align: left;
  vertical-align: middle;
  white-space: nowrap;
}
.doc-items thead th {
  height: 34px;
  background: #fafafa;
  color: #606266;
  font-weight: 500;
}
.doc-items tbody td {
  height: 40px;
}
.doc-items .c-sel {
  width: 40px;
  text-align: center;
}
.doc-items .c-idx {
  width: 56px;
  text-align: center;
  color: #909399;
}
.doc-items .c-op {
  width: 64px;
  text-align: center;
}
.doc-items__empty {
  text-align: center;
  color: #909399;
  padding: 18px 0;
  border: none;
}

/* ---------- 明细行里的产品/物料选择单元格 ---------- */
.doc-pick-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: nowrap;
}
.doc-pick-cell__main {
  flex: 1 1 auto;
  min-width: 0;
  display: block;
  line-height: 16px;
}
.doc-pick-cell__name {
  display: block;
  color: #1f2329;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-pick-cell__sub {
  display: block;
  color: #909399;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.doc-pick-cell :deep(.el-button) {
  flex: none;
  margin-left: 0;
}
.doc-pick-cell__empty {
  color: #c0c4cc;
}
.doc-pick {
  display: flex;
  gap: 6px;
  align-items: center;
}
.doc-pick .el-input {
  flex: 1;
}
.doc-field :deep(.el-textarea__inner) {
  min-height: 30px !important;
  padding-top: 5px;
  padding-bottom: 5px;
}

/* ---------- 明细表 ---------- */
.doc-table {
  width: 100%;
}
.doc-table :deep(th.el-table__cell) {
  background: #fafafa;
  color: #606266;
  font-weight: 500;
  font-size: 12px;
  padding: 6px 0;
}
.doc-table :deep(td.el-table__cell) {
  padding: 4px 0;
}
.doc-table :deep(.el-input__wrapper) {
  box-shadow: none;
  border-radius: 0;
  padding: 0 4px;
}
.doc-table :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #c6e2ff inset;
}

/* ---------- 合计行 ---------- */
.doc-sum {
  display: flex;
  align-items: center;
  min-height: 34px;
  border: 1px solid #e5e7eb;
  border-top: none;
  background: #fffbe6;
  font-size: 12px;
}
.doc-sum__label {
  width: 96px;
  text-align: center;
  color: #606266;
  border-right: 1px solid #e5e7eb;
  height: 34px;
  line-height: 34px;
}
.doc-sum__body {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 0 12px;
  color: #1f2329;
}
</style>
