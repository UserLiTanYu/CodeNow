/**
 * 格式化日期字符串
 * @param {string} dateStr - ISO 格式日期字符串
 * @returns {string} 格式化后的日期字符串 (YYYY-MM-DD HH:mm)
 */
export function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.replace('T', ' ').substring(0, 16)
}
