import request from '@/utils/request'

export function dashboardStats() {
  return request.get('/dashboard/stats') as Promise<any>
}
