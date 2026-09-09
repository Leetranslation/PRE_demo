import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// ===== 库存 =====
export function inventoryList(params: any) {
  return request.get('/inventory/list', { params }) as Promise<PageResult<any>>
}
export function inventoryTx(params: any) {
  return request.get('/inventory/tx', { params }) as Promise<PageResult<any>>
}

// 库存预警
export function inventoryWarning() {
  return request.get('/inventory/warning') as Promise<any[]>
}

// 批号/序列号跟踪
export function batchTrack(params: any) {
  return request.get('/inventory/batchTrack', { params }) as Promise<PageResult<any>>
}
export function serialTrack(params: any) {
  return request.get('/inventory/serialTrack', { params }) as Promise<PageResult<any>>
}

// 锁库/解锁
export function reservationList(params: any) {
  return request.get('/inventory/reservation/list', { params }) as Promise<PageResult<any>>
}
export function reservationLock(data: any) {
  return request.post('/inventory/reservation/lock', data)
}
export function reservationRelease(reservationId: number) {
  return request.post('/inventory/reservation/release', null, { params: { reservationId } })
}

// 其他出入库
export function otherStockList(params: any) {
  return request.get('/inventory/otherStock/list', { params }) as Promise<PageResult<any>>
}
export function otherStockCreate(data: any) {
  return request.post('/inventory/otherStock/create', data)
}

// ===== 库位 =====
export function locationList(params: any) {
  return request.get('/base/location/list', { params }) as Promise<PageResult<any>>
}
export function locationByWarehouse(warehouseId: number) {
  return request.get('/base/location/byWarehouse', { params: { warehouseId } }) as Promise<any[]>
}
export function locationCreate(data: any) {
  return request.post('/base/location/create', data)
}
export function locationUpdate(data: any) {
  return request.post('/base/location/update', data)
}
