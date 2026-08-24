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
