import { describe, expect, it } from 'vitest'
import { formatDate } from '../format'

describe('formatDate', () => {
  it('formats an ISO local date-time string', () => {
    expect(formatDate('2026-07-15T18:30:45')).toBe('2026-07-15 18:30')
  })

  it('returns an empty string for empty values', () => {
    expect(formatDate('')).toBe('')
    expect(formatDate(null)).toBe('')
  })
})
