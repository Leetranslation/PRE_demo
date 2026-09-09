<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="logo">
        <span class="logo-text">Token 制造</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="menu"
        background-color="#1f2430"
        text-color="#bfcbd9"
        active-text-color="#ffffff"
        router
      >
        <el-menu-item index="/dashboard">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-sub-menu index="/base">
          <template #title>
            <el-icon><Files /></el-icon>
            <span>基础资料</span>
          </template>
          <el-menu-item index="/base/customer">客户档案</el-menu-item>
          <el-menu-item index="/base/supplier">供应商档案</el-menu-item>
          <el-menu-item index="/base/material">物料档案</el-menu-item>
          <el-menu-item index="/base/product">产品档案</el-menu-item>
          <el-menu-item index="/base/warehouse">仓库</el-menu-item>
          <el-menu-item index="/base/location">库位管理</el-menu-item>
          <el-menu-item index="/base/area">库区管理</el-menu-item>
          <el-menu-item index="/base/bom">BOM 管理</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/produceData">
          <template #title>
            <el-icon><Tools /></el-icon>
            <span>生产数据</span>
          </template>
          <el-menu-item index="/produceData/process">工序管理</el-menu-item>
          <el-menu-item index="/produceData/processRoute">工艺路线</el-menu-item>
          <el-menu-item index="/produceData/workshop">车间管理</el-menu-item>
          <el-menu-item index="/produceData/machine">机台设备</el-menu-item>
          <el-menu-item index="/produceData/workCalendar">工作日历</el-menu-item>
          <el-menu-item index="/produceData/unit">单位管理</el-menu-item>
          <el-menu-item index="/produceData/unitConversion">多单位转换率</el-menu-item>
          <el-menu-item index="/produceData/serialRule">序列号规则</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/sales">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>销售管理</span>
          </template>
          <el-menu-item index="/sales/order">销售订单</el-menu-item>
          <el-menu-item index="/sales/quotation">报价单</el-menu-item>
          <el-menu-item index="/sales/contract">合同管理</el-menu-item>
          <el-menu-item index="/sales/invoice">销售发票</el-menu-item>
          <el-menu-item index="/sales/verification">销售核销</el-menu-item>
          <el-menu-item index="/sales/outbound">销售出库单</el-menu-item>
          <el-menu-item index="/sales/return">销售退货单</el-menu-item>
          <el-menu-item index="/sales/afterSale">售后管理</el-menu-item>
          <el-menu-item index="/sales/stocking">销售备货单</el-menu-item>
          <el-menu-item index="/sales/designCenter">设计中心</el-menu-item>
          <el-menu-item index="/sales/packing">装箱单</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/purchase">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>采购管理</span>
          </template>
          <el-menu-item index="/purchase/order">采购订单</el-menu-item>
          <el-menu-item index="/purchase/requisition">采购申请单</el-menu-item>
          <el-menu-item index="/purchase/inquiry">采购询价单</el-menu-item>
          <el-menu-item index="/purchase/invoice">采购发票</el-menu-item>
          <el-menu-item index="/purchase/verification">采购核销</el-menu-item>
          <el-menu-item index="/purchase/return">采购退货单</el-menu-item>
          <el-menu-item index="/purchase/inbound">采购入库单</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/production">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>生产管理</span>
          </template>
          <el-menu-item index="/production/order">生产工单</el-menu-item>
          <el-menu-item index="/production/issue">领料单</el-menu-item>
          <el-menu-item index="/production/return">退料单</el-menu-item>
          <el-menu-item index="/production/inbound">生产入库单</el-menu-item>
          <el-menu-item index="/production/reportWork">报工管理</el-menu-item>
          <el-menu-item index="/production/auditWork">审工管理</el-menu-item>
          <el-menu-item index="/production/progressInquiry">工序进度查询</el-menu-item>
          <el-menu-item index="/production/simpleWorkReport">简易报工</el-menu-item>
          <el-menu-item index="/production/schedule">生产排产计划</el-menu-item>
          <el-menu-item index="/production/mps">主生产计划</el-menu-item>
        </el-sub-menu>
        <el-menu-item index="/mrp/index">
          <el-icon><DataAnalysis /></el-icon>
          <span>MRP 计划</span>
        </el-menu-item>
        <el-sub-menu index="/inventory">
          <template #title>
            <el-icon><Coin /></el-icon>
            <span>库存管理</span>
          </template>
          <el-menu-item index="/inventory/stock">库存查询</el-menu-item>
          <el-menu-item index="/inventory/flow">库存流水</el-menu-item>
          <el-menu-item index="/inventory/warning">库存预警</el-menu-item>
          <el-menu-item index="/inventory/track">批号/序列号跟踪</el-menu-item>
          <el-menu-item index="/inventory/reservation">锁库管理</el-menu-item>
          <el-menu-item index="/inventory/otherStock">其他出入库</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/quality">
          <template #title>
            <el-icon><CircleCheck /></el-icon>
            <span>质量管理</span>
          </template>
          <el-menu-item index="/quality/index">质量检验</el-menu-item>
          <el-menu-item index="/quality/apply">质检申请单</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="/system">
          <template #title>
            <el-icon><User /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/department">部门管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useTabsStore } from '@/stores/tabs'
import TagsView from './TagsView.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const tabsStore = useTabsStore()
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta.title as string)
const avatarText = computed(() => (userStore.realName || userStore.username || 'A').charAt(0))

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
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.menu {
  border-right: none;
  flex: 1;
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
