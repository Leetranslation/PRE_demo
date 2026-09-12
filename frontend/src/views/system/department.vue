<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增部门</el-button>
      <el-button @click="doExport">导出</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="50" reserve-selection />
      <el-table-column prop="code" label="部门编码" width="150" />
      <el-table-column prop="name" label="部门名称" width="180" />
      <el-table-column prop="parentName" label="上级部门" width="140" />
      <el-table-column prop="sort" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="160" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑部门' : '新增部门'" width="480px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="部门编码" required><el-input v-model="form.code" /></el-form-item>
      <el-form-item label="部门名称" required><el-input v-model="form.name" /></el-form-item>
      <el-form-item label="上级部门ID"><el-input v-model="form.parentId" placeholder="上级部门ID" /></el-form-item>
      <el-form-item label="排序"><el-input v-model="form.sort" placeholder="排序" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'SystemDepartment' })
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { departmentList, departmentCreate, departmentUpdate } from '@/api/system'
import { useExport } from '@/composables/useExport'

const loading = ref(false)
const records = ref<any[]>([])
const dialogVisible = ref(false)
const form = reactive<any>({})

const { selection, onSelectionChange, handleExport } = useExport(
  [
    { prop: 'code', label: '部门编码' },
    { prop: 'name', label: '部门名称' },
    { prop: 'parentId', label: '上级部门ID' },
    { prop: 'sort', label: '排序' },
    { prop: 'status', label: '状态', formatter: (row: any) => (row.status === 1 ? '启用' : '停用') }
  ],
  '部门'
)
function doExport() { handleExport(records.value) }

async function load() {
  loading.value = true
  try {
    records.value = await departmentList()
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.keys(form).forEach((k) => delete form[k])
  form.parentId = 0
  form.sort = 0
  dialogVisible.value = true
}
function openEdit(row: any) {
  Object.keys(form).forEach((k) => delete form[k])
  Object.assign(form, row)
  dialogVisible.value = true
}
async function handleSave() {
  if (!form.code || !form.name) { ElMessage.warning('请填写编码和名称'); return }
  form.parentId = Number(form.parentId) || 0
  form.sort = Number(form.sort) || 0
  if (form.id) await departmentUpdate(form)
  else await departmentCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
</style>
