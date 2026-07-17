import request from '@/utils/request'

export const getCommentTree = (articleId, params) => request.get(`/comments/article/${articleId}`, { params })
export const createComment = (data) => request.post('/comments', data)
export const getComments = (params) => request.get('/comments', { params })
export const deleteComment = (id) => request.delete(`/comments/${id}`)
export const likeComment = (id) => request.post(`/comments/${id}/likes`)
export const unlikeComment = (id) => request.delete(`/comments/${id}/likes`)
