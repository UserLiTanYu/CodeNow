import request from '@/utils/request'

export const changePassword = (data) => request.put('/member/password', data)
export const sendChangeEmailCode = (email) => request.post('/member/email/code', { email })
export const changeEmail = (data) => request.put('/member/email', data)
export const uploadAvatar = (formData) => request.post('/member/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const getMyComments = (params) => request.get('/member/comments', { params })
export const deleteMyComment = (id) => request.delete(`/member/comments/${id}`)
export const getNotifications = (params) => request.get('/member/notifications', { params })
export const getUnreadNotificationCount = () => request.get('/member/notifications/unread-count')
export const markNotificationRead = (id) => request.put(`/member/notifications/${id}/read`)
export const markAllNotificationsRead = () => request.put('/member/notifications/read-all')

export const getFavoriteStatus = (articleId) => request.get(`/member/favorites/${articleId}/status`)
export const addFavorite = (articleId) => request.post(`/member/favorites/${articleId}`)
export const removeFavorite = (articleId) => request.delete(`/member/favorites/${articleId}`)
export const getFavorites = (params) => request.get('/member/favorites', { params })
