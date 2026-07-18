import request from '@/utils/request'

export const getAuthorArticles = (params) => request.get('/author/articles', { params })
export const getAuthorArticle = (id) => request.get(`/author/articles/${id}`)
export const createAuthorArticle = (data) => request.post('/author/articles', data)
export const updateAuthorArticle = (id, data) => request.put(`/author/articles/${id}`, data)
export const deleteAuthorArticle = (id) => request.delete(`/author/articles/${id}`)
export const toggleAuthorArticleStatus = (id) => request.put(`/author/articles/${id}/status`)

export const getAuthorTags = () => request.get('/author/tags')
export const getAuthorComments = (params) => request.get('/author/comments', { params })
export const deleteAuthorComment = (id) => request.delete(`/author/comments/${id}`)
export const uploadAuthorImage = (formData) =>
  request.post('/author/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })