import request from '@/utils/request'

// 文章
export const getAuthorArticles = (params) => request.get('/author/articles', { params })
export const getAuthorArticle = (id) => request.get(`/author/articles/${id}`)
export const createAuthorArticle = (data) => request.post('/author/articles', data)
export const updateAuthorArticle = (id, data) => request.put(`/author/articles/${id}`, data)
export const deleteAuthorArticle = (id) => request.delete(`/author/articles/${id}`)
export const toggleAuthorArticleStatus = (id) => request.put(`/author/articles/${id}/status`)

// 分类
export const getAuthorCategories = () => request.get('/author/categories')
export const createAuthorCategory = (data) => request.post('/author/categories', data)
export const updateAuthorCategory = (id, data) => request.put(`/author/categories/${id}`, data)
export const deleteAuthorCategory = (id) => request.delete(`/author/categories/${id}`)

// 标签
export const getAuthorTags = () => request.get('/author/tags')
export const createAuthorTag = (data) => request.post('/author/tags', data)
export const updateAuthorTag = (id, data) => request.put(`/author/tags/${id}`, data)
export const deleteAuthorTag = (id) => request.delete(`/author/tags/${id}`)

// 评论
export const getAuthorComments = (params) => request.get('/author/comments', { params })
export const deleteAuthorComment = (id) => request.delete(`/author/comments/${id}`)

// 上传
export const uploadAuthorImage = (formData) =>
  request.post('/author/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
