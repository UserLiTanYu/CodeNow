/**
 * 认证会话管理工具
 * 提供统一的会话失效处理，通过浏览器事件通知各 Store 清理内存状态
 * 请求层不能直接依赖 Pinia，因此使用事件机制实现跨模块通信
 */

/**
 * 认证失效事件名称
 * @type {string}
 */
export const AUTH_INVALIDATED_EVENT = 'codenow:auth-invalidated'

/**
 * 使当前认证会话失效
 * 清除 localStorage 中的 Token，并触发浏览器事件通知所有监听者清理状态
 * @returns {void}
 */
export function invalidateAuthSession() {
  // 请求层不能直接依赖 Pinia；通过浏览器事件通知所有已挂载的用户 Store 清理内存状态。
  localStorage.removeItem('token')
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new Event(AUTH_INVALIDATED_EVENT))
  }
}
