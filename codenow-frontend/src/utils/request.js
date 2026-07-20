/**
 * HTTP 请求工具模块
 * 基于 Axios 创建统一的请求实例，提供 Token 自动注入、
 * 业务错误码处理和会话失效管理功能
 */
import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import { invalidateAuthSession } from '@/utils/authSession'

/**
 * Axios 请求实例
 * 统一 Token 注入、业务错误码和 HTTP 错误使用同一套会话失效策略
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

/**
 * 跳转到登录页面
 * 先清除本地认证状态，然后携带当前路径作为 redirect 参数跳转到登录页
 * 如果已在登录页则不重复跳转
 * @returns {void}
 */
export function redirectToLogin() {
  invalidateAuthSession()

  const currentRoute = router.currentRoute.value
  if (currentRoute.path === '/login') return

  const query = currentRoute.fullPath && currentRoute.fullPath !== '/'
    ? { redirect: currentRoute.fullPath }
    : {}
  router.replace({ name: 'login', query })
}

/**
 * 判断当前请求是否为认证相关请求（登录、注册、密码重置等）
 * 认证请求的错误不触发自动跳转，避免密码或验证码错误造成重定向循环
 * @param {Object} config - Axios 请求配置
 * @returns {boolean} 是否为认证请求
 */
function isLoginRequest(config) {
  return [
    '/auth/login',
    '/auth/register',
    '/auth/register/code',
    '/auth/password/code',
    '/auth/password/reset',
  ].includes(config?.url)
}

/**
 * 判断状态码是否表示未授权
 * @param {number} code - HTTP 状态码或业务状态码
 * @returns {boolean} 是否为 401 或 403
 */
function isUnauthorized(code) {
  return code === 401 || code === 403
}

// 请求拦截器：自动带上 Token
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 后端可能用 HTTP 状态或响应体业务码报告 401/403；两条通道都归一为会话失效。
// 认证入口排除自动跳转，避免密码或验证码错误造成重定向循环。
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (isUnauthorized(res.code) && !isLoginRequest(response.config)) {
        redirectToLogin()
      }
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    const status = error.response?.status
    const code = error.response?.data?.code
    if ((isUnauthorized(status) || isUnauthorized(code)) && !isLoginRequest(error.config)) {
      redirectToLogin()
    }
    ElMessage.error(error.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
