<template>
  <el-container class="layout" :class="{ 'is-ai-open': aiStore.open }" :style="layoutStyle">
    <el-aside width="92px" class="aside" ref="asideRef">
      <div class="logo">
        <span class="logo-text">Token</span>
      </div>
      <ul class="top-menu">
        <li
          v-for="(m, idx) in topMenus"
          :key="m.key"
          :ref="(el) => setItemRef(el, idx)"
          class="top-menu-item"
          :class="{ active: isTopActive(m) }"
          @mouseenter="onTopEnter(m, idx)"
          @mouseleave="onTopLeave"
          @click="onTopClick(m)"
        >
          <el-icon class="top-icon"><component :is="m.icon" /></el-icon>
          <span class="top-title">{{ m.title }}</span>
        </li>
      </ul>
    </el-aside>

    <!-- 多列浮层（hover 触发） -->
    <template v-if="flyout.visible && flyout.menu">
      <div
        ref="flyoutRef"
        class="flyout"
        :style="{ top: flyout.top + 'px', left: flyout.left + 'px' }"
        @mouseenter="cancelClose"
        @mouseleave="onTopLeave"
      >
        <div class="flyout-columns">
          <div
            v-for="(col, ci) in flyout.menu.groups"
            :key="ci"
            class="flyout-col"
          >
            <div class="flyout-col-title">{{ col.groupTitle }}</div>
            <ul class="flyout-col-items">
              <li
                v-for="it in col.items"
                :key="it.path"
                class="flyout-item"
                :class="{ active: route.path === it.path }"
                @click="goTo(it.path)"
              >
                {{ it.title }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </template>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- 全局功能搜索：把整棵菜单树摊平，直接搜二级功能，不用先猜分组 -->
          <div ref="searchBox" class="hsearch">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索功能，如「销售订单」"
              :prefix-icon="Search"
              clearable
              @focus="openSearch"
              @keydown.esc="closeSearch"
              @keydown.enter.prevent="goSearchFirst"
              @keydown.down.prevent="moveSearch(1)"
              @keydown.up.prevent="moveSearch(-1)"
            />
            <div v-if="searchVisible" class="hsearch__panel">
              <div
                v-for="(it, i) in searchResults"
                :key="it.path"
                class="hsearch__item"
                :class="{ on: i === searchActive }"
                @mousedown.prevent="goSearch(it.path)"
                @mouseenter="searchActive = i"
              >
                <span class="hsearch__title">{{ it.title }}</span>
                <span class="hsearch__top">{{ it.top }}</span>
              </div>
              <div v-if="!searchResults.length" class="hsearch__empty">没找到相关功能</div>
            </div>
          </div>

          <!-- TK Ai 助手入口（和右下角悬浮球打开同一个抽屉） -->
          <button class="tkai-entry" title="TK Ai 助手" @click="aiStore.openAssistant()">
            <TkAiIcon variant="brand" :size="24" />
          </button>
          <span class="username">{{ userStore.realName || userStore.username }}</span>
          <el-dropdown>
            <el-avatar :size="32" style="background: #d32f2f">{{ avatarText }}</el-avatar>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <TagsView />
      <el-main class="main">
        <router-view v-slot="{ Component, route: currentRoute }">
          <keep-alive :include="tabsStore.cachedNames">
            <component :is="Component" :key="currentRoute.path" />
          </keep-alive>
        </router-view>
      </el-main>
    </el-container>
  </el-container>

  <!-- TK Ai 助手：顶栏按钮和右下角悬浮球共用这一个抽屉实例 -->
  <TkAiFab @open="aiStore.openAssistant()" />
  <TkAiDrawer v-model="aiStore.open" />
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import { useAiStore } from '@/stores/ai'
import TagsView from './TagsView.vue'
import TkAiIcon from '@/components/TkAiIcon.vue'
import TkAiFab from '@/components/TkAiFab.vue'
import TkAiDrawer from '@/components/TkAiDrawer.vue'
import { topMenus, type TopMenu } from './menuConfig'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const aiStore = useAiStore()

// ⚠️ 抽屉开关原本是这里的局部 ref；现在挪到 `stores/ai.ts` ——
//    「AI 工业大脑」页的数字员工卡片也要能打开它，兄弟组件只能靠共享状态。

const activeMenu = computed(() => route.path)

/**
 * AI 抽屉打开时，整个布局**向右收**出抽屉宽度。
 *
 * 🔴 之前抽屉是盖在页面上的（`modal:false` 只去掉遮罩，不改变层叠）——
 *    而整页单据表单的「保存 / 提交」就在右上角，正好被抽屉压住点不到。
 *    现在改成让位：主区域变窄，抽屉占右侧，两边都能操作。
 */
const layoutStyle = computed(() => ({
  paddingRight: aiStore.open ? aiStore.width + 'px' : '0px'
}))
const currentTitle = computed(() => route.meta.title as string)
const avatarText = computed(() => (userStore.realName || userStore.username || 'A').charAt(0))

// 一级菜单 ref 数组（用于浮层定位）
const itemRefs = ref<(HTMLElement | null)[]>([])
function setItemRef(el: any, idx: number) {
  itemRefs.value[idx] = (el as HTMLElement) ?? null
}

const asideRef = ref<HTMLElement | null>(null)
const flyoutRef = ref<HTMLElement | null>(null)

const ASIDE_WIDTH = 92

const flyout = reactive({
  visible: false,
  menu: null as TopMenu | null,
  /** 实际渲染用的 top（会被 clampFlyout 修正） */
  top: 0,
  /** 一级菜单的原始位置，clamp 时以它为基准，避免重复修正导致漂移 */
  topBase: 0,
  left: ASIDE_WIDTH
})

/** 浮层离视口上下沿至少留这么多 */
const FLYOUT_MARGIN = 12

/**
 * 把浮层挤回视口内。
 *
 * 🔴 之前只按 `rect.top` 定位。靠近屏幕底部的一级菜单（「系统管理」是最后一个）
 *    弹出来时会超出视口下沿被裁掉 —— 用户以为「系统管理只有用户管理一个子项」，
 *    实际「角色管理」「部门管理」都被裁在屏幕外，点不到。
 *
 * 修法：浮层渲染完成后量一次真实高度，`top` 取
 * `min(原始位置, 视口高 - 浮层高 - 边距)`，并且不小于边距。
 * ⚠️ 必须 `await nextTick()` 之后量 —— 渲染前 `getBoundingClientRect()` 是 0。
 */
function clampFlyout() {
  const el = flyoutRef.value
  if (!el) return
  const h = el.getBoundingClientRect().height
  const maxTop = window.innerHeight - h - FLYOUT_MARGIN
  flyout.top = Math.max(FLYOUT_MARGIN, Math.min(flyout.topBase, maxTop))
}

let closeTimer: number | null = null
function cancelClose() {
  if (closeTimer) {
    clearTimeout(closeTimer)
    closeTimer = null
  }
}
function onTopLeave() {
  cancelClose()
  closeTimer = window.setTimeout(() => {
    flyout.visible = false
    flyout.menu = null
  }, 180)
}

async function onTopEnter(m: TopMenu, idx: number) {
  // 一级直跳（无子菜单）不弹浮层，保留 click 跳转
  if (!m.groups || m.groups.length === 0) {
    flyout.visible = false
    flyout.menu = null
    cancelClose()
    return
  }
  cancelClose()
  const el = itemRefs.value[idx]
  const rect = el?.getBoundingClientRect()
  flyout.menu = m
  flyout.topBase = rect ? rect.top : 60
  flyout.top = flyout.topBase
  flyout.left = ASIDE_WIDTH
  flyout.visible = true
  // 等浮层渲染出来（此时才有高度）再挤回视口内
  await nextTick()
  clampFlyout()
}

function closeFlyout() {
  cancelClose()
  flyout.visible = false
  flyout.menu = null
}

function onTopClick(m: TopMenu) {
  // 只有无子菜单的一级（path 直跳）才在 click 时跳转
  if (m.path && (!m.groups || m.groups.length === 0)) {
    router.push(m.path)
    closeFlyout()
  }
}

function isTopActive(m: TopMenu) {
  if (m.path && route.path === m.path) return true
  if (m.pathPrefix && m.pathPrefix.some((p) => route.path === p || route.path.startsWith(p + '/'))) {
    return true
  }
  if (m.groups) {
    return m.groups.some((g) => g.items.some((it) => it.path === route.path))
  }
  return false
}

function goTo(path: string) {
  router.push(path)
  closeFlyout()
}

// 点击浮层和左侧菜单之外，关闭浮层（替代全屏遮罩，避免遮挡菜单导致抖动）
function onDocumentClick(e: MouseEvent) {
  if (!flyout.visible) return
  const target = e.target as HTMLElement
  if (asideRef.value && asideRef.value.contains(target)) return
  if (flyoutRef.value && flyoutRef.value.contains(target)) return
  closeFlyout()
}
onMounted(() => {
  document.addEventListener('click', onDocumentClick)
  // 搜索面板用 mousedown（不是 click）：click 要等鼠标抬起，
  // 那时输入框可能已经失焦把面板收掉了，点结果会点空。
  document.addEventListener('mousedown', onSearchOutside)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocumentClick)
  document.removeEventListener('mousedown', onSearchOutside)
})

// ==================== 全局功能搜索 ====================
//
// 84 个菜单项藏在 mega menu 浮层里 —— 想找「销售核销」得先知道它挂在
// 「销售管理 → 销售财务」下面。顶栏这个搜索把整棵菜单树**摊平**，
// 输入关键词直接跳，省掉「先猜分组」这一步。
//
// ⚠️ 菜单数据只读 `menuConfig.ts`，不另建一份 —— 加菜单只改那一处即可。

interface SearchItem {
  /** 归属路径，结果里作灰色小字提示，如「销售管理 · 销售单据」 */
  top: string
  /** 二级菜单标题（搜索主要匹配它） */
  title: string
  path: string
}

/** 最多显示几条 —— 多了就要滚动，反而难挑（也配合「不要滚动条」的整体取向） */
const SEARCH_MAX = 10

const searchAll = computed<SearchItem[]>(() => {
  const out: SearchItem[] = []
  for (const m of topMenus) {
    // 一级本身是页面的（首页 / 驾驶舱 / 架构总览 / AI 工业大脑）
    if (m.path && (!m.groups || m.groups.length === 0)) {
      out.push({ top: '直达', title: m.title, path: m.path })
    }
    for (const g of m.groups || []) {
      for (const it of g.items) {
        out.push({ top: `${m.title} · ${g.groupTitle}`, title: it.title, path: it.path })
      }
    }
  }
  return out
})

const searchVisible = ref(false)
const searchKeyword = ref('')
/** 键盘上下键当前高亮的下标 */
const searchActive = ref(0)
const searchBox = ref<HTMLElement | null>(null)

const searchResults = computed<SearchItem[]>(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  // 没输关键词时把整棵菜单列出来 —— 当索引用，浏览也能发现功能
  const list = kw
    ? searchAll.value.filter(
        (i) => i.title.toLowerCase().includes(kw) || i.top.toLowerCase().includes(kw)
      )
    : searchAll.value
  return list.slice(0, SEARCH_MAX)
})

// 关键词一变，高亮回到第一条（否则可能出现「高亮项已经不在结果里」）
watch(searchKeyword, () => {
  searchActive.value = 0
})

function openSearch() {
  searchVisible.value = true
  searchActive.value = 0
}

function closeSearch() {
  searchVisible.value = false
}

function moveSearch(step: number) {
  const n = searchResults.value.length
  if (!n) return
  searchVisible.value = true
  // 循环移动：到底了回到第一条
  searchActive.value = (searchActive.value + step + n) % n
}

function goSearch(path: string) {
  searchVisible.value = false
  searchKeyword.value = ''
  router.push(path)
}

function goSearchFirst() {
  const it = searchResults.value[searchActive.value] || searchResults.value[0]
  if (it) goSearch(it.path)
}

/** 点搜索框外面就收起面板 */
function onSearchOutside(e: MouseEvent) {
  if (!searchVisible.value) return
  if (searchBox.value && !searchBox.value.contains(e.target as Node)) closeSearch()
}

function handleLogout() {
  userStore.logout()
  tabsStore.reset()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
  /* 抽屉开合时主区域平滑收放，不要「啪」地跳 */
  transition: padding-right 0.2s ease;
}
.aside {
  background-color: #1f2430;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 10;
}
.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.top-menu {
  list-style: none;
  margin: 0;
  padding: 8px 0;
  flex: 1;
  /* 菜单项可能比屏幕高 —— 保留滚轮滚动，但**不显示滚动条**：
     深色底上一条浅色柱子太扎眼，用户明确说不好看。
     ⚠️ 必须留着 overflow-y: auto，否则滚轮就滚不动了。 */
  overflow-y: auto;
  scrollbar-width: none;      /* Firefox */
  -ms-overflow-style: none;   /* 旧 Edge / IE */
}
.top-menu::-webkit-scrollbar {
  width: 0;
  height: 0;
}
.top-menu-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 14px 4px;
  margin: 2px 6px;
  border-radius: 6px;
  color: #bfcbd9;
  cursor: pointer;
  transition: background-color 0.15s, color 0.15s;
  font-size: 12px;
  user-select: none;
}
.top-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.08);
  color: #fff;
}
.top-menu-item.active {
  background-color: #d32f2f;
  color: #fff;
}
.top-icon {
  font-size: 20px;
  margin-bottom: 4px;
}
.top-title {
  line-height: 1.2;
  text-align: center;
}

.flyout {
  position: fixed;
  z-index: 999;
  background: #fff;
  border-radius: 6px;
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
  padding: 16px 20px;
  min-width: 240px;
  max-width: 80vw;
  /* 上下各留 12px 边距（和 clampFlyout 的 FLYOUT_MARGIN 对齐）。
     内容再高就在浮层内部滚动 —— 比被视口裁掉、点不到强 */
  max-height: calc(100vh - 24px);
  overflow: auto;
  border-top: 3px solid #d32f2f;
}
.flyout-columns {
  display: flex;
  gap: 28px;
  align-items: flex-start;
}
.flyout-col {
  min-width: 168px;
}
.flyout-col-title {
  font-size: 12px;
  color: #909399;
  margin-bottom: 10px;
  font-weight: 600;
  padding-bottom: 6px;
  border-bottom: 1px solid #ebeef5;
}
.flyout-col-items {
  list-style: none;
  margin: 0;
  padding: 0;
}
.flyout-item {
  padding: 8px 10px;
  border-radius: 4px;
  cursor: pointer;
  color: #303133;
  font-size: 14px;
  white-space: nowrap;
  transition: background-color 0.15s, color 0.15s;
}
.flyout-item:hover {
  background-color: #fef0f0;
  color: #d32f2f;
}
.flyout-item.active {
  color: #d32f2f;
  font-weight: 600;
  background-color: #fef0f0;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ---------- 全局功能搜索 ---------- */
.hsearch {
  position: relative;
  width: 220px;
}
.hsearch__panel {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  width: 340px;
  padding: 4px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 1000;
}
.hsearch__item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 4px;
  font-size: 13px;
  line-height: 20px;
  cursor: pointer;
}
.hsearch__item.on {
  background: #fef0f0;
}
.hsearch__title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #303133;
}
.hsearch__top {
  flex: none;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: #9ca3af;
}
.hsearch__empty {
  padding: 14px 8px;
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
}
.username {
  font-size: 14px;
  color: #333;
}
.tkai-entry {
  display: inline-flex;
  align-items: center;
  padding: 3px 11px 3px 5px;
  border: 1px solid #e6e4de;
  border-radius: 16px;
  background: #fff;
  cursor: pointer;
  transition: border-color 0.15s ease, background 0.15s ease;
}
.tkai-entry:hover {
  border-color: #d32f2f;
  background: #fdeaea;
}
.main {
  padding: 16px;
  overflow: auto;
}
</style>