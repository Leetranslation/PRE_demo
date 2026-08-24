import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, type LoginParams } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const username = ref<string>(localStorage.getItem('username') || '')
  const realName = ref<string>(localStorage.getItem('realName') || '')

  async function login(params: LoginParams) {
    const res = await loginApi(params)
    token.value = res.token
    username.value = res.username
    realName.value = res.realName
    localStorage.setItem('token', res.token)
    localStorage.setItem('username', res.username)
    localStorage.setItem('realName', res.realName || '')
  }

  function logout() {
    token.value = ''
    username.value = ''
    realName.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('username')
    localStorage.removeItem('realName')
  }

  return { token, username, realName, login, logout }
})
