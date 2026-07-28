/**
 * 标签管理 API（管理员端）
 * 提供标签的增删改查操作
 */
import request from '@/utils/request'

/**
 * 获取标签列表
 * @returns {Promise<Object>} 标签列表数据
 */
export const getTags = () => request.get('/tags')

/**
 * 创建新标签
 * @param {Object} data - 标签数据，包含 name 等
 * @returns {Promise<Object>} 创建结果
 */
export const createTag = (data) => request.post('/tags', data)

/**
 * 更新标签
 * @param {number|string} id - 标签 ID
 * @param {Object} data - 更新的标签数据
 * @returns {Promise<Object>} 更新结果
 */
export const updateTag = (id, data) => request.put(`/tags/${id}`, data)

/**
 * 删除标签
 * @param {number|string} id - 标签 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteTag = (id) => request.delete(`/tags/${id}`)
