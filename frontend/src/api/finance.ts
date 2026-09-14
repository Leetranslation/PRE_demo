import request from '@/utils/request'
import type { PageResult } from '@/api/base'

// ===== 成本核算 =====
export function costCalculate(orderId: number) {
  return request.post('/cost/calculate', null, { params: { orderId } })
}

// ===== 应收台账 =====
export function receivableList(params: any) {
  return request.get('/finance/receivable/list', { params }) as Promise<PageResult<any>>
}

// ===== 应付台账 =====
export function payableList(params: any) {
  return request.get('/finance/payable/list', { params }) as Promise<PageResult<any>>
}

// ===== 收款单 =====
export function receiptList(params: any) {
  return request.get('/finance/receipt/list', { params }) as Promise<PageResult<any>>
}
export function receiptCreate(data: any) {
  return request.post('/finance/receipt/create', data)
}
export function receiptWriteoff(receiptId: number, items: any[]) {
  return request.post('/finance/receipt/writeoff', items, { params: { receiptId } })
}
export function receiptWriteoffItems(receiptId: number) {
  return request.get('/finance/receipt/writeoffItems', { params: { receiptId } }) as Promise<any[]>
}

// ===== 付款单 =====
export function paymentList(params: any) {
  return request.get('/finance/payment/list', { params }) as Promise<PageResult<any>>
}
export function paymentCreate(data: any) {
  return request.post('/finance/payment/create', data)
}
export function paymentWriteoff(paymentId: number, items: any[]) {
  return request.post('/finance/payment/writeoff', items, { params: { paymentId } })
}
export function paymentWriteoffItems(paymentId: number) {
  return request.get('/finance/payment/writeoffItems', { params: { paymentId } }) as Promise<any[]>
}
