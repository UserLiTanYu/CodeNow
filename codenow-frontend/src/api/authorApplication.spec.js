import { beforeEach, describe, expect, it, vi } from 'vitest'

const { request } = vi.hoisted(() => ({ request: vi.fn() }))

vi.mock('@/utils/request', () => ({ default: request }))

import {
  approveAuthorApplication,
  cancelAuthorApplication,
  getAuthorApplicationHistory,
  getAuthorApplicationLatest,
  getAuthorApplications,
  rejectAuthorApplication,
  revokeAuthorRole,
  submitAuthorApplication,
} from './authorApplication'

describe('author application api', () => {
  beforeEach(() => request.mockReset())

  it('uses member endpoints for submit, latest, history and cancel', () => {
    const payload = { reason: '申请理由' }
    submitAuthorApplication(payload)
    getAuthorApplicationLatest()
    getAuthorApplicationHistory({ pageNum: 2 })
    cancelAuthorApplication(9)

    expect(request).toHaveBeenNthCalledWith(1, {
      url: '/member/author-applications',
      method: 'post',
      data: payload,
    })
    expect(request).toHaveBeenNthCalledWith(2, {
      url: '/member/author-applications/latest',
      method: 'get',
    })
    expect(request).toHaveBeenNthCalledWith(3, {
      url: '/member/author-applications',
      method: 'get',
      params: { pageNum: 2 },
    })
    expect(request).toHaveBeenNthCalledWith(4, {
      url: '/member/author-applications/9/cancel',
      method: 'put',
    })
  })

  it('uses administrator endpoints for review and role revocation', () => {
    getAuthorApplications({ status: 'PENDING' })
    approveAuthorApplication(4, { reviewRemark: '通过' })
    rejectAuthorApplication(5, { reviewRemark: '材料不足' })
    revokeAuthorRole(7, { reason: '违反规范' })

    expect(request).toHaveBeenNthCalledWith(1, {
      url: '/admin/author-applications',
      method: 'get',
      params: { status: 'PENDING' },
    })
    expect(request).toHaveBeenNthCalledWith(2, {
      url: '/admin/author-applications/4/approve',
      method: 'put',
      data: { reviewRemark: '通过' },
    })
    expect(request).toHaveBeenNthCalledWith(3, {
      url: '/admin/author-applications/5/reject',
      method: 'put',
      data: { reviewRemark: '材料不足' },
    })
    expect(request).toHaveBeenNthCalledWith(4, {
      url: '/admin/users/7/author-role/revoke',
      method: 'put',
      data: { reason: '违反规范' },
    })
  })
})
