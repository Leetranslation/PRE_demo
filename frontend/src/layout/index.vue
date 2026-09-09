<template>
  <el-container class="layout">
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
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import TagsView from './TagsView.vue'
import { topMenus, type TopMenu } from './menuConfig'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()

const activeMenu = computed(() => route.path)
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
  top: 0,
  left: ASIDE_WIDTH
})

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

function onTopEnter(m: TopMenu, idx: number) {
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
  flyout.top = rect ? rect.top : 60
  flyout.left = ASIDE_WIDTH
  flyout.visible = true
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
onMounted(() => document.addEventListener('click', onDocumentClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocumentClick))

function handleLogout() {
  userStore.logout()
  tabsStore.reset()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
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
  overflow-y: auto;
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
  max-height: calc(100vh - 80px);
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
.username {
  font-size: 14px;
  color: #333;
}
.main {
  padding: 16px;
  overflow: auto;
}
</style>