<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增替代料</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column label="原物料" min-width="160">
        <template #default="{ row }">{{ materialName(row.materialId) }}</template>
      </el-table-column>
      <el-table-column label="替代物料" min-width="160">
        <template #default="{ row }">{{ materialName(row.substituteMaterialId) }}</template>
      </el-table-column>
      <el-table-column prop="ratio" label="替代比例" width="110" align="right" />
      <el-table-column prop="priority" label="优先级" width="90" align="center" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" width="170" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
          <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
            {{ row.status === 1 ? '停用' : '启用' }}
          </el-button>
          <el-button link type="danger" @click="doDelete(row)">删除</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑替代料' : '新增替代料'" width="560px">
    <el-form :model="form" label-width="100px">
      <el-form-item label="原物料" required>
        <el-input :model-value="form.materialName" placeholder="点击选择" readonly style="width: 100%">
          <template #append><el-button @click="pickerTarget = 'material'">选择</el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item label="替代物料" required>
        <el-input :model-value="form.substituteName" placeholder="点击选择" readonly style="width: 100%">
          <template #append><el-button @click="pickerTarget = 'substitute'">选择</el-button></template>
        </el-input>
      </el-form-item>
      <el-form-item label="替代比例">
        <el-input v-model="form.ratio" type="number" style="width: 100%" />
        <div class="tip">1 单位原物料 = 比例 单位替代物料</div>
      </el-form-item>
      <el-form-item label="优先级">
        <el-input v-model="form.priority" type="number" style="width: 100%" />
        <div class="tip">数字越小越优先</div>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>

  <MaterialPicker v-model="pickerVisible" @select="onPick" />
</template>

<script setup lang="ts">
defineOptions({ name: 'MaterialSubstitute' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { substituteList, substituteCreate, substituteUpdate, substituteStatus, substituteDelete } from '@/api/base'
import { materialList } from '@/api/base'
import MaterialPicker from '@/components/MaterialPicker.vue'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const pickerVisible = ref(false)
const pickerTarget = ref('')
const materials = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, status: undefined as number | undefined })
const form = reactive<any>({})

function materialName(id: number) {
  const m = materials.value.find((x) => x.id === id)
  return m ? `${m.code} ${m.name}` : `#${id}`
}

async function loadMaterials() {
  const res = await materialList({ page: 1, pageSize: 200, status: 1 })
  materials.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await substituteList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.status = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) {
    Object.assign(form, row)
    form.materialName = materialName(row.materialId)
    form.substituteName = materialName(row.substituteMaterialId)
  } else {
    form.ratio = 1
    form.priority = 0
  }
  dialogVisible.value = true
}

function onPick(item: any) {
  if (pickerTarget.value === 'material') {
    form.materialId = item.id
    form.materialName = `${item.code} ${item.name}`
  } else {
    form.substituteMaterialId = item.id
    form.substituteName = `${item.code} ${item.name}`
  }
}

async function handleSave() {
  if (!form.materialId || !form.substituteMaterialId) { ElMessage.warning('请选择原物料和替代物料'); return }
  const data = {
    id: form.id, materialId: form.materialId, substituteMaterialId: form.substituteMaterialId,
    ratio: form.ratio ? Number(form.ratio) : 1, priority: form.priority ? Number(form.priority) : 0, remark: form.remark
  }
  if (form.id) await substituteUpdate(data)
  else await substituteCreate(data)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

async function toggleStatus(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定${action}该替代料吗？`, '提示', { type: 'warning' })
  await substituteStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  load()
}

async function doDelete(row: any) {
  await ElMessageBox.confirm('确定删除该替代料关系吗？', '提示', { type: 'warning' })
  await substituteDelete(row.id)
  ElMessage.success('删除成功')
  load()
}

loadMaterials()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.tip { font-size: 12px; color: #909399; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
