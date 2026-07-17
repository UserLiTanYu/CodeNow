import { describe, expect, it } from 'vitest'
import { documentExtension, documentTitle, parseTextDocument } from './documentImport'

describe('documentImport', () => {
  it('normalizes extensions', () => {
    expect(documentExtension('Guide.MD')).toBe('md')
    expect(documentExtension('README')).toBe('')
  })

  it('uses the first markdown h1 as title', () => {
    expect(documentTitle('guide.md', 'intro\n# Java 集合入门\n正文')).toBe('Java 集合入门')
  })

  it('falls back to filename for txt', () => {
    expect(documentTitle('学习笔记.txt', '第一行正文')).toBe('学习笔记')
  })

  it('removes utf8 bom and normalizes newlines', () => {
    expect(parseTextDocument('note.txt', '\uFEFF第一行\r\n第二行')).toEqual({
      title: 'note',
      content: '第一行\n第二行',
    })
  })
})
