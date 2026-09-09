<template>
  <el-card shadow="never">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="检验方法" name="method">
        <div class="toolbar">
          <el-button type="primary" @click="openMethod()">新增检验方法</el-button>
        </div>
        <el-table :data="methods" v-loading="methodLoading" border stripe row-key="id">
          <el-table-column prop="code" label="编码" width="160" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openMethod(row)">编辑</el-button>
              <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleMethod(row)">
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pagination"
          v-model:current-page="methodQuery.page"
          v-model:page-size="methodQuery.pageSize"
          :total="methodTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadMethods"
          @current-change="loadMethods"
        />
      </el-tab-pane>

      <el-tab-pane label="检验方案" name="scheme">
        <div class="toolbar">
          <el-button type="primary" @click="openScheme()">新增检验方案</el-button>
        </div>
        <el-table :data="schemes" v-loading="schemeLoading" border stripe row-key="id">
          <el-table-column prop="code" label="编码" width="160" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column label="适用类型" width="90">
            <template #default="{ row }">{{ row.itemType === 'product' ? '产品' : '物料' }}</template>
          </el-table-column>
          <el-table-column prop="standard" label="检验标准" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="80">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openScheme(row)">编辑</el-button>
              <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleScheme(row)">
                {{ row.status === 1 ? '停用' : '启用' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-pagination
          class="pagination"
          v-model:current-page="schemeQuery.page"
          v-model:page-size="schemeQuery.pageSize"
          :total="schemeTotal"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadSchemes"
          @current-change="loadSchemes"
        />
      </el-tab-pane>
    </el-tabs>
  </el-card>

  <el-dialog v-model="methodVisible" :title="methodForm.id ? '编辑检验方法' : '新增检验方法'" width="520px">
    <el-form :model="methodForm" label-width="100px">
      <el-form-item label="名称" required><el-input v-model="methodForm.name" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="methodForm.description" type="textarea" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="methodForm.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="methodVisible = false">取消</el-button>
      <el-button type="primary" @click="saveMethod">保存</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="schemeVisible" :title="schemeForm.id ? '编辑检验方案' : '新增检验方案'" width="560px">
    <el-form :model="schemeForm" label-width="100px">
      <el-form-item label="名称" required><el-input v-model="schemeForm.name" /></el-form-item>
      <el-form-item label="适用类型">
        <el-radio-group v-model="schemeForm.itemType">
          <el-radio-button value="material">物料</el-radio-button>
          <el-radio-button value="product">产品</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="检验方法">
        <el-select v-model="schemeForm.inspectionMethodId" placeholder="请选择" clearable filterable style="width: 100%">
          <el-option v-for="m in methods" :key="m.id" :label="m.name" :value="m.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="检验标准"><el-input v-model="schemeForm.standard" type="textarea" /></el-form-item>
      <el-form-item label="备注"><el-input v-model="schemeForm.remark" type="textarea" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="schemeVisible = false">取消</el-button>
      <el-button type="primary" @click="saveScheme">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'QualityBase' })
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  inspectionMethodList, inspectionMethodCreate, inspectionMethodUpdate, inspectionMethodStatus,
  inspectionSchemeList, inspectionSchemeCreate, inspectionSchemeUpdate, inspectionSchemeStatus
} from '@/api/base'

const activeTab = ref('method')

const methodLoading = ref(false)
const methods = ref<any[]>([])
const methodTotal = ref(0)
const methodVisible = ref(false)
const methodQuery = reactive({ page: 1, pageSize: 10 })
const methodForm = reactive<any>({})

const schemeLoading = ref(false)
const schemes = ref<any[]>([])
const schemeTotal = ref(0)
const schemeVisible = ref(false)
const schemeQuery = reactive({ page: 1, pageSize: 10 })
const schemeForm = reactive<any>({})

async function loadMethods() {
  methodLoading.value = true
  try {
    const res = await inspectionMethodList(methodQuery)
    methods.value = res.records
    methodTotal.value = res.total
  } finally {
    methodLoading.value = false
  }
}

async function loadSchemes() {
  schemeLoading.value = true
  try {
    const res = await inspectionSchemeList(schemeQuery)
    schemes.value = res.records
    schemeTotal.value = res.total
  } finally {
    schemeLoading.value = false
  }
}

function openMethod(row?: any) {
  Object.keys(methodForm).forEach((k) => delete methodForm[k])
  if (row) Object.assign(methodForm, row)
  methodVisible.value = true
}
async function saveMethod() {
  if (!methodForm.name) { ElMessage.warning('请填写名称'); return }
  if (methodForm.id) await inspectionMethodUpdate(methodForm)
  else await inspectionMethodCreate(methodForm)
  ElMessage.success('保存成功')
  methodVisible.value = false
  loadMethods()
}
async function toggleMethod(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定${action}该检验方法吗？`, '提示', { type: 'warning' })
  await inspectionMethodStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  loadMethods()
}

function openScheme(row?: any) {
  Object.keys(schemeForm).forEach((k) => delete schemeForm[k])
  if (row) Object.assign(schemeForm, row)
  else schemeForm.itemType = 'material'
  schemeVisible.value = true
}
async function saveScheme() {
  if (!schemeForm.name) { ElMessage.warning('请填写名称'); return }
  if (schemeForm.id) await inspectionSchemeUpdate(schemeForm)
  else await inspectionSchemeCreate(schemeForm)
  ElMessage.success('保存成功')
  schemeVisible.value = false
  loadSchemes()
}
async function toggleScheme(row: any) {
  const action = row.status === 1 ? '停用' : '启用'
  await ElMessageBox.confirm(`确定${action}该检验方案吗？`, '提示', { type: 'warning' })
  await inspectionSchemeStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(`${action}成功`)
  loadSchemes()
}

loadMethods()
loadSchemes()
</script>

<style scoped>
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
