<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增角色</el-button>
      <el-button @click="doExport">导出</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="code" label="角色编码" width="150" />
      <el-table-column prop="name" label="角色名称" width="150" />
      <el-table-column label="数据权限" width="120">
        <template #default="{ row }">
          {{ scopeText(row.dataScope) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" @click="openAssign(row)">分配权限</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <!-- 新增/编辑角色 -->
  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑角色' : '新增角色'" width="500px">
    <el-form :model="form" label-width="90px">
      <el-form-item label="角色编码" required><el-input v-model="form.code" :disabled="!!form.id" /></el-form-item>
      <el-form-item label="角色名称" required><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="数据权限">
        <el-select v-model="form.dataScope" style="width: 100%">
          <el-option label="全部" value="all" />
          <el-option label="本部门" value="dept" />
          <el-option label="本人" value="self" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <!-- 分配权限 -->
  <el-dialog v-model="permVisible" title="分配权限" width="520px">
    <el-checkbox-group v-model="selectedPerms">
      <el-checkbox v-for="p in allPerms" :key="p.id" :value="p.id" style="display: block; margin: 4px 0;">
        {{ p.name }}（{{ p.code }}）
      </el-checkbox>
    </el-checkbox-group>
    <template #footer>
      <el-button @click="permVisible = false">取消</el-button>
      <el-button type="primary" @click="handleAssignPerm">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { roleList, roleCreate, roleUpdate, rolePermissions, roleAssign, permissionList } from '@/api/system'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const dialogVisible = ref(false)
const permVisible = ref(false)
const allPerms = ref<any[]>([])
const selectedPerms = ref<number[]>([])
const currentRoleId = ref<number | null>(null)
const form = reactive<any>({})

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'code', label: '角色编码' },
    { prop: 'name', label: '角色名称' },
    { prop: 'dataScope', label: '数据权限', formatter: (row: any) => scopeText(row.dataScope) },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '角色'
)
function doExport() { handleExport(records.value) }

function scopeText(v: string) {
  return { all: '全部', dept: '本部门', self: '本人' }[v] || v
}

async function load() {
  loading.value = true
  try {
    records.value = await roleList()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.dataScope = 'self'
  dialogVisible.value = true
}
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialogVisible.value = true
}
async function handleSave() {
  if (!form.code || !form.name) { ElMessage.warning('请填写编码和名称'); return }
  if (form.id) await roleUpdate(form)
  else await roleCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function openAssign(row: any) {
  currentRoleId.value = row.id
  allPerms.value = await permissionList()
  selectedPerms.value = await rolePermissions(row.id)
  permVisible.value = true
}
async function handleAssignPerm() {
  await roleAssign(currentRoleId.value!, selectedPerms.value)
  ElMessage.success('权限已分配')
  permVisible.value = false
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
