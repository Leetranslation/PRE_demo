import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// ===== 设备点检 =====
export function equipmentCheckList(params: any) {
  return request.get('/equipment/check/list', { params }) as Promise<PageResult<any>>
}
export function equipmentCheckCreate(data: any) {
  return request.post('/equipment/check/create', data)
}
export function equipmentCheckUpdate(data: any) {
  return request.post('/equipment/check/update', data)
}
export function equipmentCheckDelete(id: number) {
  return request.post('/equipment/check/delete', null, { params: { id } })
}

// ===== 设备保养 =====
export function equipmentMaintenanceList(params: any) {
  return request.get('/equipment/maintenance/list', { params }) as Promise<PageResult<any>>
}
export function equipmentMaintenanceCreate(data: any) {
  return request.post('/equipment/maintenance/create', data)
}
export function equipmentMaintenanceUpdate(data: any) {
  return request.post('/equipment/maintenance/update', data)
}
export function equipmentMaintenanceComplete(id: number) {
  return request.post('/equipment/maintenance/complete', null, { params: { id } })
}
export function equipmentMaintenanceDelete(id: number) {
  return request.post('/equipment/maintenance/delete', null, { params: { id } })
}

// ===== 设备维修 =====
export function equipmentRepairList(params: any) {
  return request.get('/equipment/repair/list', { params }) as Promise<PageResult<any>>
}
export function equipmentRepairCreate(data: any) {
  return request.post('/equipment/repair/create', data)
}
export function equipmentRepairUpdate(data: any) {
  return request.post('/equipment/repair/update', data)
}
export function equipmentRepairStatus(id: number, status: string) {
  return request.post('/equipment/repair/status', null, { params: { id, status } })
}
export function equipmentRepairDelete(id: number) {
  return request.post('/equipment/repair/delete', null, { params: { id } })
}
