<script setup lang="ts">
/**
 * 整页单据表单（对齐金蝶风格，见参考图）：
 *   顶部操作栏：标题 + 取消 / 自定义按钮 / 保存 / 提交
 *   「基本信息」：红色竖条标题 + 5 列字段网格
 *   「物料列表」：红色竖条标题 + 工具条 + 表格 + 合计行
 *
 * ⚠️ 只负责布局，字段与校验由各单据页提供。
 */
import { ref } from 'vue'
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

const selected = ref<any[]>([])

function addRow() {
  emit('add')
}

function removeRow(i: number) {
  const next = [...props.items]
  next.splice(i, 1)
  emit('update:items', next)
}

function removeSelected() {
  const set = new Set(selected.value)
  emit('update:items', props.items.filter((r) => !set.has(r)))
  selected.value = []
}

function money(v: any) {
  return Number(v || 0).toFixed(2)
}
</script>

<template>
  <div class="doc-page">
    <!-- 顶部操作栏 -->
    <div class="doc-top">
      <div class="doc-top__title">{{ title }}</div>
      <div class="doc-top__actions">
        <el-button size="small" @click="emit('cancel')">取消</el-button>
        <slot name="actions" />
        <el-button size="small" @click="emit('save')">保存</el-button>
        <el-button v-if="!hideSubmit" size="small" type="danger" @click="emit('submit')">提交</el-button>
      </div>
    </div>

    <!-- 基本信息 -->
    <section class="doc-sec">
      <div class="doc-sec__head">
        <span class="doc-sec__title">{{ basicTitle }}</span>
        <span class="doc-sec__toggle"><el-icon><ArrowUpBold /></el-icon></span>
      </div>
      <div class="doc-grid">
        <div
          v-for="f in fields"
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

    <!-- 明细列表（columns 为空则不显示，如生产工单只有表头） -->
    <section v-if="columns.length" class="doc-sec">
      <div class="doc-sec__head">
        <span class="doc-sec__title">{{ itemsTitle }}</span>
        <div class="doc-sec__tools">
          <slot name="tools" />
          <el-button v-if="!hideAdd" size="small" @click="addRow">添加明细</el-button>
          <el-button size="small" :disabled="!selected.length" @click="removeSelected">删除</el-button>
        </div>
      </div>

      <el-table
        :data="items"
        size="small"
        border
        class="doc-table"
        @selection-change="(rows: any[]) => (selected = rows)"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column type="index" label="序号" width="56" align="center" />
        <el-table-column
          v-for="c in columns"
          :key="c.key"
          :label="c.label"
          :width="c.width"
          :min-width="c.width ? undefined : 120"
          :align="c.align || 'left'"
        >
          <template #default="scope">
            <el-input
              v-if="c.type === 'input'"
              v-model="scope.row[c.key]"
              size="small"
              :placeholder="c.placeholder || ''"
            />
            <el-date-picker
              v-else-if="c.type === 'date'"
              v-model="scope.row[c.key]"
              type="date"
              value-format="YYYY-MM-DD"
              size="small"
              style="width: 100%"
            />
            <span v-else-if="c.type === 'money'">{{ money(scope.row[c.key]) }}</span>
            <slot v-else :name="'col-' + c.key" :row="scope.row" :index="scope.$index">{{
              scope.row[c.key]
            }}</slot>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="64" fixed="right" align="center">
          <template #default="{ $index }">
            <el-button link type="danger" size="small" @click="removeRow($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="doc-sum">
        <span class="doc-sum__label">合计</span>
        <div class="doc-sum__body"><slot name="summary" /></div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.doc-page {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  min-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
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

/* ---------- 区块 ---------- */
.doc-sec {
  padding: 0 16px 14px;
}
.doc-sec + .doc-sec {
  border-top: 1px solid #e5e7eb;
  padding-top: 10px;
}
.doc-sec__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 42px;
}
.doc-sec__title {
  position: relative;
  padding-left: 9px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2329;
}
.doc-sec__title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 14px;
  background: #d9363e;
  border-radius: 1px;
}
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
