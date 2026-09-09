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
export function salesOutboundList(params: any) {
  return request.get('/sales/outbound/list', { params }) as Promise<PageResult<any>>
}

// 销售退货
export function salesReturnCreate(data: any) {
  return request.post('/sales/return/create', data)
}

// 报价单
export function quotationList(params: any) {
  return request.get('/sales/quotation/list', { params }) as Promise<PageResult<any>>
}
export function quotationItems(quotationId: number) {
  return request.get('/sales/quotation/items', { params: { quotationId } }) as Promise<any[]>
}
export function quotationCreate(data: any) {
  return request.post('/sales/quotation/create', data)
}
export function quotationSubmit(id: number) {
  return request.post('/sales/quotation/submit', null, { params: { id } })
}
export function quotationApprove(id: number) {
  return request.post('/sales/quotation/approve', null, { params: { id } })
}
export function quotationUnapprove(id: number) {
  return request.post('/sales/quotation/unapprove', null, { params: { id } })
}
export function quotationConvert(id: number) {
  return request.post('/sales/quotation/convert', null, { params: { id } })
}

// 合同
export function contractList(params: any) {
  return request.get('/sales/contract/list', { params }) as Promise<PageResult<any>>
}
export function contractItems(contractId: number) {
  return request.get('/sales/contract/items', { params: { contractId } }) as Promise<any[]>
}
export function contractCreate(data: any) {
  return request.post('/sales/contract/create', data)
}
export function contractSubmit(id: number) {
  return request.post('/sales/contract/submit', null, { params: { id } })
}
export function contractApprove(id: number) {
  return request.post('/sales/contract/approve', null, { params: { id } })
}
export function contractUnapprove(id: number) {
  return request.post('/sales/contract/unapprove', null, { params: { id } })
}
export function contractConvert(id: number) {
  return request.post('/sales/contract/convert', null, { params: { id } })
}

// 销售发票
export function salesInvoiceList(params: any) {
  return request.get('/sales/invoice/list', { params }) as Promise<PageResult<any>>
}
export function salesInvoiceCreate(data: any) {
  return request.post('/sales/invoice/create', data)
}
export function salesInvoiceSubmit(id: number) {
  return request.post('/sales/invoice/submit', null, { params: { id } })
}
export function salesInvoiceApprove(id: number) {
  return request.post('/sales/invoice/approve', null, { params: { id } })
}
export function salesInvoiceUnapprove(id: number) {
  return request.post('/sales/invoice/unapprove', null, { params: { id } })
}

// 销售核销
export function salesVerificationList(params: any) {
  return request.get('/sales/verification/list', { params }) as Promise<PageResult<any>>
}
export function salesVerificationCreate(data: any) {
  return request.post('/sales/verification/create', data)
}
export function salesVerificationSubmit(id: number) {
  return request.post('/sales/verification/submit', null, { params: { id } })
}
export function salesVerificationApprove(id: number) {
  return request.post('/sales/verification/approve', null, { params: { id } })
}
export function salesVerificationUnapprove(id: number) {
  return request.post('/sales/verification/unapprove', null, { params: { id } })
}

// 销售退货
export function salesReturnList(params: any) {
  return request.get('/sales/return/list', { params }) as Promise<PageResult<any>>
}
export function salesReturnItems(returnId: number) {
  return request.get('/sales/return/items', { params: { returnId } }) as Promise<any[]>
}

// 售后
export function afterSaleList(params: any) {
  return request.get('/sales/afterSale/list', { params }) as Promise<PageResult<any>>
}
export function afterSaleCreate(data: any) {
  return request.post('/sales/afterSale/create', data)
}
export function afterSaleStatus(id: number, status: string) {
  return request.post('/sales/afterSale/status', null, { params: { id, status } })
}

// 备货单
export function salesStockingList(params: any) {
  return request.get('/sales/stocking/list', { params }) as Promise<PageResult<any>>
}
export function salesStockingCreate(data: any) {
  return request.post('/sales/stocking/create', data)
}
export function salesStockingSubmit(id: number) {
  return request.post('/sales/stocking/submit', null, { params: { id } })
}
export function salesStockingApprove(id: number) {
  return request.post('/sales/stocking/approve', null, { params: { id } })
}

// 设计中心
export function designCenterList(params: any) {
  return request.get('/sales/designCenter/list', { params }) as Promise<PageResult<any>>
}
export function designCenterCreate(data: any) {
  return request.post('/sales/designCenter/create', data)
}
export function designCenterStatus(id: number, status: string) {
  return request.post('/sales/designCenter/status', null, { params: { id, status } })
}

// 装箱单
export function packingList(params: any) {
  return request.get('/sales/packing/list', { params }) as Promise<PageResult<any>>
}
export function packingCreate(data: any) {
  return request.post('/sales/packing/create', data)
}
