<template>
  <el-card shadow="never">
    <el-form :inline="true" :model="query" class="filter-bar">
      <el-form-item label="关键字">
        <el-input v-model="query.keyword" placeholder="名称/编号" clearable style="width: 180px" @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="车间">
        <el-select v-model="query.workshopId" placeholder="全部" clearable filterable style="width: 150px">
          <el-option v-for="w in workshops" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="设备状态">
        <el-select v-model="query.equipStatus" placeholder="全部" clearable style="width: 130px">
          <el-option label="正常" value="normal" />
          <el-option label="维修中" value="repairing" />
          <el-option label="停用" value="stopped" />
          <el-option label="报废" value="scrapped" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增设备</el-button>
    </div>

    <el-table :data="records" v-loading="loading" border stripe row-key="id">
      <el-table-column prop="code" label="设备编号" width="150" />
      <el-table-column prop="name" label="设备名称" min-width="130" />
      <el-table-column prop="model" label="型号" width="110" />
      <el-table-column prop="manufacturer" label="厂商" width="130" />
      <el-table-column label="所属车间" width="120">
        <template #default="{ row }">{{ workshopName(row.workshopId) }}</template>
      </el-table-column>
      <el-table-column prop="purchaseDate" label="购置日期" width="110" />
      <el-table-column prop="warrantyUntil" label="保修期至" width="110" />
      <el-table-column label="设备状态" width="100">
        <template #default="{ row }">
          <el-tag :type="equipTagType(row.equipStatus)" size="small">{{ equipText(row.equipStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastMaintenanceDate" label="上次保养" width="110" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
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

  <el-dialog v-model="dialogVisible" :title="form.id ? '编辑设备' : '新增设备'" width="640px">
    <el-form :model="form" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12"><el-form-item label="设备名称" required><el-input v-model="form.name" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="所属车间">
            <el-select v-model="form.workshopId" filterable placeholder="请选择" style="width: 100%">
              <el-option v-for="w in workshops" :key="w.id" :label="w.name" :value="w.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12"><el-form-item label="类型"><el-input v-model="form.type" placeholder="如：CNC/注塑机" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="型号"><el-input v-model="form.model" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="厂商"><el-input v-model="form.manufacturer" /></el-form-item></el-col>
        <el-col :span="12">
          <el-form-item label="设备状态">
            <el-select v-model="form.equipStatus" style="width: 100%">
              <el-option label="正常" value="normal" />
              <el-option label="维修中" value="repairing" />
              <el-option label="停用" value="stopped" />
              <el-option label="报废" value="scrapped" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12"><el-form-item label="购置日期"><el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="购置价格"><el-input v-model="form.purchasePrice" type="number" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="保修期至"><el-date-picker v-model="form.warrantyUntil" type="date" value-format="YYYY-MM-DD" style="width: 100%" /></el-form-item></el-col>
        <el-col :span="12"><el-form-item label="保养周期(天)"><el-input v-model="form.maintenanceCycle" type="number" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item></el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSave">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
defineOptions({ name: 'EquipmentLedger' })
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { machineList, machineCreate, machineUpdate, workshopList } from '@/api/base'

const loading = ref(false)
const records = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const workshops = ref<any[]>([])
const query = reactive({ page: 1, pageSize: 10, keyword: '', workshopId: undefined as number | undefined, equipStatus: undefined as string | undefined })
const form = reactive<any>({})

function equipText(v: string) { return { normal: '正常', repairing: '维修中', stopped: '停用', scrapped: '报废' }[v] || '正常' }
function equipTagType(v: string) { return { normal: 'success', repairing: 'warning', stopped: 'info', scrapped: 'danger' }[v] || 'success' }

function workshopName(id: number) {
  const w = workshops.value.find((x) => x.id === id)
  return w ? w.name : ''
}

async function loadWorkshops() {
  const res = await workshopList({ page: 1, pageSize: 100, status: 1 })
  workshops.value = res.records
}

async function load() {
  loading.value = true
  try {
    const res = await machineList(query)
    records.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; load() }
function handleReset() { query.keyword = ''; query.workshopId = undefined; query.equipStatus = undefined; query.page = 1; load() }

function openDialog(row?: any) {
  Object.keys(form).forEach((k) => delete form[k])
  if (row) Object.assign(form, row)
  else form.equipStatus = 'normal'
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name) { ElMessage.warning('请填写设备名称'); return }
  form.maintenanceCycle = form.maintenanceCycle ? Number(form.maintenanceCycle) : undefined
  if (form.id) await machineUpdate(form)
  else await machineCreate(form)
  ElMessage.success('保存成功')
  dialogVisible.value = false
  load()
}

loadWorkshops()
load()
</script>

<style scoped>
.filter-bar { margin-bottom: 8px; }
.toolbar { margin-bottom: 12px; }
.pagination { margin-top: 12px; justify-content: flex-end; }
</style>
