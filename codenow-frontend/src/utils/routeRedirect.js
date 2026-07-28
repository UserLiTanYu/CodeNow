/**
 * Vue Router 的 fullPath 已经包含百分号编码。它作为另一个路由的 query 值时，
 * 需要先还原一次，否则 `%E4...` 会再次变成 `%25E4...`。
 */
export function normalizeRedirectTarget(fullPath) {
  if (!fullPath) return ''
  try {
    return decodeURIComponent(fullPath)
  } catch {
    return fullPath
  }
}
