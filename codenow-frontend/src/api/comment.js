/**
 * 评论 API
 * 提供评论的树形查询、创建、删除和点赞功能
 */
import request from '@/utils/request'

/**
 * 获取指定文章的评论树
 * @param {number|string} articleId - 文章 ID
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 评论树形结构数据
 */
export const getCommentTree = (articleId, params) => request.get(`/comments/article/${articleId}`, { params })

/**
 * 创建评论
 * @param {Object} data - 评论数据，包含 articleId、content、parentId 等
 * @returns {Promise<Object>} 创建结果
 */
export const createComment = (data) => request.post('/comments', data)

/**
 * 获取评论列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 评论列表数据
 */
export const getComments = (params) => request.get('/comments', { params })

/**
 * 删除评论
 * @param {number|string} id - 评论 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteComment = (id) => request.delete(`/comments/${id}`)

/**
 * 点赞评论
 * @param {number|string} id - 评论 ID
 * @returns {Promise<Object>} 点赞结果
 */
export const likeComment = (id) => request.post(`/comments/${id}/likes`)

/**
 * 取消点赞评论
 * @param {number|string} id - 评论 ID
 * @returns {Promise<Object>} 取消点赞结果
 */
export const unlikeComment = (id) => request.delete(`/comments/${id}/likes`)
