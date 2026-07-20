/**
 * 文章管理 API（管理员端）
 * 提供文章的增删改查、状态切换和置顶操作
 */
import request from '@/utils/request'

/**
 * 获取文章列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 文章列表数据
 */
export const getArticles = (params) => request.get('/articles', { params })

/**
 * 获取单篇文章详情
 * @param {number|string} id - 文章 ID
 * @returns {Promise<Object>} 文章详情数据
 */
export const getArticle = (id) => request.get(`/articles/${id}`)

/**
 * 创建新文章
 * @param {Object} data - 文章数据，包含标题、内容、分类、标签等
 * @returns {Promise<Object>} 创建结果
 */
export const createArticle = (data) => request.post('/articles', data)

/**
 * 更新文章
 * @param {number|string} id - 文章 ID
 * @param {Object} data - 更新的文章数据
 * @returns {Promise<Object>} 更新结果
 */
export const updateArticle = (id, data) => request.put(`/articles/${id}`, data)

/**
 * 删除文章
 * @param {number|string} id - 文章 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteArticle = (id) => request.delete(`/articles/${id}`)

/**
 * 切换文章发布状态（发布/草稿）
 * @param {number|string} id - 文章 ID
 * @returns {Promise<Object>} 切换结果
 */
export const toggleArticleStatus = (id) => request.put(`/articles/${id}/status`)

/**
 * 切换文章置顶状态
 * @param {number|string} id - 文章 ID
 * @returns {Promise<Object>} 切换结果
 */
export const toggleArticleTop = (id) => request.put(`/articles/${id}/top`)
