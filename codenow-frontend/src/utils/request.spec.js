import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import router from '@/router'
import { useUserStore } from '@/stores/user'
import request, { redirectToLogin } from './request'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
}))

describe('authentication response handling', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
    vi.restoreAllMocks()
  })

  it('clears local and reactive authentication state when redirecting', () => {
    localStorage.setItem('token', 'expired-token')
    const userStore = useUserStore()
    userStore.token = 'expired-token'
    userStore.userInfo = { role: 'AUTHOR' }
    router.currentRoute.value = { path: '/tags', fullPath: '/tags?page=2' }
    const replace = vi.spyOn(router, 'replace').mockResolvedValue()

    redirectToLogin()

    expect(localStorage.getItem('token')).toBeNull()
    expect(userStore.token).toBe('')
    expect(userStore.userInfo).toBeNull()
    expect(replace).toHaveBeenCalledWith({
      name: 'login',
      query: { redirect: '/tags?page=2' },
    })
  })

  it('normalizes an encoded localized path before redirecting to login', () => {
    router.currentRoute.value = {
      path: '/author-console/articles',
      fullPath: '/author-console/articles?keyword=%E4%BD%9C%E8%80%85',
    }
    const replace = vi.spyOn(router, 'replace').mockResolvedValue()

    redirectToLogin()

    expect(replace).toHaveBeenCalledWith({
      name: 'login',
      query: { redirect: '/author-console/articles?keyword=作者' },
    })
  })

  it('handles a business 401 response from a protected endpoint', async () => {
    localStorage.setItem('token', 'expired-token')
    router.currentRoute.value = { path: '/articles', fullPath: '/articles' }
    const replace = vi.spyOn(router, 'replace').mockResolvedValue()

    await expect(request.get('/auth/me', {
      adapter: async (config) => ({
        data: { code: 401, message: '登录已失效，请重新登录' },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      }),
    })).rejects.toThrow('登录已失效，请重新登录')

    expect(localStorage.getItem('token')).toBeNull()
    expect(replace).toHaveBeenCalledWith({
      name: 'login',
      query: { redirect: '/articles' },
    })
  })
})
