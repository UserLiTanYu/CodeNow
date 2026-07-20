/**
 * 头像工具函数
 * 提供头像 URL 处理和默认头像回退逻辑
 */

/**
 * 默认头像路径
 * @type {string}
 */
export const DEFAULT_AVATAR = '/images/default-avatar.svg'

/**
 * 获取头像 URL，若无自定义头像则返回默认头像
 * @param {string} url - 用户自定义头像 URL
 * @returns {string} 有效的头像 URL
 */
export function avatarUrl(url) {
  return url || DEFAULT_AVATAR
}

/**
 * 图片加载失败时回退为默认头像的事件处理函数
 * 通常绑定到 img 标签的 @error 事件
 * @param {Event} event - 图片加载错误事件
 * @returns {void}
 */
export function useDefaultAvatar(event) {
  const image = event.currentTarget
  image.onerror = null
  image.src = DEFAULT_AVATAR
}
