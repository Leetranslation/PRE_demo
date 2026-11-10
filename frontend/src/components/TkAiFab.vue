<script setup lang="ts">
/**
 * 悬浮球：可拖动 / 不用时贴边半隐藏 / 鼠标移上去整颗滑出。
 * 和顶栏入口打开的是同一个抽屉，所以这里只负责「触发」，不持有任何对话状态。
 *
 * 定位一律用 left + top（而不是 right），因为球可能吸附在左边也可能在右边，
 * 用 left 表达就不用为「吸附侧」写两套样式。
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import TkAiIcon from './TkAiIcon.vue'

// ⚠️ 必须赋值给变量 —— 下面 onClick() 要用它触发 open。
// （原本写成 `defineEmits<{ open: [] }>()` 不赋值，TS 报 `Cannot find name 'emit'`，
//   运行期点球直接 ReferenceError，抽屉打不开。）
const emit = defineEmits<{ open: [] }>()

/** 球直径 —— 必须和模板里传给 TkAiIcon 的 size 一致 */
const SIZE = 56
/** 完全滑出时，球边距屏幕边缘的间距 */
const GAP = 26
/** 贴边收起时露在外面的宽度 */
const PEEK = 24
/** 按下后位移超过这个距离才算「拖动」，否则当点击 */
const DRAG_THRESHOLD = 4
/** 鼠标移开后延迟多久收起，避免手抖一下就弹回去 */
const COLLAPSE_DELAY = 320

const vw = ref(window.innerWidth)
const vh = ref(window.innerHeight)

const side = ref<'left' | 'right'>('right')
const top = ref(0)
const dragging = ref(false)
const hovering = ref(false)

/** 拖动过程中的实时左坐标 */
const dragLeft = ref(0)

const expanded = computed(() => dragging.value || hovering.value)

/** 球当前应该在的水平位置 */
const currentLeft = computed(() => {
  if (dragging.value) return dragLeft.value
  if (side.value === 'right') {
    return expanded.value ? vw.value - SIZE - GAP : vw.value - PEEK
  }
  return expanded.value ? GAP : PEEK - SIZE
})

function clamp(v: number, lo: number, hi: number) {
  return Math.min(Math.max(v, lo), hi)
}

// ---------------- 悬停展开 ----------------
let collapseTimer: number | null = null

function onEnter() {
  if (collapseTimer) {
    clearTimeout(collapseTimer)
    collapseTimer = null
  }
  hovering.value = true
}

function onLeave() {
  if (collapseTimer) clearTimeout(collapseTimer)
  collapseTimer = window.setTimeout(() => {
    hovering.value = false
  }, COLLAPSE_DELAY)
}

// ---------------- 拖动 ----------------
const drag = reactive({ x0: 0, y0: 0, left0: 0, top0: 0, moved: false })

function onPointerDown(e: PointerEvent) {
  if (e.button !== 0) return
  const el = e.currentTarget as HTMLElement
  el.setPointerCapture(e.pointerId)
  dragging.value = true
  drag.x0 = e.clientX
  drag.y0 = e.clientY
  drag.left0 = currentLeft.value
  drag.top0 = top.value
  drag.moved = false
  dragLeft.value = drag.left0
}

function onPointerMove(e: PointerEvent) {
  if (!dragging.value) return
  const dx = e.clientX - drag.x0
  const dy = e.clientY - drag.y0
  if (!drag.moved && Math.abs(dx) + Math.abs(dy) > DRAG_THRESHOLD) drag.moved = true
  // 允许略微拖出屏幕（留 40% 在里面），松手会自动吸回去
  dragLeft.value = clamp(drag.left0 + dx, -SIZE * 0.6, vw.value - SIZE * 0.4)
  top.value = clamp(drag.top0 + dy, 8, vh.value - SIZE - 8)
}

function onPointerUp(e: PointerEvent) {
  if (!dragging.value) return
  const el = e.currentTarget as HTMLElement
  if (el.hasPointerCapture(e.pointerId)) el.releasePointerCapture(e.pointerId)
  dragging.value = false
  // 松手后吸附到更近的一侧
  const center = dragLeft.value + SIZE / 2
  side.value = center < vw.value / 2 ? 'left' : 'right'
}

/** drag.moved 要留到 click 里判断完才能重置，所以在 pointerdown 时重置 */
function onClick() {
  if (drag.moved) return
  emit('open')
}

// ---------------- 窗口尺寸变化 ----------------
function onResize() {
  vw.value = window.innerWidth
  vh.value = window.innerHeight
  top.value = clamp(top.value, 8, vh.value - SIZE - 8)
}

onMounted(() => {
  top.value = vh.value - SIZE - 36
  window.addEventListener('resize', onResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  if (collapseTimer) clearTimeout(collapseTimer)
})
</script>

<template>
  <button
    class="tkai-fab"
    :class="{ 'is-dragging': dragging }"
    :style="{ left: currentLeft + 'px', top: top + 'px' }"
    title="TK Ai 助手"
    aria-label="打开 TK Ai 助手"
    @mouseenter="onEnter"
    @mouseleave="onLeave"
    @pointerdown="onPointerDown"
    @pointermove="onPointerMove"
    @pointerup="onPointerUp"
    @pointercancel="onPointerUp"
    @click="onClick"
  >
    <TkAiIcon :size="SIZE" />
  </button>
</template>

<style scoped>
.tkai-fab {
  position: fixed;
  z-index: 2000;
  padding: 0;
  border: none;
  background: none;
  border-radius: 16px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.18);
  cursor: grab;
  line-height: 0;
  /* 拖动靠 pointer 事件自己算，别让浏览器抢去做滚动 */
  touch-action: none;
  transition:
    left 0.3s cubic-bezier(0.22, 0.61, 0.36, 1),
    box-shadow 0.16s ease,
    transform 0.16s ease;
}
.tkai-fab:hover {
  box-shadow: 0 8px 20px rgba(211, 47, 47, 0.32);
  transform: scale(1.05);
}
/* 拖动中：位置必须跟手，不能有过渡 */
.tkai-fab.is-dragging {
  cursor: grabbing;
  transform: scale(1.06);
  box-shadow: 0 10px 24px rgba(211, 47, 47, 0.36);
  transition:
    box-shadow 0.16s ease,
    transform 0.16s ease;
}
</style>
