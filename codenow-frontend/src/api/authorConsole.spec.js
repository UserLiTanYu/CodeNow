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
  createAuthorCategory,
  createAuthorTag,
  deleteAuthorArticle,
  deleteAuthorCategory,
  deleteAuthorComment,
  deleteAuthorTag,
  getAuthorArticle,
  getAuthorArticles,
  getAuthorCategories,
  getAuthorComments,
  getAuthorProfile,
  getAuthorTags,
  toggleAuthorArticleStatus,
  updateAuthorArticle,
  updateAuthorCategory,
  updateAuthorProfile,
  updateAuthorTag,
  uploadAuthorImage,
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

  it('uses author-scoped tag, comment and image upload endpoints', () => {
    const params = { pageNum: 1, pageSize: 10, articleId: 9 }
    const formData = new FormData()

    getAuthorTags()
    getAuthorComments(params)
    deleteAuthorComment(5)
    uploadAuthorImage(formData)

    expect(request.get).toHaveBeenNthCalledWith(1, '/author/tags')
    expect(request.get).toHaveBeenNthCalledWith(2, '/author/comments', { params })
    expect(request.delete).toHaveBeenCalledWith('/author/comments/5')
    expect(request.post).toHaveBeenCalledWith('/author/upload/image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  })

  it('uses author-scoped category and tag CRUD endpoints', () => {
    const category = { name: 'Java' }
    const tag = { name: 'Spring Boot' }

    getAuthorCategories()
    createAuthorCategory(category)
    updateAuthorCategory(3, category)
    deleteAuthorCategory(3)
    getAuthorTags()
    createAuthorTag(tag)
    updateAuthorTag(4, tag)
    deleteAuthorTag(4)

    expect(request.get).toHaveBeenNthCalledWith(1, '/author/categories')
    expect(request.post).toHaveBeenNthCalledWith(1, '/author/categories', category)
    expect(request.put).toHaveBeenNthCalledWith(1, '/author/categories/3', category)
    expect(request.delete).toHaveBeenNthCalledWith(1, '/author/categories/3')
    expect(request.get).toHaveBeenNthCalledWith(2, '/author/tags')
    expect(request.post).toHaveBeenNthCalledWith(2, '/author/tags', tag)
    expect(request.put).toHaveBeenNthCalledWith(2, '/author/tags/4', tag)
    expect(request.delete).toHaveBeenNthCalledWith(2, '/author/tags/4')
  })

  it('uses the current author profile endpoint for reading and updating', () => {
    const profile = { bio: '专注后端开发', expertise: ['Java'] }

    getAuthorProfile()
    updateAuthorProfile(profile)

    expect(request.get).toHaveBeenCalledWith('/author/profile')
    expect(request.put).toHaveBeenCalledWith('/author/profile', profile)
  })
})
