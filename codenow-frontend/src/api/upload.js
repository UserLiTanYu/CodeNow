/**
 * 文件上传 API（管理员端）
 * 提供图片上传和文章包导入功能
 */
import request from '@/utils/request'

/**
 * 上传图片
 * @param {FormData} formData - 包含图片文件的 FormData 对象
 * @returns {Promise<Object>} 上传结果，包含图片访问地址
 */
export const uploadImage = (formData) =>
  request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

/**
 * 导入文章包（支持 Markdown/Txt 批量导入）
 * @param {FormData} formData - 包含文章包文件的 FormData 对象
 * @returns {Promise<Object>} 导入结果
 */
export const importArticlePackage = (formData) =>
  request.post('/upload/article-package', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000,
  })
