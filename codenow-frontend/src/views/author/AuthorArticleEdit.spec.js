import { shallowMount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AuthorArticleEdit from './AuthorArticleEdit.vue'
import { getAuthorCategories, getAuthorTags } from '@/api/authorConsole'

vi.mock('@/api/authorConsole', () => ({
  createAuthorArticle: vi.fn(),
  getAuthorArticle: vi.fn(),
  getAuthorCategories: vi.fn(),
  getAuthorTags: vi.fn(),
  updateAuthorArticle: vi.fn(),
  uploadAuthorImage: vi.fn(),
}))
vi.mock('@/api/blog', () => ({ getBlogCategories: vi.fn() }))

const ArticleEditor = {
  name: 'ArticleEditor',
  props: ['loadCategories', 'loadTags'],
  template: '<div />',
}

describe('AuthorArticleEdit', () => {
  it('loads author-scoped categories and tags', () => {
    const wrapper = shallowMount(AuthorArticleEdit, {
      global: { stubs: { ArticleEditor } },
    })
    const editor = wrapper.findComponent(ArticleEditor)

    expect(editor.props('loadCategories')).toBe(getAuthorCategories)
    expect(editor.props('loadTags')).toBe(getAuthorTags)
  })
})
