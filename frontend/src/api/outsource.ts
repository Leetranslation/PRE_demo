import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// ===== 外协单 =====
export function outsourceOrderList(params: any) {
  return request.get('/outsource/order/list', { params }) as Promise<PageResult<any>>
}
export function outsourceOrderItems(outsourceId: number) {
  return request.get('/outsource/order/items', { params: { outsourceId } }) as Promise<any[]>
}
export function outsourceOrderCreate(data: any) {
  return request.post('/outsource/order/create', data)
}
export function outsourceOrderUpdate(data: any) {
  return request.post('/outsource/order/update', data)
}
export function outsourceOrderSubmit(id: number) {
  return request.post('/outsource/order/submit', null, { params: { id } })
}
export function outsourceOrderApprove(id: number) {
  return request.post('/outsource/order/approve', null, { params: { id } })
}
export function outsourceOrderUnapprove(id: number) {
  return request.post('/outsource/order/unapprove', null, { params: { id } })
}
export function outsourceOrderStart(id: number) {
  return request.post('/outsource/order/start', null, { params: { id } })
}
export function outsourceOrderFinish(id: number) {
  return request.post('/outsource/order/finish', null, { params: { id } })
}
export function outsourceOrderVoid(id: number) {
  return request.post('/outsource/order/void', null, { params: { id } })
}
