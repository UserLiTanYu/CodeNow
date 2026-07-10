import { defineStore } from 'pinia'
import { ref } from 'vue'
import request from '@/utils/request'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)

  async function login(username, password) {
    const res = await request.post('/auth/login', { username, password })
    token.value = res.data.token
    userInfo.value = res.data
    localStorage.setItem('token', res.data.token)
    return res
  }

  async function logout() {
    try {
      await request.post('/auth/logout')
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
    }
  }

  async function fetchUserInfo() {
    const res = await request.get('/auth/me')
    userInfo.value = res.data
    return res.data
  }

  return { token, userInfo, login, logout, fetchUserInfo }
})
