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
}

export interface DocColumn {
  key: string
  label: string
  width?: string
  align?: 'left' | 'right' | 'center'
  type?: 'text' | 'input' | 'date' | 'money'
  placeholder?: string
}
