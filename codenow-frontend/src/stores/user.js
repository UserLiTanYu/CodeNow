import { defineStore } from 'pinia'
import { computed, onScopeDispose, ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import { AUTH_INVALIDATED_EVENT } from '@/utils/authSession'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(null)
  const isLoggedIn = computed(() => Boolean(token.value))
  const isAdmin = computed(() => userInfo.value?.role?.toUpperCase() === 'ADMIN')
  const isAuthor = computed(() => userInfo.value?.role?.toUpperCase() === 'AUTHOR')
  const canEnterAuthorConsole = computed(() => ['AUTHOR', 'ADMIN'].includes(userInfo.value?.role?.toUpperCase()))

  function clearSession() {
    token.value = ''
    userInfo.value = null
  }

  if (typeof window !== 'undefined') {
    window.addEventListener(AUTH_INVALIDATED_EVENT, clearSession)
    onScopeDispose(() => window.removeEventListener(AUTH_INVALIDATED_EVENT, clearSession))
  }

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
      clearSession()
      localStorage.removeItem('token')
    }
  }

  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data
    return res.data
  }

  return { token, userInfo, isLoggedIn, isAdmin, isAuthor, canEnterAuthorConsole, login, logout, fetchUserInfo, clearSession }
})
