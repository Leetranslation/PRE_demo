<template>
  <div class="login-page">
    <div class="login-card">
      <h1 class="title">Token 制造</h1>
      <p class="subtitle">制造业 ERP 管理系统</p>
      <el-form :model="form" @submit.prevent="handleLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" native-type="submit">登 录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const form = reactive({ username: '', password: '' })

function handleLogin() {
  // TODO: 接入后端登录接口
  router.push('/dashboard')
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2430 0%, #d32f2f 100%);
}
.login-card {
  width: 380px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.2);
}
.title {
  text-align: center;
  color: #d32f2f;
  font-size: 26px;
  margin-bottom: 4px;
}
.subtitle {
  text-align: center;
  color: #999;
  font-size: 14px;
  margin-bottom: 28px;
}
.login-btn {
  width: 100%;
}
</style>
