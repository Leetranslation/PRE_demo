import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// ===== 库存 =====
export function inventoryList(params: any) {
  return request.get('/inventory/list', { params }) as Promise<PageResult<any>>
}
export function inventoryTx(params: any) {
  return request.get('/inventory/tx', { params }) as Promise<PageResult<any>>
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
