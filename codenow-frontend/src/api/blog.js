import request from '@/utils/request'

export const getBlogArticles = (params) => request.get('/blog/articles', { params })
export const getBlogArticle = (id) => request.get(`/blog/articles/${id}`)
export const getHotArticles = () => request.get('/blog/articles/hot')
export const getBlogCategories = () => request.get('/blog/categories')
export const getBlogTags = () => request.get('/blog/tags')
export const getPublicAuthors = (params) => request.get('/blog/authors', { params })
export const getPublicAuthor = (id) => request.get(`/blog/authors/${id}`)
export const getPublicAuthorArticles = (id, params) => request.get(`/blog/authors/${id}/articles`, { params })
