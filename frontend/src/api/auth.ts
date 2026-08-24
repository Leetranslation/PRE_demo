import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: number
  username: string
  realName: string
}

export function login(data: LoginParams) {
  return request.post('/auth/login', data) as Promise<LoginResult>
}
