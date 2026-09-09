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
export function productionOrderStart(id: number) {
  return request.post('/production/order/start', null, { params: { id } })
}
export function productionOrderFinish(id: number) {
  return request.post('/production/order/finish', null, { params: { id } })
}
export function productionOrderVoid(id: number) {
  return request.post('/production/order/void', null, { params: { id } })
}

// 报工
export function reportWorkList(params: any) {
  return request.get('/production/reportWork/list', { params }) as Promise<PageResult<any>>
}
export function reportWorkCreate(data: any) {
  return request.post('/production/reportWork/create', data)
}
export function reportWorkApprove(id: number) {
  return request.post('/production/reportWork/approve', null, { params: { id } })
}
export function reportWorkUnapprove(id: number) {
  return request.post('/production/reportWork/unapprove', null, { params: { id } })
}
export function reportWorkReject(id: number, remark?: string) {
  return request.post('/production/reportWork/reject', null, { params: { id, remark } })
}
export function reportWorkBatchApprove(workOrderId?: number) {
  return request.post('/production/reportWork/batchApprove', null, { params: { workOrderId } })
}
export function reportWorkProcessProgress(workOrderId: number) {
  return request.get('/production/reportWork/processProgress', { params: { workOrderId } }) as Promise<any[]>
}

// 排产计划
export function schedulePlanList(params: any) {
  return request.get('/production/schedule/list', { params }) as Promise<PageResult<any>>
}
export function schedulePlanCreate(data: any) {
  return request.post('/production/schedule/create', data)
}
export function schedulePlanStatus(id: number, status: string) {
  return request.post('/production/schedule/status', null, { params: { id, status } })
}

// 齐套分析
export function completeAnalysis(workOrderId: number) {
  return request.get('/production/completeAnalysis/analysis', { params: { workOrderId } }) as Promise<any[]>
}

// 主生产计划
export function mpsPlanList(params: any) {
  return request.get('/production/mps/list', { params }) as Promise<PageResult<any>>
}
export function mpsPlanCreate(data: any) {
  return request.post('/production/mps/create', data)
}
export function mpsPlanGenerate(id: number) {
  return request.post('/production/mps/generate', null, { params: { id } })
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
