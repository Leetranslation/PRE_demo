<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="用户名/姓名" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增用户</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe>
      <el-table-column prop="username" label="用户名" width="150" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="phone" label="手机号" width="140" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openAssignRoles(row)">分配角色</el-button>
          <el-button link type="info" @click="handleResetPwd(row)">重置密码</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      @size-change="load"
      @current-change="load"
    />
  </el-card>

  <!-- 新增/编辑用户 -->
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑用户' : '新增用户'" width="520px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="用户名" required><el-input v-model="form.username" :disabled="!!form.id" /></el-form-item>
      <el-form-item v-if="!form.id" label="密码" required><el-input v-model="form.password" type="password" show-password /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="form.phone" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      <el-form-item label="部门ID"><el-input-number v-model="form.departmentId" :min="1" style="width: 100%" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <!-- 分配角色 -->
  <el-dialog v-model="roleVisible" title="分配角色" width="480px">
    <el-checkbox-group v-model="selectedRoles">
      <el-checkbox v-for="r in allRoles" :key="r.id" :value="r.id">{{ r.name }}</el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="roleVisible = false">取消</el-button>
      <el-button type="primary" @click="handleAssignRoles">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { userList, userCreate, userUpdate, userResetPassword, userStatus, userRoles, userAssignRoles, roleList } from '@/api/system'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const roleVisible = ref(false)
const allRoles = ref<any[]>([])
const selectedRoles = ref<number[]>([])
const currentUserId = ref<number | null>(null)
const query = reactive({ page: 1, pageSize: 10, keyword: '' })
const form = reactive<any>({})

async function load() {
  loading.value = true
  try {
    const res = await userList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.page = 1; load() }

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  dialogVisible.value = true
}
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialogVisible.value = true
}
async function handleSave() {
  if (!form.username) { ElMessage.warning('请填写用户名'); return }
  if (!form.id && !form.password) { ElMessage.warning('请填写密码'); return }
  if (form.id) await userUpdate(form)
  else await userCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}
async function handleResetPwd(row: any) {
  const { value } = await ElMessageBox.prompt('请输入新密码', '重置密码', { inputType: 'password' })
  await userResetPassword(row.id, value)
  ElMessage.success('密码已重置')
}
async function toggleStatus(row: any) {
  await ElMessageBox.confirm(`确定${row.status === 1 ? '停用' : '启用'}该用户吗？`, '提示', { type: 'warning' })
  await userStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  load()
}
async function openAssignRoles(row: any) {
  currentUserId.value = row.id
  allRoles.value = await roleList()
  selectedRoles.value = await userRoles(row.id)
  roleVisible.value = true
}
async function handleAssignRoles() {
  await userAssignRoles(currentUserId.value!, selectedRoles.value)
  ElMessage.success('角色已分配')
  roleVisible.value = false
}

load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
