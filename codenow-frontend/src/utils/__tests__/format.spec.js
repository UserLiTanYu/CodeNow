import { describe, expect, it } from 'vitest'
import { formatDate, formatDateOnly } from '../format'

describe('formatDate', () => {
  it('formats an ISO local date-time string', () => {
    expect(formatDate('2026-07-15T18:30:45')).toBe('2026-07-15 18:30')
  })

  it('returns an empty string for empty values', () => {
    expect(formatDate('')).toBe('')
    expect(formatDate(null)).toBe('')
  })
})

describe('formatDateOnly', () => {
  it('keeps only the calendar date for list pages', () => {
    expect(formatDateOnly('2026-07-15T18:30:45')).toBe('2026-07-15')
  })

  it('returns an empty string for empty values', () => {
    expect(formatDateOnly('')).toBe('')
    expect(formatDateOnly(null)).toBe('')
  })
})
