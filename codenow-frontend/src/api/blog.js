import request from '@/utils/request'

export const getBlogArticles = (params) => request.get('/blog/articles', { params })
export const getBlogArticle = (id) => request.get(`/blog/articles/${id}`)
export const getHotArticles = (params) => request.get('/blog/articles/hot', { params })
export const getBlogCategories = () => request.get('/blog/categories')
export const getBlogTags = () => request.get('/blog/tags')
