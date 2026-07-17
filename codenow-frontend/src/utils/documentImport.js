export const DOCUMENT_IMPORT_MAX_SIZE = 2 * 1024 * 1024
export const DOCUMENT_IMPORT_EXTENSIONS = ['md', 'txt']

export function documentExtension(filename = '') {
  const index = filename.lastIndexOf('.')
  return index < 0 ? '' : filename.slice(index + 1).toLowerCase()
}

export function documentTitle(filename, content) {
  const extension = documentExtension(filename)
  if (extension === 'md') {
    const heading = content.match(/^\s*#\s+(.+?)\s*#*\s*$/m)
    if (heading?.[1]) return heading[1].trim()
  }
  return filename.replace(/\.[^.]+$/, '').trim()
}

export function parseTextDocument(filename, rawContent) {
  const content = String(rawContent ?? '').replace(/^\uFEFF/, '').replace(/\r\n?/g, '\n')
  return {
    title: documentTitle(filename, content),
    content,
  }
}
