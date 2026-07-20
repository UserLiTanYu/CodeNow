/**
 * 博客前台 API
 * 提供博客公开展示页面的文章、分类、标签、作者等查询接口
 */
import request from '@/utils/request'

/**
 * 获取博客文章列表（分页）
 * @param {Object} params - 查询参数，如 page、size、categoryId、tagId 等
 * @returns {Promise<Object>} 文章列表数据
 */
export const getBlogArticles = (params) => request.get('/blog/articles', { params })

/**
 * 获取博客文章详情
 * @param {number|string} id - 文章 ID
 * @returns {Promise<Object>} 文章详情数据
 */
export const getBlogArticle = (id) => request.get(`/blog/articles/${id}`)

/**
 * 获取热门文章列表
 * @returns {Promise<Object>} 热门文章列表
 */
export const getHotArticles = () => request.get('/blog/articles/hot')

/**
 * 获取博客分类列表
 * @returns {Promise<Object>} 分类列表数据（树形结构）
 */
export const getBlogCategories = () => request.get('/blog/categories')

/**
 * 获取博客标签列表
 * @returns {Promise<Object>} 标签列表数据
 */
export const getBlogTags = () => request.get('/blog/tags')

/**
 * 获取公开作者列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 作者列表数据
 */
export const getPublicAuthors = (params) => request.get('/blog/authors', { params })

/**
 * 获取公开作者详情
 * @param {number|string} id - 作者 ID
 * @returns {Promise<Object>} 作者详情数据
 */
export const getPublicAuthor = (id) => request.get(`/blog/authors/${id}`)

/**
 * 获取指定作者的文章列表（分页）
 * @param {number|string} id - 作者 ID
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 该作者的文章列表
 */
export const getPublicAuthorArticles = (id, params) => request.get(`/blog/authors/${id}/articles`, { params })

/**
 * 获取指定作者的分类列表
 * @param {number|string} id - 作者 ID
 * @returns {Promise<Object>} 该作者的分类列表
 */
export const getPublicAuthorCategories = (id) => request.get(`/blog/authors/${id}/categories`)

/**
 * 获取指定作者的标签列表
 * @param {number|string} id - 作者 ID
 * @returns {Promise<Object>} 该作者的标签列表
 */
export const getPublicAuthorTags = (id) => request.get(`/blog/authors/${id}/tags`)
