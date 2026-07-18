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

  it('registers member author application and admin review routes', async () => {
    const router = (await import('./index')).default
    const routeNames = router.getRoutes().map((route) => route.name)

    expect(routeNames).toContain('author-application')
    expect(routeNames).toContain('admin-author-applications')
  })

  it('registers the author console article list and edit routes with role metadata', async () => {
    const router = (await import('./index')).default
    const routes = router.getRoutes()
    const consoleRoute = routes.find((route) => route.path === '/author-console')
    const listRoute = routes.find((route) => route.name === 'author-articles')
    const editRoute = routes.find((route) => route.name === 'author-article-edit')
    const commentsRoute = routes.find((route) => route.name === 'author-comments')

    expect(consoleRoute?.meta.allowedRoles).toEqual(['AUTHOR', 'ADMIN'])
    expect(listRoute?.path).toBe('/author-console/articles')
    expect(editRoute?.path).toBe('/author-console/articles/edit/:id?')
    expect(commentsRoute?.path).toBe('/author-console/comments')
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

  it.each([
    ['USER', { path: '/blog' }],
    ['AUTHOR', true],
    ['ADMIN', true],
  ])('applies author console allowed roles to %s', async (role, expected) => {
    localStorage.setItem('token', `${role.toLowerCase()}-token`)
    globalThis.fetch = vi.fn().mockResolvedValue({
      status: 200,
      ok: true,
      json: vi.fn().mockResolvedValue({ code: 200, data: { id: 3, role } }),
    })

    await expect(authGuard({
      path: '/author-console/articles',
      fullPath: '/author-console/articles',
      meta: { allowedRoles: ['AUTHOR', 'ADMIN'] },
    })).resolves.toEqual(expected)
  })

  it('revalidates the current role on repeated restricted-route checks', async () => {
    localStorage.setItem('token', 'author-token')
    globalThis.fetch = vi.fn().mockResolvedValue({
      status: 200,
      ok: true,
      json: vi.fn().mockResolvedValue({ code: 200, data: { id: 4, role: 'author' } }),
    })
    const target = {
      path: '/author-console/articles',
      fullPath: '/author-console/articles',
      meta: { allowedRoles: ['AUTHOR', 'ADMIN'] },
    }

    await expect(authGuard(target)).resolves.toBe(true)
    await expect(authGuard(target)).resolves.toBe(true)
    expect(fetch).toHaveBeenCalledTimes(2)
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

  it('fails closed for a restricted route when verification is unavailable', async () => {
    localStorage.setItem('token', 'valid-token')
    globalThis.fetch = vi.fn().mockRejectedValue(new TypeError('network error'))

    await expect(authGuard({ path: '/articles', fullPath: '/articles' })).resolves.toEqual({ path: '/blog' })
    expect(localStorage.getItem('token')).toBe('valid-token')
  })
})
