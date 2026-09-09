<template>
  <div class="tags-view">
    <div
      v-for="tab in tabsStore.tabs"
      :key="tab.path"
      class="tag-item"
      :class="{ active: tab.path === route.path }"
      @click="navigate(tab)"
    >
      <span class="tag-title">{{ tab.title }}</span>
      <el-icon
        v-if="!tab.affix"
        class="tag-close"
        @click.stop="handleClose(tab)"
      >
        <Close />
      </el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useTabsStore, type TabItem } from '@/stores/tabs'

const route = useRoute()
const router = useRouter()
const tabsStore = useTabsStore()

function navigate(tab: TabItem) {
  if (tab.path !== route.path) {
    router.push(tab.path)
  }
}

function handleClose(tab: TabItem) {
  const closedCurrent = tabsStore.closeTab(tab.path)
  if (!closedCurrent) return
  // 关闭的是当前标签，跳转到最后一个标签
  if (tab.path === route.path) {
    const last = tabsStore.tabs[tabsStore.tabs.length - 1]
    router.push(last ? last.path : '/dashboard')
  }
}
</script>

<style scoped>
.tags-view {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  overflow-x: auto;
  white-space: nowrap;
}
.tag-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  font-size: 13px;
  color: #495060;
  background: #f4f4f5;
  border: 1px solid #e4e7ed;
  border-radius: 3px;
  cursor: pointer;
  user-select: none;
  transition: all 0.2s;
}
.tag-item:hover {
  color: #d32f2f;
}
.tag-item.active {
  color: #fff;
  background: #d32f2f;
  border-color: #d32f2f;
}
.tag-close {
  font-size: 14px;
  border-radius: 50%;
}
.tag-close:hover {
  background: rgba(0, 0, 0, 0.15);
}
.tag-item.active .tag-close:hover {
  background: rgba(255, 255, 255, 0.3);
}
</style>
