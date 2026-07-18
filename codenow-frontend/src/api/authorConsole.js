import request from '@/utils/request'

export const getAuthorArticles = (params) => request.get('/author/articles', { params })
export const getAuthorArticle = (id) => request.get(`/author/articles/${id}`)
export const createAuthorArticle = (data) => request.post('/author/articles', data)
export const updateAuthorArticle = (id, data) => request.put(`/author/articles/${id}`, data)
export const deleteAuthorArticle = (id) => request.delete(`/author/articles/${id}`)
export const toggleAuthorArticleStatus = (id) => request.put(`/author/articles/${id}/status`)