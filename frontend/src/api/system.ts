import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// ===== 用户 =====
export function userList(params: any) {
  return request.get('/system/user/list', { params }) as Promise<PageResult<any>>
}
export function userCreate(data: any) {
  return request.post('/system/user/create', data)
}
export function userUpdate(data: any) {
  return request.post('/system/user/update', data)
}
export function userResetPassword(id: number, newPassword: string) {
  return request.post('/system/user/reset-password', null, { params: { id, newPassword } })
}
export function userStatus(id: number, status: number) {
  return request.post('/system/user/status', null, { params: { id, status } })
}
export function userRoles(userId: number) {
  return request.get('/system/user/roles', { params: { userId } }) as Promise<number[]>
}
export function userAssignRoles(userId: number, roleIds: number[]) {
  return request.post('/system/user/assign-roles', roleIds, { params: { userId } })
}

// ===== 角色 =====
export function roleList() {
  return request.get('/system/role/list') as Promise<any[]>
}
export function roleCreate(data: any) {
  return request.post('/system/role/create', data)
}
export function roleUpdate(data: any) {
  return request.post('/system/role/update', data)
}
export function rolePermissions(roleId: number) {
  return request.get('/system/role/permissions', { params: { roleId } }) as Promise<number[]>
}
export function roleAssign(roleId: number, permissionIds: number[]) {
  return request.post('/system/role/assign', permissionIds, { params: { roleId } })
}
export function permissionList() {
  return request.get('/system/role/permission-list') as Promise<any[]>
}

// ===== 部门 =====
export function departmentList() {
  return request.get('/system/department/list') as Promise<any[]>
}
export function departmentCreate(data: any) {
  return request.post('/system/department/create', data)
}
export function departmentUpdate(data: any) {
  return request.post('/system/department/update', data)
}
