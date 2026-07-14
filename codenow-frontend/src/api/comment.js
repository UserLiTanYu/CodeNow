import request from '@/utils/request'

export const getCommentTree = (articleId) => request.get(`/comments/article/${articleId}`)
export const createComment = (data) => request.post('/comments', data)
export const getComments = (params) => request.get('/comments', { params })
export const deleteComment = (id) => request.delete(`/comments/${id}`)
