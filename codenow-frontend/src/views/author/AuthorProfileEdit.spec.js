import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthorProfileEdit from './AuthorProfileEdit.vue'
import { getAuthorProfile } from '@/api/authorConsole'

vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn() } }))
vi.mock('@/api/authorConsole', () => ({
  getAuthorProfile: vi.fn(),
  updateAuthorProfile: vi.fn(),
}))

describe('AuthorProfileEdit', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAuthorProfile.mockResolvedValue({ data: {
      bio: '专注 Java 后端工程实践与性能优化。',
      expertise: 'Java,Spring Boot',
      websiteUrl: 'https://example.com',
      portfolioUrl: null,
    } })
  })

  it('loads the current author profile on mount', async () => {
    const wrapper = shallowMount(AuthorProfileEdit, {
      global: {
        directives: { loading: () => {} },
        components: {
          ElCard: { template: '<div><slot name="header"/><slot/></div>' },
        },
        stubs: {
          ElButton: true, ElForm: true, ElFormItem: true,
          ElInput: true, ElOption: true, ElSelect: true,
        },
      },
    })
    await flushPromises()

    expect(getAuthorProfile).toHaveBeenCalledOnce()
    expect(wrapper.exists()).toBe(true)
  })
})
