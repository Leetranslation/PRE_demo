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
export function warningGeneratePurchase(itemId: number) {
  return request.post('/inventory/warning/generatePurchase', null, { params: { itemId } })
}
export function warningGenerateWorkOrder(itemId: number) {
  return request.post('/inventory/warning/generateWorkOrder', null, { params: { itemId } })
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
/** 其他出入库明细 */
export function otherStockItems(stockId: number) {
  return request.get('/inventory/otherStock/items', { params: { stockId } }) as Promise<any[]>
}

// ===== 库存盘点 =====
export function stocktakeList(params: any) {
  return request.get('/inventory/stocktake/list', { params }) as Promise<PageResult<any>>
}
export function stocktakeItems(stocktakeId: number) {
  return request.get('/inventory/stocktake/items', { params: { stocktakeId } }) as Promise<any[]>
}
export function stocktakeCreate(warehouseId: number, remark?: string) {
  return request.post('/inventory/stocktake/create', null, { params: { warehouseId, remark } })
}
export function stocktakeCount(data: any[]) {
  return request.post('/inventory/stocktake/count', data)
}
export function stocktakeAdjust(stocktakeId: number) {
  return request.post('/inventory/stocktake/adjust', null, { params: { stocktakeId } })
}

// ===== 库存调拨 =====
export function transferList(params: any) {
  return request.get('/inventory/transfer/list', { params }) as Promise<PageResult<any>>
}
export function transferItems(transferId: number) {
  return request.get('/inventory/transfer/items', { params: { transferId } }) as Promise<any[]>
}
export function transferCreate(data: any) {
  return request.post('/inventory/transfer/create', data)
}

// ===== 组装/拆卸 =====
export function assemblyList(params: any) {
  return request.get('/inventory/assembly/list', { params }) as Promise<PageResult<any>>
}
export function assemblyItems(orderId: number) {
  return request.get('/inventory/assembly/items', { params: { orderId } }) as Promise<any[]>
}
export function assemblyCreate(data: any) {
  return request.post('/inventory/assembly/create', data)
}

// ===== 期初余额 =====
export function beginBalanceList(params: any) {
  return request.get('/inventory/beginBalance/list', { params }) as Promise<PageResult<any>>
}
export function beginBalanceSave(data: any) {
  return request.post('/inventory/beginBalance/save', data)
}
export function beginBalanceDelete(id: number) {
  return request.post('/inventory/beginBalance/delete', null, { params: { id } })
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
