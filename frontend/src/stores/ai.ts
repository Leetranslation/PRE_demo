import { defineStore } from 'pinia'
import { ref } from 'vue'

/** 抽屉里的两种模式：知识库问答（怎么用）/ 智能问数（是多少） */
export type AiMode = 'kb' | 'bi'

/**
 * AI 助手抽屉的开关状态。
 *
 * ## 为什么抽成 store
 *
 * 抽屉实例挂在 `layout/index.vue` 上（顶栏按钮和右下角悬浮球共用同一个），
 * 但「AI 工业大脑」页里那些**数字员工卡片**也要能把它打开 ——
 * 两个组件是路由兄弟关系，只能用共享状态，不能靠 props 传。
 *
 * ⚠️ 只共享「开 / 关」和「这次该落在哪个模式」，**不共享对话内容** ——
 *    对话状态留在 `TkAiDrawer` 内部。
 *    这样从任何入口打开都是同一个抽屉、同一份上下文，不会出现两个对话。
 */
export const useAiStore = defineStore('ai', () => {
  const open = ref(false)

  /** 抽屉宽度（px）。放 store 里是因为 layout 要按它给主区域让位 */
  const MIN_W = 380
  const MAX_W = 960
  const DEFAULT_W = 520
  const W_KEY = 'tkai-drawer-width'

  function widthMax() {
    return Math.max(MIN_W, Math.min(MAX_W, window.innerWidth - 120))
  }
  function clampW(v: number) {
    return Math.min(Math.max(v, MIN_W), widthMax())
  }
  function readSavedWidth() {
    try {
      const saved = Number(localStorage.getItem(W_KEY))
      if (Number.isFinite(saved) && saved > 0) return clampW(saved)
    } catch {
      /* 无痕模式等，用默认值 */
    }
    return DEFAULT_W
  }

  const width = ref(readSavedWidth())

  function setWidth(v: number) {
    width.value = clampW(v)
    try {
      localStorage.setItem(W_KEY, String(width.value))
    } catch {
      /* 存不进去不影响使用 */
    }
  }

  /**
   * 一次性「这次打开请落在哪个模式」指令。
   *
   * ⚠️ 消费完必须清空（由 `TkAiDrawer` 负责），否则下次从顶栏打开也会被它劫持。
   */
  const pendingMode = ref<AiMode | null>(null)

  /**
   * 打开助手抽屉。
   *
   * @param mode 指定落在哪个模式；**不传就沿用上次的模式**
   *（顶栏按钮 / 悬浮球不传 —— 用户上回在问数，这次多半还在问数）
   */
  function openAssistant(mode?: AiMode) {
    if (mode) pendingMode.value = mode
    open.value = true
  }

  return { open, pendingMode, openAssistant, width, setWidth, clampW, widthMax, MIN_W, MAX_W }
})
