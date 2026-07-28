/**
 * 作者申请管理 API
 * 提供作者身份申请的提交、查询、审批和撤回功能
 * 包含会员端接口（/member/）和管理员端接口（/admin/）
 */
import request from '@/utils/request'

/**
 * 提交作者身份申请（会员端）
 * @param {Object} data - 申请信息
 * @returns {Promise<Object>} 提交结果
 */
export function submitAuthorApplication(data) {
  return request({ url: '/member/author-applications', method: 'post', data })
}

/**
 * 获取最新一条作者申请记录（会员端）
 * @returns {Promise<Object>} 最新的申请记录
 */
export function getAuthorApplicationLatest() {
  return request({ url: '/member/author-applications/latest', method: 'get' })
}

/**
 * 获取作者申请历史列表（会员端）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 申请历史列表
 */
export function getAuthorApplicationHistory(params) {
  return request({ url: '/member/author-applications', method: 'get', params })
}

/**
 * 撤回作者申请（会员端）
 * @param {number|string} id - 申请记录 ID
 * @returns {Promise<Object>} 撤回结果
 */
export function cancelAuthorApplication(id) {
  return request({ url: `/member/author-applications/${id}/cancel`, method: 'put' })
}

/**
 * 获取作者申请列表（管理员端）
 * @param {Object} params - 查询参数，如 page、size、status 等
 * @returns {Promise<Object>} 申请列表
 */
export function getAuthorApplications(params) {
  return request({ url: '/admin/author-applications', method: 'get', params })
}

/**
 * 获取单条作者申请详情（管理员端）
 * @param {number|string} id - 申请记录 ID
 * @returns {Promise<Object>} 申请详情
 */
export function getAuthorApplication(id) {
  return request({ url: `/admin/author-applications/${id}`, method: 'get' })
}

/**
 * 通过作者申请（管理员端）
 * @param {number|string} id - 申请记录 ID
 * @param {Object} data - 审批备注等信息
 * @returns {Promise<Object>} 审批结果
 */
export function approveAuthorApplication(id, data) {
  return request({ url: `/admin/author-applications/${id}/approve`, method: 'put', data })
}

/**
 * 拒绝作者申请（管理员端）
 * @param {number|string} id - 申请记录 ID
 * @param {Object} data - 拒绝原因等信息
 * @returns {Promise<Object>} 审批结果
 */
export function rejectAuthorApplication(id, data) {
  return request({ url: `/admin/author-applications/${id}/reject`, method: 'put', data })
}

/**
 * 撤销用户的作者角色（管理员端）
 * @param {number|string} userId - 用户 ID
 * @param {Object} data - 撤销原因等信息
 * @returns {Promise<Object>} 撤销结果
 */
export function revokeAuthorRole(userId, data) {
  return request({ url: `/admin/users/${userId}/author-role/revoke`, method: 'put', data })
}
