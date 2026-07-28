/**
 * 文档导入工具函数
 * 提供文档文件的解析、标题提取和格式处理功能
 * 支持 Markdown (.md) 和纯文本 (.txt) 文件导入
 */

/**
 * 文档导入最大文件大小（2MB）
 * @type {number}
 */
export const DOCUMENT_IMPORT_MAX_SIZE = 2 * 1024 * 1024

/**
 * 支持导入的文档扩展名列表
 * @type {string[]}
 */
export const DOCUMENT_IMPORT_EXTENSIONS = ['md', 'txt']

/**
 * 获取文件扩展名（小写）
 * @param {string} filename - 文件名
 * @returns {string} 文件扩展名（不含点号），无扩展名时返回空字符串
 */
export function documentExtension(filename = '') {
  const index = filename.lastIndexOf('.')
  return index < 0 ? '' : filename.slice(index + 1).toLowerCase()
}

/**
 * 从文档内容中提取标题
 * 对于 Markdown 文件，优先提取首个一级标题（# 开头）；否则使用文件名作为标题
 * @param {string} filename - 文件名
 * @param {string} content - 文档内容
 * @returns {string} 提取的标题文本
 */
export function documentTitle(filename, content) {
  const extension = documentExtension(filename)
  if (extension === 'md') {
    const heading = content.match(/^\s*#\s+(.+?)\s*#*\s*$/m)
    if (heading?.[1]) return heading[1].trim()
  }
  return filename.replace(/\.[^.]+$/, '').trim()
}

/**
 * 解析文本文档，提取标题和标准化内容
 * 去除 UTF-8 BOM 并统一换行符为 LF，避免跨操作系统格式差异影响解析
 * @param {string} filename - 文件名
 * @param {string} rawContent - 原始文件内容
 * @returns {Object} 包含 title 和 content 的对象
 */
export function parseTextDocument(filename, rawContent) {
  // 去除 UTF-8 BOM 并统一换行，避免快照比较和 Markdown 解析受到操作系统格式影响。
  const content = String(rawContent ?? '').replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n')
  return {
    title: documentTitle(filename, content),
    content,
  }
}
