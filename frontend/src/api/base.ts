import request from '@/utils/request'

export interface PageResult<T> {
  page: number
  pageSize: number
  total: number
  records: T[]
}

export interface ListParams {
  page?: number
  pageSize?: number
  keyword?: string
  status?: number
}

// ===== 客户 =====
export function customerList(params: ListParams) {
  return request.get('/base/customer/list', { params }) as Promise<PageResult<any>>
}
export function customerCreate(data: any) {
  return request.post('/base/customer/create', data)
}
export function customerUpdate(data: any) {
  return request.post('/base/customer/update', data)
}
export function customerStatus(id: number, status: number) {
  return request.post('/base/customer/status', null, { params: { id, status } })
}

// ===== 供应商 =====
export function supplierList(params: ListParams) {
  return request.get('/base/supplier/list', { params }) as Promise<PageResult<any>>
}
export function supplierCreate(data: any) {
  return request.post('/base/supplier/create', data)
}
export function supplierUpdate(data: any) {
  return request.post('/base/supplier/update', data)
}
export function supplierStatus(id: number, status: number) {
  return request.post('/base/supplier/status', null, { params: { id, status } })
}

// ===== 物料 =====
export function materialList(params: ListParams) {
  return request.get('/base/material/list', { params }) as Promise<PageResult<any>>
}
export function materialCreate(data: any) {
  return request.post('/base/material/create', data)
}
export function materialUpdate(data: any) {
  return request.post('/base/material/update', data)
}
export function materialStatus(id: number, status: number) {
  return request.post('/base/material/status', null, { params: { id, status } })
}

// ===== 产品 =====
export function productList(params: ListParams) {
  return request.get('/base/product/list', { params }) as Promise<PageResult<any>>
}
export function productCreate(data: any) {
  return request.post('/base/product/create', data)
}
export function productUpdate(data: any) {
  return request.post('/base/product/update', data)
}
export function productStatus(id: number, status: number) {
  return request.post('/base/product/status', null, { params: { id, status } })
}

// ===== 仓库 =====
export function warehouseList(params: ListParams) {
  return request.get('/base/warehouse/list', { params }) as Promise<PageResult<any>>
}
export function warehouseCreate(data: any) {
  return request.post('/base/warehouse/create', data)
}
export function warehouseUpdate(data: any) {
  return request.post('/base/warehouse/update', data)
}
export function warehouseStatus(id: number, status: number) {
  return request.post('/base/warehouse/status', null, { params: { id, status } })
}

// ===== 字典 =====
export function dictList(params: ListParams & { dictType?: string }) {
  return request.get('/base/dict/list', { params }) as Promise<PageResult<any>>
}
export function dictByType(dictType: string) {
  return request.get('/base/dict/type', { params: { dictType } }) as Promise<any[]>
}

// ===== BOM =====
export function bomList(params: ListParams) {
  return request.get('/base/bom/list', { params }) as Promise<PageResult<any>>
}
export function bomCreate(data: any) {
  return request.post('/base/bom/create', data)
}
export function bomUpdate(data: any) {
  return request.post('/base/bom/update', data)
}
export function bomItems(bomId: number) {
  return request.get('/base/bom/items', { params: { bomId } }) as Promise<any[]>
}
