import { beforeEach, describe, expect, it, vi } from 'vitest'
import router from '@/router'
import request, { redirectToLogin } from './request'

vi.mock('element-plus', () => ({
  ElMessage: { error: vi.fn() },
}))

describe('authentication response handling', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('clears the token and preserves the current route when redirecting', () => {
    localStorage.setItem('token', 'expired-token')
    router.currentRoute.value = { path: '/tags', fullPath: '/tags?page=2' }
    const replace = vi.spyOn(router, 'replace').mockResolvedValue()

    redirectToLogin()

    expect(localStorage.getItem('token')).toBeNull()
    expect(replace).toHaveBeenCalledWith({
      name: 'login',
      query: { redirect: '/tags?page=2' },
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
