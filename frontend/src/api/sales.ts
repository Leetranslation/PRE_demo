import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// 销售订单
export function salesOrderList(params: any) {
  return request.get('/sales/order/list', { params }) as Promise<PageResult<any>>
}
export function salesOrderItems(orderId: number) {
  return request.get('/sales/order/items', { params: { orderId } }) as Promise<any[]>
}
export function salesOrderCreate(data: any) {
  return request.post('/sales/order/create', data)
}
export function salesOrderSubmit(id: number) {
  return request.post('/sales/order/submit', null, { params: { id } })
}
export function salesOrderApprove(id: number) {
  return request.post('/sales/order/approve', null, { params: { id } })
}
export function salesOrderUnapprove(id: number) {
  return request.post('/sales/order/unapprove', null, { params: { id } })
}

// 销售出库
export function salesOutboundCreate(data: any) {
  return request.post('/sales/outbound/create', data)
}

// 销售退货
export function salesReturnCreate(data: any) {
  return request.post('/sales/return/create', data)
}
