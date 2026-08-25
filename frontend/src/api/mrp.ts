import request from '@/utils/request'

export function mrpCalculate(data: any) {
  return request.post('/mrp/calculate', data)
}
export function mrpPlans() {
  return request.get('/mrp/plans') as Promise<any[]>
}
export function mrpRequirements(planId: number) {
  return request.get('/mrp/requirements', { params: { planId } }) as Promise<any[]>
}
export function mrpConfirm(data: any) {
  return request.post('/mrp/confirm', data)
}
