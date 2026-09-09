import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { RouteLocationNormalizedLoaded } from 'vue-router'

/** 标签项 */
export interface TabItem {
  /** 路由完整路径，作为唯一标识 */
  path: string
  /** 路由 name，用于 keep-alive include 匹配 */
  name: string
  /** 显示标题 */
  title: string
  /** 是否固定（首页），不可关闭 */
  affix: boolean
}

const MAX_TABS = 15

export const useTabsStore = defineStore('tabs', () => {
  const tabs = ref<TabItem[]>([])

  /** 已打开标签的组件 name 列表，用于 keep-alive 缓存 */
  const cachedNames = computed(() => tabs.value.map((t) => t.name))

  /** 首页固定标签 */
  function initAffix() {
    if (tabs.value.length === 0) {
      tabs.value.push({ path: '/dashboard', name: 'Dashboard', title: '首页', affix: true })
    }
  }

  /** 判断标签栏是否已满（不允许再新增标签） */
  function isFull(): boolean {
    return tabs.value.length >= MAX_TABS
  }

  /** 是否已存在该路径的标签 */
  function hasTab(path: string): boolean {
    return tabs.value.some((t) => t.path === path)
  }

  /** 添加标签；返回是否成功（达到上限且是新标签时失败） */
  function addTab(route: RouteLocationNormalizedLoaded): boolean {
    const existing = tabs.value.find((t) => t.path === route.path)
    if (existing) {
      return true
    }
    // 达到上限，拒绝新增
    if (tabs.value.length >= MAX_TABS) {
      ElMessage.warning(`标签页已达上限（${MAX_TABS} 个），请先关闭其他标签`)
      return false
    }
    // route.name 类型为 string | symbol | undefined，keep-alive 的 include 只认字符串
    const name = typeof route.name === 'string' ? route.name : route.path
    const title = (route.meta.title as string) || name || '未命名'
    tabs.value.push({
      path: route.path,
      name,
      title,
      affix: false
    })
    return true
  }

  /** 关闭标签（固定标签不可关），返回是否关闭了当前标签 */
  function closeTab(path: string): boolean {
    const idx = tabs.value.findIndex((t) => t.path === path)
    if (idx === -1) return false
    if (tabs.value[idx].affix) return false
    tabs.value.splice(idx, 1)
    return true
  }

  /** 关闭全部非固定标签 */
  function closeOthers() {
    tabs.value = tabs.value.filter((t) => t.affix)
  }

  /** 退出登录时清空 */
  function reset() {
    tabs.value = []
  }

  return { tabs, cachedNames, initAffix, addTab, hasTab, isFull, closeTab, closeOthers, reset }
})
