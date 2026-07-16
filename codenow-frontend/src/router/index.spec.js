import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authGuard, resetTokenVerification } from './index'

describe('authGuard', () => {
  beforeEach(() => {
    localStorage.clear()
    resetTokenVerification()
    vi.restoreAllMocks()
  })

  it('allows public blog routes without a token', async () => {
    globalThis.fetch = vi.fn()

    await expect(authGuard({ path: '/blog', fullPath: '/blog' })).resolves.toBe(true)
    expect(fetch).not.toHaveBeenCalled()
  })

  it('redirects unauthenticated users and preserves the target route', async () => {
    await expect(authGuard({ path: '/articles', fullPath: '/articles?status=1' })).resolves.toEqual({
      name: 'login',
      query: { redirect: '/articles?status=1' },
    })
  })

  it('requires login for the member profile', async () => {
    await expect(authGuard({ path: '/blog/profile', fullPath: '/blog/profile', meta: { requiresAuth: true } })).resolves.toEqual({
      name: 'login',
      query: { redirect: '/blog/profile' },
    })
  })

  it('allows a valid token after checking the current user', async () => {
    localStorage.setItem('token', 'valid-token')
    globalThis.fetch = vi.fn().mockResolvedValue({
      status: 200,
      ok: true,
      json: vi.fn().mockResolvedValue({ code: 200, data: { id: 1, role: 'ADMIN' } }),
    })

    await expect(authGuard({ path: '/articles', fullPath: '/articles' })).resolves.toBe(true)
    expect(fetch).toHaveBeenCalledWith('/api/auth/me', {
      headers: { Authorization: 'valid-token' },
    })
  })

  it('prevents ordinary users from entering the admin area', async () => {
    localStorage.setItem('token', 'member-token')
    globalThis.fetch = vi.fn().mockResolvedValue({
      status: 200,
      ok: true,
      json: vi.fn().mockResolvedValue({ code: 200, data: { id: 2, role: 'USER' } }),
    })

    await expect(authGuard({ path: '/users', fullPath: '/users', meta: { requiresAdmin: true } })).resolves.toEqual({ path: '/blog' })
  })

  it('clears an invalid token and redirects to login', async () => {
    localStorage.setItem('token', 'expired-token')
    globalThis.fetch = vi.fn().mockResolvedValue({ status: 401, ok: false })

    await expect(authGuard({ path: '/comments', fullPath: '/comments' })).resolves.toEqual({
      name: 'login',
      query: { redirect: '/comments' },
    })
    expect(localStorage.getItem('token')).toBeNull()
  })

  it('keeps the token when verification fails because the service is unavailable', async () => {
    localStorage.setItem('token', 'valid-token')
    globalThis.fetch = vi.fn().mockRejectedValue(new TypeError('network error'))

    await expect(authGuard({ path: '/articles', fullPath: '/articles' })).resolves.toBe(true)
    expect(localStorage.getItem('token')).toBe('valid-token')
  })
})
