/**
 * 分类管理 API（管理员端）
 * 提供分类的增删改查操作
 */
import request from '@/utils/request'

/**
 * 获取分类列表（树形结构）
 * @returns {Promise<Object>} 分类列表数据
 */
export const getCategories = () => request.get('/categories')

/**
 * 创建新分类
 * @param {Object} data - 分类数据，包含 name、parentId 等
 * @returns {Promise<Object>} 创建结果
 */
export const createCategory = (data) => request.post('/categories', data)

/**
 * 更新分类
 * @param {number|string} id - 分类 ID
 * @param {Object} data - 更新的分类数据
 * @returns {Promise<Object>} 更新结果
 */
export const updateCategory = (id, data) => request.put(`/categories/${id}`, data)

/**
 * 删除分类
 * @param {number|string} id - 分类 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteCategory = (id) => request.delete(`/categories/${id}`)
