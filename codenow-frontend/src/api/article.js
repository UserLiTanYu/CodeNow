import request from '@/utils/request'

export const getArticles = (params) => request.get('/articles', { params })
export const getArticle = (id) => request.get(`/articles/${id}`)
export const createArticle = (data) => request.post('/articles', data)
export const updateArticle = (id, data) => request.put(`/articles/${id}`, data)
export const deleteArticle = (id) => request.delete(`/articles/${id}`)
export const toggleArticleStatus = (id) => request.put(`/articles/${id}/status`)
export const toggleArticleTop = (id) => request.put(`/articles/${id}/top`)
