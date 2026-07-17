/**
 * 格式化日期字符串
 * @param {string} dateStr - ISO 格式日期字符串
 * @returns {string} 格式化后的日期字符串 (YYYY-MM-DD HH:mm:ss)
 */
export function formatDate(dateStr) {
  if (!dateStr) return ''
  const normalized = String(dateStr).trim().replace('T', ' ')
  const match = normalized.match(/^(\d{4}-\d{2}-\d{2})\s+(\d{2}):(\d{2})(?::(\d{2}))?/)
  if (!match) return normalized
  return `${match[1]} ${match[2]}:${match[3]}:${match[4] || '00'}`
}

export function formatDateCell(_row, _column, cellValue) {
  return formatDate(cellValue)
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
