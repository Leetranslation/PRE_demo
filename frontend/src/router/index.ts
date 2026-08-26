import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '首页', icon: 'HomeFilled' }
      },
      // ===== 基础资料 =====
      {
        path: 'base/customer',
        name: 'Customer',
        component: () => import('@/views/base/customer.vue'),
        meta: { title: '客户档案' }
      },
      {
        path: 'base/supplier',
        name: 'Supplier',
        component: () => import('@/views/base/supplier.vue'),
        meta: { title: '供应商档案' }
      },
      {
        path: 'base/material',
        name: 'Material',
        component: () => import('@/views/base/material.vue'),
        meta: { title: '物料档案' }
      },
      {
        path: 'base/product',
        name: 'Product',
        component: () => import('@/views/base/product.vue'),
        meta: { title: '产品档案' }
      },
      {
        path: 'base/warehouse',
        name: 'Warehouse',
        component: () => import('@/views/base/warehouse.vue'),
        meta: { title: '仓库' }
      },
      {
        path: 'base/bom',
        name: 'Bom',
        component: () => import('@/views/base/bom.vue'),
        meta: { title: 'BOM 管理' }
      },
      {
        path: 'base/location',
        name: 'Location',
        component: () => import('@/views/base/location.vue'),
        meta: { title: '库位管理' }
      },
      // ===== 库存 =====
      {
        path: 'inventory/stock',
        name: 'InventoryStock',
        component: () => import('@/views/inventory/stock.vue'),
        meta: { title: '库存查询' }
      },
      {
        path: 'inventory/flow',
        name: 'InventoryFlow',
        component: () => import('@/views/inventory/flow.vue'),
        meta: { title: '库存流水' }
      },
      // ===== MRP =====
      {
        path: 'mrp/index',
        name: 'Mrp',
        component: () => import('@/views/mrp/index.vue'),
        meta: { title: 'MRP 计划' }
      },
      // ===== 销售 =====
      {
        path: 'sales/order',
        name: 'SalesOrder',
        component: () => import('@/views/sales/order.vue'),
        meta: { title: '销售订单' }
      },
      {
        path: 'sales/outbound',
        name: 'SalesOutbound',
        component: () => import('@/views/sales/outbound.vue'),
        meta: { title: '销售出库单' }
      },
      {
        path: 'sales/return',
        name: 'SalesReturn',
        component: () => import('@/views/sales/return.vue'),
        meta: { title: '销售退货单' }
      },
      // ===== 采购 =====
      {
        path: 'purchase/order',
        name: 'PurchaseOrder',
        component: () => import('@/views/purchase/order.vue'),
        meta: { title: '采购订单' }
      },
      {
        path: 'purchase/inbound',
        name: 'PurchaseInbound',
        component: () => import('@/views/purchase/inbound.vue'),
        meta: { title: '采购入库单' }
      },
      // ===== 生产 =====
      {
        path: 'production/order',
        name: 'ProductionOrder',
        component: () => import('@/views/production/order.vue'),
        meta: { title: '生产工单' }
      },
      {
        path: 'production/issue',
        name: 'MaterialIssue',
        component: () => import('@/views/production/issue.vue'),
        meta: { title: '领料单' }
      },
      {
        path: 'production/return',
        name: 'MaterialReturn',
        component: () => import('@/views/production/return.vue'),
        meta: { title: '退料单' }
      },
      {
        path: 'production/inbound',
        name: 'ProductionInbound',
        component: () => import('@/views/production/inbound.vue'),
        meta: { title: '生产入库单' }
      },
      // ===== 质量 =====
      {
        path: 'quality/index',
        name: 'Quality',
        component: () => import('@/views/quality/index.vue'),
        meta: { title: '质量检验' }
      },
      // ===== 系统管理 =====
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/role',
        name: 'SystemRole',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理' }
      },
      {
        path: 'system/department',
        name: 'SystemDepartment',
        component: () => import('@/views/system/department.vue'),
        meta: { title: '部门管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 登录守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/dashboard')
  } else {
    next()
  }
})

router.afterEach((to) => {
  document.title = `${to.meta.title ?? ''} - Token 制造`
})

export default router
