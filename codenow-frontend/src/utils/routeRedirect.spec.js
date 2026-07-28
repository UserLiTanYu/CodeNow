import { describe, expect, it } from 'vitest'
import { normalizeRedirectTarget } from './routeRedirect'

describe('normalizeRedirectTarget', () => {
  it('decodes an encoded fullPath before nesting it in a login query', () => {
    expect(normalizeRedirectTarget('/blog/authors?keyword=%E4%BD%9C%E8%80%85')).toBe(
      '/blog/authors?keyword=作者',
    )
  })

  it('keeps an invalid percent sequence usable instead of throwing', () => {
    expect(normalizeRedirectTarget('/blog/authors?keyword=%')).toBe('/blog/authors?keyword=%')
  })
})
