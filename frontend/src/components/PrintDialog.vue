<template>
  <el-dialog
    :model-value="modelValue"
    :title="title + '打印预览'"
    width="880px"
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div ref="areaRef" class="print-area">
      <div class="print-title">{{ title }}</div>
      <div class="print-head">
        <div v-for="(row, i) in headRows" :key="i" class="print-head-row">
          <span v-for="(cell, j) in row" :key="j">{{ cell.label }}：{{ cell.value ?? '-' }}</span>
        </div>
      </div>
      <table class="print-table">
        <thead>
          <tr>
            <th v-for="(col, i) in columns" :key="i" :style="col.width ? 'width:' + col.width : ''">{{ col.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(r, i) in rows" :key="i">
            <td v-for="(col, j) in columns" :key="j" :align="col.align || 'left'">
              {{ col.formatter ? col.formatter(r, i) : (r[col.prop] ?? '') }}
            </td>
          </tr>
          <tr v-if="!rows.length">
            <td :colspan="columns.length" align="center">无明细</td>
          </tr>
        </tbody>
        <tfoot v-if="totalLabel">
          <tr>
            <td :colspan="columns.length - 1" align="right"><b>{{ totalLabel }}</b></td>
            <td align="right"><b>{{ totalValue }}</b></td>
          </tr>
        </tfoot>
      </table>
      <div v-if="showUpper" class="print-upper">大写：{{ upperAmount(totalAmount) }}</div>
      <div class="print-remark">备注：{{ remark || '无' }}</div>
      <div class="print-sign">
        <span v-for="(s, i) in signLabels" :key="i">{{ s }}：____________</span>
      </div>
    </div>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
      <el-button type="primary" @click="doPrint">打印</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title: string
  headRows?: any[][]
  columns: any[]
  rows?: any[]
  totalLabel?: string
  totalValue?: any
  remark?: string
  showUpper?: boolean
  totalAmount?: number
  signLabels?: string[]
}>(), {
  headRows: () => [],
  rows: () => [],
  totalLabel: '',
  totalValue: '',
  remark: '',
  showUpper: false,
  totalAmount: 0,
  signLabels: () => ['制单人', '审核人']
})

defineEmits(['update:modelValue'])

const areaRef = ref<HTMLElement>()

function upperAmount(money: any) {
  const n = Number(money)
  if (!n || isNaN(n)) return '零元整'
  const fraction = ['角', '分']
  const digit = ['零', '壹', '贰', '叁', '肆', '伍', '陆', '柒', '捌', '玖']
  const unit = [['元', '万', '亿'], ['', '拾', '佰', '仟']]
  const head = n < 0 ? '负' : ''
  let num = Math.abs(n)
  let s = ''
  for (let i = 0; i < fraction.length; i++) {
    s += (digit[Math.floor(num * 10 * Math.pow(10, i)) % 10] + fraction[i]).replace(/零./, '')
  }
  s = s || '整'
  num = Math.floor(num)
  for (let i = 0; i < unit[0].length && num > 0; i++) {
    let p = ''
    for (let j = 0; j < unit[1].length && num > 0; j++) {
      p = digit[num % 10] + unit[1][j] + p
      num = Math.floor(num / 10)
    }
    s = p.replace(/(零.)*零$/, '').replace(/^$/, '零') + unit[0][i] + s
  }
  return head + s.replace(/(零.)*零元/, '元').replace(/(零.)+/g, '零').replace(/^整$/, '零元整')
}

function doPrint() {
  const el = areaRef.value
  if (!el) return
  const win = window.open('', '_blank', 'width=900,height=700')
  if (!win) {
    ElMessage.warning('请允许浏览器弹出窗口后再打印')
    return
  }
  win.document.write(`<!DOCTYPE html><html><head><meta charset="utf-8"><title>${props.title}</title>
    <style>
      body { font-family: "Microsoft YaHei", SimSun, sans-serif; padding: 24px; color: #000; font-size: 13px; }
      .print-title { text-align: center; font-size: 22px; font-weight: bold; letter-spacing: 8px; margin-bottom: 16px; }
      .print-head { margin-bottom: 10px; }
      .print-head-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
      .print-table { width: 100%; border-collapse: collapse; margin-top: 8px; }
      .print-table th, .print-table td { border: 1px solid #000; padding: 5px 8px; font-size: 12px; }
      .print-table th { background: #f2f2f2; text-align: center; }
      .print-upper { margin-top: 8px; }
      .print-remark { margin-top: 8px; }
      .print-sign { display: flex; justify-content: space-between; margin-top: 48px; }
    </style></head><body>${el.innerHTML}</body></html>`)
  win.document.close()
  win.focus()
  setTimeout(() => win.print(), 300)
}
</script>

<style>
.print-area { color: #000; font-size: 13px; }
.print-title { text-align: center; font-size: 22px; font-weight: bold; letter-spacing: 8px; margin-bottom: 16px; }
.print-head { margin-bottom: 10px; }
.print-head-row { display: flex; justify-content: space-between; margin-bottom: 6px; }
.print-table { width: 100%; border-collapse: collapse; margin-top: 8px; }
.print-table th, .print-table td { border: 1px solid #000; padding: 5px 8px; font-size: 12px; }
.print-table th { background: #f2f2f2; text-align: center; }
.print-upper { margin-top: 8px; }
.print-remark { margin-top: 8px; }
.print-sign { display: flex; justify-content: space-between; margin-top: 48px; }
</style>
