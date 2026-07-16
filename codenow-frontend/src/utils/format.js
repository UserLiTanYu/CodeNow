/**
 * 格式化日期字符串
 * @param {string} dateStr - ISO 格式日期字符串
 * @returns {string} 格式化后的日期字符串 (YYYY-MM-DD HH:mm)
 */
export function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}

/**
 * 列表页日期只保留到天，详情页继续使用 formatDate 展示分钟。
 * @param {string} dateStr - ISO 格式日期字符串
 * @returns {string} YYYY-MM-DD
 */
export function formatDateOnly(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}
