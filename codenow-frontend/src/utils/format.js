/**
 * 日期格式化工具函数
 * 提供日期字符串的多种格式化方法，适用于列表页和详情页等不同场景
 */

/**
 * 格式化日期字符串为完整日期时间格式
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

/**
 * Element Plus 表格列格式化函数，用于将单元格日期值格式化为完整日期时间
 * @param {Object} _row - 行数据（未使用）
 * @param {Object} _column - 列配置（未使用）
 * @param {string} cellValue - 单元格原始值
 * @returns {string} 格式化后的日期字符串
 */
export function formatDateCell(_row, _column, cellValue) {
  return formatDate(cellValue)
}

/**
 * 格式化日期字符串为仅日期格式
 * 列表页日期只保留到天，详情页继续使用 formatDate 展示分钟
 * @param {string} dateStr - ISO 格式日期字符串
 * @returns {string} YYYY-MM-DD 格式的日期字符串
 */
export function formatDateOnly(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}
