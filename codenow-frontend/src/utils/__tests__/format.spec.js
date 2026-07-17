import { describe, expect, it } from 'vitest'
import { formatDate, formatDateCell, formatDateOnly } from '../format'

describe('formatDate', () => {
  it('formats an ISO local date-time string', () => {
    expect(formatDate('2026-07-15T18:30:45')).toBe('2026-07-15 18:30:45')
  })

  it('adds missing seconds', () => {
    expect(formatDate('2026-07-15T18:30')).toBe('2026-07-15 18:30:00')
  })

  it('formats table cell values', () => {
    expect(formatDateCell({}, {}, '2026-07-15T18:30:45')).toBe('2026-07-15 18:30:45')
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
