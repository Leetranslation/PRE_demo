<template>
  <div>
    <!-- 待办卡片 -->
    <el-row :gutter="16">
      <el-col :span="4" v-for="card in todoCards" :key="card.title">
        <el-card class="stat-card" shadow="hover" @click="goTo(card.path)">
          <div class="stat-title">{{ card.title }}</div>
          <div class="stat-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 基础数据量 -->
    <el-card shadow="never" class="section">
      <template #header><span>基础数据</span></template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="item in baseCards" :key="item.title">
          <div class="base-item">
            <span class="base-label">{{ item.title }}</span>
            <span class="base-value">{{ item.value }}</span>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- 低库存预警 -->
    <el-card shadow="never" class="section">
      <template #header><span>低库存预警</span></template>
      <el-table :data="stats.lowStockItems || []" border size="small">
        <el-table-column prop="itemId" label="物料ID" width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">{{ row.itemType === 'material' ? '物料' : '产品' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="可用库存" width="140" align="right" />
        <el-table-column prop="safetyStock" label="安全库存" width="140" align="right" />
        <el-table-column label="状态">
          <template #default>
            <el-tag type="danger">低于安全库存</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!(stats.lowStockItems || []).length" description="暂无低库存预警" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
defineOptions({ name: 'Dashboard' })
import { reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardStats } from '@/api/dashboard'

const router = useRouter()

const stats = reactive<any>({
  pendingSalesApprove: 0, pendingPurchaseApprove: 0, pendingProductionApprove: 0,
  pendingInbound: 0, pendingOutbound: 0,
  customerCount: 0, supplierCount: 0, materialCount: 0, productCount: 0,
  lowStockItems: []
})

const todoCards = computed(() => [
  { title: '待审批销售单', value: stats.pendingSalesApprove, path: '/sales/order' },
  { title: '待审批采购单', value: stats.pendingPurchaseApprove, path: '/purchase/order' },
  { title: '待审批生产工单', value: stats.pendingProductionApprove, path: '/production/order' },
  { title: '待入库采购单', value: stats.pendingInbound, path: '/purchase/order' },
  { title: '待出库销售单', value: stats.pendingOutbound, path: '/sales/order' }
])

function goTo(path: string) {
  router.push(path)
}

const baseCards = computed(() => [
  { title: '客户', value: stats.customerCount },
  { title: '供应商', value: stats.supplierCount },
  { title: '物料', value: stats.materialCount },
  { title: '产品', value: stats.productCount }
])

async function load() {
  const res = await dashboardStats()
  Object.assign(stats, res)
}

onMounted(load)
</script>

<style scoped>
.stat-card { margin-bottom: 16px; cursor: pointer; }
.stat-card:hover { border-color: #d32f2f; }
.stat-title { font-size: 13px; color: #666; }
.stat-value { font-size: 26px; font-weight: bold; color: #d32f2f; margin-top: 8px; }
.section { margin-top: 16px; }
.base-item { display: flex; justify-content: space-between; padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.base-label { color: #666; font-size: 14px; }
.base-value { font-size: 18px; font-weight: bold; color: #333; }
</style>
