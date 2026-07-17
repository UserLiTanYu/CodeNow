import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => userInfo.value?.role?.toUpperCase() === 'ADMIN')

  async function login(account, password, captchaId, captchaCode) {
    const res = await loginApi({ account, password, captchaId, captchaCode })
    token.value = res.data.token
    userInfo.value = res.data
    localStorage.setItem('token', res.data.token)
    return res
  }

  async function logout() {
    try {
      await logoutApi()
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('token')
    }
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data
    return res.data
  }

  return { token, userInfo, isLoggedIn, isAdmin, login, logout, fetchUserInfo }
})
