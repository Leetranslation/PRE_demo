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
export function purchaseInboundList(params: any) {
  return request.get('/purchase/inbound/list', { params }) as Promise<PageResult<any>>
}

// ===== 采购申请 =====
export function purchaseRequisitionList(params: any) {
  return request.get('/purchase/requisition/list', { params }) as Promise<PageResult<any>>
}
export function purchaseRequisitionItems(requisitionId: number) {
  return request.get('/purchase/requisition/items', { params: { requisitionId } }) as Promise<any[]>
}
export function purchaseRequisitionCreate(data: any) {
  return request.post('/purchase/requisition/create', data)
}
export function purchaseRequisitionSubmit(id: number) {
  return request.post('/purchase/requisition/submit', null, { params: { id } })
}
export function purchaseRequisitionApprove(id: number) {
  return request.post('/purchase/requisition/approve', null, { params: { id } })
}
export function purchaseRequisitionUnapprove(id: number) {
  return request.post('/purchase/requisition/unapprove', null, { params: { id } })
}

// ===== 采购询价 =====
export function purchaseInquiryList(params: any) {
  return request.get('/purchase/inquiry/list', { params }) as Promise<PageResult<any>>
}
export function purchaseInquiryItems(inquiryId: number) {
  return request.get('/purchase/inquiry/items', { params: { inquiryId } }) as Promise<any[]>
}
export function purchaseInquiryCreate(data: any) {
  return request.post('/purchase/inquiry/create', data)
}
export function purchaseInquirySubmit(id: number) {
  return request.post('/purchase/inquiry/submit', null, { params: { id } })
}
export function purchaseInquiryApprove(id: number) {
  return request.post('/purchase/inquiry/approve', null, { params: { id } })
}
export function purchaseInquiryUnapprove(id: number) {
  return request.post('/purchase/inquiry/unapprove', null, { params: { id } })
}
// 比价生单：询价单转采购订单（supplierId 省略时自动选总价最低的供应商）
export function purchaseInquiryToOrder(inquiryId: number, supplierId?: number) {
  return request.post('/purchase/inquiry/toPurchaseOrder', null, { params: { inquiryId, supplierId } }) as Promise<any>
}

// ===== 采购发票 =====
export function purchaseInvoiceList(params: any) {
  return request.get('/purchase/invoice/list', { params }) as Promise<PageResult<any>>
}
export function purchaseInvoiceCreate(data: any) {
  return request.post('/purchase/invoice/create', data)
}
export function purchaseInvoiceSubmit(id: number) {
  return request.post('/purchase/invoice/submit', null, { params: { id } })
}
export function purchaseInvoiceApprove(id: number) {
  return request.post('/purchase/invoice/approve', null, { params: { id } })
}
export function purchaseInvoiceUnapprove(id: number) {
  return request.post('/purchase/invoice/unapprove', null, { params: { id } })
}

// ===== 采购核销 =====
export function purchaseVerificationList(params: any) {
  return request.get('/purchase/verification/list', { params }) as Promise<PageResult<any>>
}
export function purchaseVerificationCreate(data: any) {
  return request.post('/purchase/verification/create', data)
}
export function purchaseVerificationSubmit(id: number) {
  return request.post('/purchase/verification/submit', null, { params: { id } })
}
export function purchaseVerificationApprove(id: number) {
  return request.post('/purchase/verification/approve', null, { params: { id } })
}
export function purchaseVerificationUnapprove(id: number) {
  return request.post('/purchase/verification/unapprove', null, { params: { id } })
}

// ===== 采购退货 =====
export function purchaseReturnList(params: any) {
  return request.get('/purchase/return/list', { params }) as Promise<PageResult<any>>
}
export function purchaseReturnItems(returnId: number) {
  return request.get('/purchase/return/items', { params: { returnId } }) as Promise<any[]>
}
export function purchaseReturnCreate(data: any) {
  return request.post('/purchase/return/create', data)
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

// ===== 质检申请单（三段式） =====
export function qcApplyList(params: any) {
  return request.get('/quality/apply/list', { params }) as Promise<PageResult<any>>
}
export function qcApplyCreate(data: any) {
  return request.post('/quality/apply/create', data)
}
export function qcApplyGenerate(applyId: number) {
  return request.post('/quality/apply/generate', null, { params: { applyId } })
}
