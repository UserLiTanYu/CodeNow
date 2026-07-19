import { beforeEach, describe, expect, it, vi } from 'vitest'

const request = vi.hoisted(() => ({ get: vi.fn() }))
vi.mock('@/utils/request', () => ({ default: request }))

import { getPublicAuthor, getPublicAuthorArticles, getPublicAuthors } from './blog'

describe('public author api', () => {
  beforeEach(() => vi.clearAllMocks())

  it('uses public discovery, profile and author article endpoints', () => {
    const discovery = { pageNum: 1, pageSize: 12, keyword: 'Java', sort: 'popular' }
    const articles = { pageNum: 2, pageSize: 10, sort: 'mostViewed' }

    getPublicAuthors(discovery)
    getPublicAuthor(7)
    getPublicAuthorArticles(7, articles)

    expect(request.get).toHaveBeenNthCalledWith(1, '/blog/authors', { params: discovery })
    expect(request.get).toHaveBeenNthCalledWith(2, '/blog/authors/7')
    expect(request.get).toHaveBeenNthCalledWith(3, '/blog/authors/7/articles', { params: articles })
  })
})
