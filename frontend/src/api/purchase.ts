import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// ===== 采购订单 =====
export function purchaseOrderList(params: any) {
  return request.get('/purchase/order/list', { params }) as Promise<PageResult<any>>
}
export function purchaseOrderItems(orderId: number) {
  return request.get('/purchase/order/items', { params: { orderId } }) as Promise<any[]>
}
export function purchaseOrderCreate(data: any) {
  return request.post('/purchase/order/create', data)
}
export function purchaseOrderSubmit(id: number) {
  return request.post('/purchase/order/submit', null, { params: { id } })
}
export function purchaseOrderApprove(id: number) {
  return request.post('/purchase/order/approve', null, { params: { id } })
}
export function purchaseOrderUnapprove(id: number) {
  return request.post('/purchase/order/unapprove', null, { params: { id } })
}

// ===== 采购入库 =====
export function purchaseInboundCreate(data: any) {
  return request.post('/purchase/inbound/create', data)
}

// ===== 质检 =====
export function qualityList(params: any) {
  return request.get('/quality/list', { params }) as Promise<PageResult<any>>
}
export function qualityCreate(data: any) {
  return request.post('/quality/create', data)
}
export function qualityJudge(inspectionId: number, result: string, handleType?: string, reason?: string) {
  return request.post('/quality/judge', null, {
    params: { inspectionId, result, handleType, reason }
  })
}
