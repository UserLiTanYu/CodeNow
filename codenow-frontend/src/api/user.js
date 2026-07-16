import request from '@/utils/request'

export const getUsers = (params) => request.get('/admin/users', { params })
export const updateUserStatus = (id, status) => request.put(`/admin/users/${id}/status`, { status })
