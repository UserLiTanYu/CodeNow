import request from '@/utils/request'

export function submitAuthorApplication(data) {
  return request({ url: '/member/author-applications', method: 'post', data })
}

export function getAuthorApplicationLatest() {
  return request({ url: '/member/author-applications/latest', method: 'get' })
}

export function getAuthorApplicationHistory(params) {
  return request({ url: '/member/author-applications', method: 'get', params })
}

export function cancelAuthorApplication(id) {
  return request({ url: `/member/author-applications/${id}/cancel`, method: 'put' })
}

export function getAuthorApplications(params) {
  return request({ url: '/admin/author-applications', method: 'get', params })
}

export function getAuthorApplication(id) {
  return request({ url: `/admin/author-applications/${id}`, method: 'get' })
}

export function approveAuthorApplication(id, data) {
  return request({ url: `/admin/author-applications/${id}/approve`, method: 'put', data })
}

export function rejectAuthorApplication(id, data) {
  return request({ url: `/admin/author-applications/${id}/reject`, method: 'put', data })
}

export function revokeAuthorRole(userId, data) {
  return request({ url: `/admin/users/${userId}/author-role/revoke`, method: 'put', data })
}
