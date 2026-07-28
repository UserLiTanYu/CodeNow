/**
 * 用户管理 API（管理员端）
 * 提供用户列表查询、状态管理和登录日志查看功能
 */
import request from '@/utils/request'

/**
 * 获取用户列表（分页）
 * @param {Object} params - 查询参数，如 page、size、keyword 等
 * @returns {Promise<Object>} 用户列表数据
 */
export const getUsers = (params) => request.get('/admin/users', { params })

/**
 * 更新用户状态（启用/禁用）
 * @param {number|string} id - 用户 ID
 * @param {string} status - 目标状态
 * @param {string} reason - 状态变更原因
 * @returns {Promise<Object>} 更新结果
 */
export const updateUserStatus = (id, status, reason) => request.put(`/admin/users/${id}/status`, { status, reason })

/**
 * 获取登录日志列表（分页）
 * @param {Object} params - 查询参数，如 page、size 等分页信息
 * @returns {Promise<Object>} 登录日志列表数据
 */
export const getLoginLogs = (params) => request.get('/admin/users/login-logs', { params })
