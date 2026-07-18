import { beforeEach, describe, expect, it, vi } from 'vitest'

const request = vi.hoisted(() => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  delete: vi.fn(),
}))

vi.mock('@/utils/request', () => ({ default: request }))

import {
  createAuthorArticle,
  deleteAuthorArticle,
  getAuthorArticle,
  getAuthorArticles,
  toggleAuthorArticleStatus,
  updateAuthorArticle,
} from './authorConsole'

describe('author console api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('uses ownership-scoped author article endpoints for CRUD and status', () => {
    const params = { pageNum: 2, pageSize: 10 }
    const payload = { title: 'Stage 2' }

    getAuthorArticles(params)
    getAuthorArticle(11)
    createAuthorArticle(payload)
    updateAuthorArticle(11, payload)
    deleteAuthorArticle(11)
    toggleAuthorArticleStatus(11)

    expect(request.get).toHaveBeenNthCalledWith(1, '/author/articles', { params })
    expect(request.get).toHaveBeenNthCalledWith(2, '/author/articles/11')
    expect(request.post).toHaveBeenCalledWith('/author/articles', payload)
    expect(request.put).toHaveBeenNthCalledWith(1, '/author/articles/11', payload)
    expect(request.put).toHaveBeenNthCalledWith(2, '/author/articles/11/status')
    expect(request.delete).toHaveBeenCalledWith('/author/articles/11')
    expect([...request.get.mock.calls, ...request.post.mock.calls, ...request.put.mock.calls, ...request.delete.mock.calls]
      .flat()
      .filter((value) => typeof value === 'string'))
      .not.toContain('/author/articles/11/top')
  })
})
