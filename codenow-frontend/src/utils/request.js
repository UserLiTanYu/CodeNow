import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

export function redirectToLogin() {
  localStorage.removeItem('token')

  const currentRoute = router.currentRoute.value
  if (currentRoute.path === '/login') return

  const query = currentRoute.fullPath && currentRoute.fullPath !== '/'
    ? { redirect: currentRoute.fullPath }
    : {}
  router.replace({ name: 'login', query })
}

function isLoginRequest(config) {
  return config?.url === '/auth/login'
}

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

// 响应拦截器：统一处理错误
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
