/**
 * 认证与用户资料 API
 * 提供登录、注册、密码重置、验证码获取及个人信息管理功能
 */
import request from '@/utils/request'

/**
 * 用户登录
 * @param {Object} data - 登录信息，包含 account、password、captchaId、captchaCode
 * @returns {Promise<Object>} 登录结果，包含 token 和用户信息
 */
export const login = (data) => request.post('/auth/login', data)

/**
 * 获取图形验证码
 * @returns {Promise<Object>} 验证码数据，包含 captchaId 和验证码图片
 */
export const getCaptcha = () => request.get('/auth/captcha')

/**
 * 用户登出
 * @returns {Promise<Object>} 登出结果
 */
export const logout = () => request.post('/auth/logout')

/**
 * 获取当前登录用户信息
 * @returns {Promise<Object>} 当前用户信息
 */
export const getUserInfo = () => request.get('/auth/me')

/**
 * 发送注册邮箱验证码
 * @param {string} email - 注册邮箱地址
 * @returns {Promise<Object>} 发送结果
 */
export const sendRegisterCode = (email) => request.post('/auth/register/code', { email })

/**
 * 用户注册
 * @param {Object} data - 注册信息，包含用户名、密码、邮箱、验证码等
 * @returns {Promise<Object>} 注册结果
 */
export const register = (data) => request.post('/auth/register', data)

/**
 * 发送密码重置邮箱验证码
 * @param {string} email - 注册邮箱地址
 * @returns {Promise<Object>} 发送结果
 */
export const sendResetCode = (email) => request.post('/auth/password/code', { email })

/**
 * 重置密码
 * @param {Object} data - 重置信息，包含邮箱、验证码、新密码等
 * @returns {Promise<Object>} 重置结果
 */
export const resetPassword = (data) => request.post('/auth/password/reset', data)

/**
 * 获取当前用户个人资料
 * @returns {Promise<Object>} 用户个人资料数据
 */
export const getProfile = () => request.get('/member/profile')

/**
 * 更新当前用户个人资料
 * @param {Object} data - 更新的个人资料数据
 * @returns {Promise<Object>} 更新结果
 */
export const updateProfile = (data) => request.put('/member/profile', data)
