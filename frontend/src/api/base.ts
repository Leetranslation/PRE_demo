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

// ===== 工序 =====
export function processList(params: ListParams) {
  return request.get('/base/process/list', { params }) as Promise<PageResult<any>>
}
export function processCreate(data: any) {
  return request.post('/base/process/create', data)
}
export function processUpdate(data: any) {
  return request.post('/base/process/update', data)
}
export function processStatus(id: number, status: number) {
  return request.post('/base/process/status', null, { params: { id, status } })
}
export function processBatchPrice(ids: number[], price: number) {
  return request.post('/base/process/batchPrice', { ids, price })
}
export function processBatchPriceType(ids: number[], priceType: string) {
  return request.post('/base/process/batchPriceType', { ids, priceTypeValue: priceType })
}

// ===== 工艺路线 =====
export function processRouteList(params: ListParams) {
  return request.get('/base/processRoute/list', { params }) as Promise<PageResult<any>>
}
export function processRouteItems(routeId: number) {
  return request.get('/base/processRoute/items', { params: { routeId } }) as Promise<any[]>
}
export function processRouteCreate(data: any) {
  return request.post('/base/processRoute/create', data)
}
export function processRouteUpdate(data: any) {
  return request.post('/base/processRoute/update', data)
}
export function processRouteStatus(id: number, status: number) {
  return request.post('/base/processRoute/status', null, { params: { id, status } })
}
export function processRouteDelete(id: number) {
  return request.post('/base/processRoute/delete', null, { params: { id } })
}

// ===== 车间 =====
export function workshopList(params: ListParams) {
  return request.get('/base/workshop/list', { params }) as Promise<PageResult<any>>
}
export function workshopCreate(data: any) {
  return request.post('/base/workshop/create', data)
}
export function workshopUpdate(data: any) {
  return request.post('/base/workshop/update', data)
}
export function workshopStatus(id: number, status: number) {
  return request.post('/base/workshop/status', null, { params: { id, status } })
}

// ===== 机台 =====
export function machineList(params: ListParams & { workshopId?: number }) {
  return request.get('/base/machine/list', { params }) as Promise<PageResult<any>>
}
export function machineCreate(data: any) {
  return request.post('/base/machine/create', data)
}
export function machineUpdate(data: any) {
  return request.post('/base/machine/update', data)
}
export function machineStatus(id: number, status: number) {
  return request.post('/base/machine/status', null, { params: { id, status } })
}

// ===== 工作日历 =====
export function workCalendarList(params: { page?: number; pageSize?: number; yearMonth?: string }) {
  return request.get('/base/workCalendar/list', { params }) as Promise<PageResult<any>>
}
export function workCalendarBatchSet(data: any) {
  return request.post('/base/workCalendar/batchSet', data)
}

// ===== 单位 =====
export function unitList(params: ListParams) {
  return request.get('/base/unit/list', { params }) as Promise<PageResult<any>>
}
export function unitCreate(data: any) {
  return request.post('/base/unit/create', data)
}
export function unitUpdate(data: any) {
  return request.post('/base/unit/update', data)
}
export function unitStatus(id: number, status: number) {
  return request.post('/base/unit/status', null, { params: { id, status } })
}

// ===== 单位换算率 =====
export function unitConversionList(params: ListParams) {
  return request.get('/base/unitConversion/list', { params }) as Promise<PageResult<any>>
}
export function unitConversionCreate(data: any) {
  return request.post('/base/unitConversion/create', data)
}
export function unitConversionUpdate(data: any) {
  return request.post('/base/unitConversion/update', data)
}
export function unitConversionStatus(id: number, status: number) {
  return request.post('/base/unitConversion/status', null, { params: { id, status } })
}

// ===== 序列号规则 =====
export function serialRuleList(params: ListParams) {
  return request.get('/base/serialRule/list', { params }) as Promise<PageResult<any>>
}
export function serialRuleCreate(data: any) {
  return request.post('/base/serialRule/create', data)
}
export function serialRuleUpdate(data: any) {
  return request.post('/base/serialRule/update', data)
}
export function serialRuleStatus(id: number, status: number) {
  return request.post('/base/serialRule/status', null, { params: { id, status } })
}

// ===== 库区 =====
export function areaList(params: ListParams & { warehouseId?: number }) {
  return request.get('/base/area/list', { params }) as Promise<PageResult<any>>
}
export function areaCreate(data: any) {
  return request.post('/base/area/create', data)
}
export function areaUpdate(data: any) {
  return request.post('/base/area/update', data)
}
export function areaStatus(id: number, status: number) {
  return request.post('/base/area/status', null, { params: { id, status } })
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
