import request from '@/utils/request'

export const getFavoriteStatus = (articleId) => request.get(`/member/favorites/${articleId}/status`)
export const addFavorite = (articleId) => request.post(`/member/favorites/${articleId}`)
export const removeFavorite = (articleId) => request.delete(`/member/favorites/${articleId}`)
export const getFavorites = (params) => request.get('/member/favorites', { params })
