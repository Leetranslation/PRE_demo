/**
 * 整页单据表单的类型定义。
 * ⚠️ 单独放 .ts：`<script setup>` 里不能 `export interface`。
 */

export interface DocField {
  key: string
  label: string
  type?: 'input' | 'select' | 'date' | 'datetime' | 'textarea' | 'readonly' | 'slot' | 'pick'
  required?: boolean
  options?: { label: string; value: any }[]
  /** 占几列，默认 1（整行 5 列） */
  span?: number
  placeholder?: string
  /**
   * 归属的卡片分组标题（如「基本信息」「订单条款」「其他信息」）。
   * 不填就落到默认分组（`basicTitle`）。
   * 渲染时按**首次出现顺序**生成卡片，所以字段顺序决定卡片顺序。
   */
  group?: string
}

export interface DocColumn {
  key: string
  label: string
  width?: string
  align?: 'left' | 'right' | 'center'
  /**
   * 单元格类型。
   * - `picker`：关联产品/物料。由 DocForm 自己记住「在选第几行」，
   *   选完调 `applyPick()` 整体换数组写回。
   *   ⚠️ 不要让页面拿插槽里的 row 直接改属性 —— 跨组件传引用在 el-table
   *   插槽里不可靠，实测改完不重渲染（这就是「选完产品行内还是未选择」的根因）。
   */
  type?: 'text' | 'input' | 'date' | 'money' | 'picker'
  placeholder?: string
  /** type=picker：主文本，返回空串则显示 emptyText */
  display?: (row: any) => string
  /** type=picker：副文本（如规格、单位） */
  sub?: (row: any) => string
  /** type=picker：未选时的灰字提示 */
  emptyText?: string
  /** type=picker：按钮文案 */
  pickText?: (row: any) => string
}
