<template>
  <div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="card in cards" :key="card.title">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-title">{{ card.title }}</div>
          <div class="stat-value">{{ card.value }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="section">
      <template #header>
        <span>我的待办</span>
      </template>
      <el-empty description="暂无待办事项" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
const cards = [
  { title: '待审批销售单', value: 0 },
  { title: '待审批采购单', value: 0 },
  { title: '待审批生产工单', value: 0 },
  { title: '低库存预警', value: 0 }
]
</script>

<style scoped>
.stat-card {
  margin-bottom: 16px;
}
.stat-title {
  font-size: 14px;
  color: #666;
}
.stat-value {
  font-size: 28px;
  font-weight: bold;
  color: #d32f2f;
  margin-top: 8px;
}
.section {
  margin-top: 8px;
}
</style>
