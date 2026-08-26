import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// 生产工单
export function productionOrderList(params: any) {
  return request.get('/production/order/list', { params }) as Promise<PageResult<any>>
}
export function productionOrderCreate(data: any) {
  return request.post('/production/order/create', data)
}
export function productionOrderSubmit(id: number) {
  return request.post('/production/order/submit', null, { params: { id } })
}
export function productionOrderApprove(id: number) {
  return request.post('/production/order/approve', null, { params: { id } })
}
export function productionOrderUnapprove(id: number) {
  return request.post('/production/order/unapprove', null, { params: { id } })
}

// 领料
export function materialIssueCreate(data: any) {
  return request.post('/production/issue/create', data)
}
export function materialIssueList(params: any) {
  return request.get('/production/issue/list', { params }) as Promise<PageResult<any>>
}
export function materialIssueItems(issueId: number) {
  return request.get('/production/issue/items', { params: { issueId } }) as Promise<any[]>
}

// 退料
export function materialReturnCreate(data: any) {
  return request.post('/production/return/create', data)
}
export function materialReturnList(params: any) {
  return request.get('/production/return/list', { params }) as Promise<PageResult<any>>
}
export function materialReturnItems(returnId: number) {
  return request.get('/production/return/items', { params: { returnId } }) as Promise<any[]>
}

// 生产入库列表
export function productionInboundList(params: any) {
  return request.get('/production/inbound/list', { params }) as Promise<PageResult<any>>
}

// 生产入库
export function productionInboundCreate(data: any) {
  return request.post('/production/inbound/create', data)
}
