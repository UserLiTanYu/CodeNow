import request from '@/utils/request'

export const login = (data) => request.post('/auth/login', data)
export const logout = () => request.post('/auth/logout')
export const getUserInfo = () => request.get('/auth/me')
export const sendRegisterCode = (email) => request.post('/auth/register/code', { email })
export const register = (data) => request.post('/auth/register', data)
export const sendResetCode = (email) => request.post('/auth/password/code', { email })
export const resetPassword = (data) => request.post('/auth/password/reset', data)
export const getProfile = () => request.get('/member/profile')
export const updateProfile = (data) => request.put('/member/profile', data)
