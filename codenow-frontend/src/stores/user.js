/**
 * 用户状态管理 Store
 * 基于 Pinia 管理用户认证状态，包括 Token、用户信息和角色权限
 * Token 持久化在 localStorage，用户资料只驻留 Pinia；页面刷新后按需重新获取资料
 * isLoggedIn 仅表示本地存在 Token，服务端有效性仍由路由守卫和响应拦截器确认
 */
import { defineStore } from 'pinia'
import { computed, onScopeDispose, ref } from 'vue'
import { login as loginApi, logout as logoutApi, getUserInfo } from '@/api/auth'
import { AUTH_INVALIDATED_EVENT } from '@/utils/authSession'

/**
 * 用户 Store
 * 管理用户登录状态、Token、用户信息及角色判断
 * @returns {Object} Store 实例，包含状态和方法
 */
export const useUserStore = defineStore('user', () => {
  /** @type {import('vue').Ref<string>} 用户认证 Token */
  const token = ref(localStorage.getItem('token') || '')

  /** @type {import('vue').Ref<Object|null>} 用户信息对象 */
  const userInfo = ref(null)

  /** @type {import('vue').ComputedRef<boolean>} 是否已登录（本地存在 Token） */
  const isLoggedIn = computed(() => Boolean(token.value))

  /** @type {import('vue').ComputedRef<boolean>} 是否为管理员角色 */
  const isAdmin = computed(() => userInfo.value?.role?.toUpperCase() === 'ADMIN')

  /** @type {import('vue').ComputedRef<boolean>} 是否为作者角色 */
  const isAuthor = computed(() => userInfo.value?.role?.toUpperCase() === 'AUTHOR')

  /** @type {import('vue').ComputedRef<boolean>} 是否可以进入作者控制台（作者或管理员） */
  const canEnterAuthorConsole = computed(() => ['AUTHOR', 'ADMIN'].includes(userInfo.value?.role?.toUpperCase()))

  /**
   * 清除会话状态（内存中的 Token 和用户信息）
   * @returns {void}
   */
  function clearSession() {
    token.value = ''
    userInfo.value = null
  }

  // 监听全局认证失效事件，自动清理内存状态
  if (typeof window !== 'undefined') {
    window.addEventListener(AUTH_INVALIDATED_EVENT, clearSession)
    onScopeDispose(() => window.removeEventListener(AUTH_INVALIDATED_EVENT, clearSession))
  }

  /**
   * 用户登录
   * @param {string} account - 账号
   * @param {string} password - 密码
   * @param {string} captchaId - 验证码 ID
   * @param {string} captchaCode - 验证码
   * @returns {Promise<Object>} 登录响应数据
   */
  async function login(account, password, captchaId, captchaCode) {
    const res = await loginApi({ account, password, captchaId, captchaCode })
    token.value = res.data.token
    userInfo.value = res.data
    localStorage.setItem('token', res.data.token)
    return res
  }

  /**
   * 用户登出
   * 调用后端登出接口并清除本地状态
   * @returns {Promise<void>}
   */
  async function logout() {
    try {
      await logoutApi()
    } finally {
      clearSession()
      localStorage.removeItem('token')
    }
  }

  /**
   * 获取当前用户信息
   * 通常在页面刷新后调用，用于恢复用户资料
   * @returns {Promise<Object>} 用户信息数据
   */
  async function fetchUserInfo() {
    const res = await getUserInfo()
    userInfo.value = res.data
    return res.data
  }

  return { token, userInfo, isLoggedIn, isAdmin, isAuthor, canEnterAuthorConsole, login, logout, fetchUserInfo, clearSession }
})
