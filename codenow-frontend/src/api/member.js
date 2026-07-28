/**
 * 会员中心 API
 * 提供密码修改、邮箱更换、头像上传、评论管理、通知消息和收藏功能
 */
import request from '@/utils/request'

/**
 * 修改密码
 * @param {Object} data - 包含 oldPassword、newPassword 等
 * @returns {Promise<Object>} 修改结果
 */
export const changePassword = (data) => request.put('/member/password', data)

/**
 * 发送更换邮箱验证码
 * @param {string} email - 新邮箱地址
 * @returns {Promise<Object>} 发送结果
 */
export const sendChangeEmailCode = (email) => request.post('/member/email/code', { email })

/**
 * 更换邮箱
 * @param {Object} data - 包含 email、code 等
 * @returns {Promise<Object>} 更换结果
 */
export const changeEmail = (data) => request.put('/member/email', data)

/**
 * 上传用户头像
 * @param {FormData} formData - 包含头像文件的 FormData 对象
 * @returns {Promise<Object>} 上传结果，包含头像访问地址
 */
export const uploadAvatar = (formData) => request.post('/member/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

/**
 * 获取我的评论列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 评论列表数据
 */
export const getMyComments = (params) => request.get('/member/comments', { params })

/**
 * 删除我的评论
 * @param {number|string} id - 评论 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteMyComment = (id) => request.delete(`/member/comments/${id}`)

/**
 * 获取通知消息列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 通知列表数据
 */
export const getNotifications = (params) => request.get('/member/notifications', { params })

/**
 * 获取未读通知数量
 * @returns {Promise<Object>} 未读通知数量
 */
export const getUnreadNotificationCount = () => request.get('/member/notifications/unread-count')

/**
 * 标记单条通知为已读
 * @param {number|string} id - 通知 ID
 * @returns {Promise<Object>} 标记结果
 */
export const markNotificationRead = (id) => request.put(`/member/notifications/${id}/read`)

/**
 * 标记所有通知为已读
 * @returns {Promise<Object>} 标记结果
 */
export const markAllNotificationsRead = () => request.put('/member/notifications/read-all')

/**
 * 获取文章收藏状态
 * @param {number|string} articleId - 文章 ID
 * @returns {Promise<Object>} 收藏状态
 */
export const getFavoriteStatus = (articleId) => request.get(`/member/favorites/${articleId}/status`)

/**
 * 收藏文章
 * @param {number|string} articleId - 文章 ID
 * @returns {Promise<Object>} 收藏结果
 */
export const addFavorite = (articleId) => request.post(`/member/favorites/${articleId}`)

/**
 * 取消收藏文章
 * @param {number|string} articleId - 文章 ID
 * @returns {Promise<Object>} 取消收藏结果
 */
export const removeFavorite = (articleId) => request.delete(`/member/favorites/${articleId}`)

/**
 * 获取收藏文章列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 收藏文章列表
 */
export const getFavorites = (params) => request.get('/member/favorites', { params })
