import request from '@/utils/request'

export const uploadImage = (formData) =>
  request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

export const importArticlePackage = (formData) =>
  request.post('/upload/article-package', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
