import { describe, expect, it } from 'vitest'
import { categoryCascaderOptions, findCategory, flattenCategories } from './categoryTree'

const tree = [{ id: 1, name: 'Java', children: [{ id: 2, name: '集合', children: [] }] }]

describe('categoryTree', () => {
  it('flattens categories with full paths', () => {
    expect(flattenCategories(tree).map((item) => item.path)).toEqual(['Java', 'Java / 集合'])
  })

  it('finds nested categories', () => {
    expect(findCategory(tree, '2')?.name).toBe('集合')
  })

  it('builds cascader options and disables current category', () => {
    expect(categoryCascaderOptions(tree, 2)[0].children[0].disabled).toBe(true)
  })
})
