import request from '@/utils/request'

export function dashboardStats() {
  return request.get('/dashboard/stats') as Promise<any>
}

export function dashboardCockpit() {
  return request.get('/dashboard/cockpit') as Promise<any>
}
