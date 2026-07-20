/**
 * 作者控制台 API
 * 提供作者端的文章、分类、标签、评论管理和图片上传功能
 * 作者只能操作自己创建的资源
 */
import request from '@/utils/request'

// ==================== 文章管理 ====================

/**
 * 获取作者的文章列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 文章列表数据
 */
export const getAuthorArticles = (params) => request.get('/author/articles', { params })

/**
 * 获取作者单篇文章详情
 * @param {number|string} id - 文章 ID
 * @returns {Promise<Object>} 文章详情数据
 */
export const getAuthorArticle = (id) => request.get(`/author/articles/${id}`)

/**
 * 作者创建新文章
 * @param {Object} data - 文章数据，包含标题、内容、分类、标签等
 * @returns {Promise<Object>} 创建结果
 */
export const createAuthorArticle = (data) => request.post('/author/articles', data)

/**
 * 作者更新文章
 * @param {number|string} id - 文章 ID
 * @param {Object} data - 更新的文章数据
 * @returns {Promise<Object>} 更新结果
 */
export const updateAuthorArticle = (id, data) => request.put(`/author/articles/${id}`, data)

/**
 * 作者删除文章
 * @param {number|string} id - 文章 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteAuthorArticle = (id) => request.delete(`/author/articles/${id}`)

/**
 * 作者切换文章发布状态（发布/草稿）
 * @param {number|string} id - 文章 ID
 * @returns {Promise<Object>} 切换结果
 */
export const toggleAuthorArticleStatus = (id) => request.put(`/author/articles/${id}/status`)

// ==================== 分类管理 ====================

/**
 * 获取作者的分类列表
 * @returns {Promise<Object>} 分类列表数据
 */
export const getAuthorCategories = () => request.get('/author/categories')

/**
 * 作者创建新分类
 * @param {Object} data - 分类数据，包含名称等
 * @returns {Promise<Object>} 创建结果
 */
export const createAuthorCategory = (data) => request.post('/author/categories', data)

/**
 * 作者更新分类
 * @param {number|string} id - 分类 ID
 * @param {Object} data - 更新的分类数据
 * @returns {Promise<Object>} 更新结果
 */
export const updateAuthorCategory = (id, data) => request.put(`/author/categories/${id}`, data)

/**
 * 作者删除分类
 * @param {number|string} id - 分类 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteAuthorCategory = (id) => request.delete(`/author/categories/${id}`)

// ==================== 标签管理 ====================

/**
 * 获取作者的标签列表
 * @returns {Promise<Object>} 标签列表数据
 */
export const getAuthorTags = () => request.get('/author/tags')

/**
 * 作者创建新标签
 * @param {Object} data - 标签数据，包含名称等
 * @returns {Promise<Object>} 创建结果
 */
export const createAuthorTag = (data) => request.post('/author/tags', data)

/**
 * 作者更新标签
 * @param {number|string} id - 标签 ID
 * @param {Object} data - 更新的标签数据
 * @returns {Promise<Object>} 更新结果
 */
export const updateAuthorTag = (id, data) => request.put(`/author/tags/${id}`, data)

/**
 * 作者删除标签
 * @param {number|string} id - 标签 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteAuthorTag = (id) => request.delete(`/author/tags/${id}`)

// ==================== 评论管理 ====================

/**
 * 获取作者文章的评论列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 评论列表数据
 */
export const getAuthorComments = (params) => request.get('/author/comments', { params })

/**
 * 作者删除评论
 * @param {number|string} id - 评论 ID
 * @returns {Promise<Object>} 删除结果
 */
export const deleteAuthorComment = (id) => request.delete(`/author/comments/${id}`)

// ==================== 文件上传 ====================

/**
 * 作者上传图片
 * @param {FormData} formData - 包含图片文件的 FormData 对象
 * @returns {Promise<Object>} 上传结果，包含图片访问地址
 */
export const uploadAuthorImage = (formData) =>
  request.post('/author/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
