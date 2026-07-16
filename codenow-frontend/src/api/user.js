import request from '@/utils/request'

export const getUsers = (params) => request.get('/admin/users', { params })
export const updateUserStatus = (id, status, reason) => request.put(`/admin/users/${id}/status`, { status, reason })
export const getLoginLogs = (params) => request.get('/admin/users/login-logs', { params })
