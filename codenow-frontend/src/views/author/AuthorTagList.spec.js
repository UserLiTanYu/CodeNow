import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthorTagList from './AuthorTagList.vue'
import { getAuthorTags } from '@/api/authorConsole'

const ElTable = { template: '<div><slot /></div>' }
const ElTableColumn = { template: '<div />' }

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))
vi.mock('@/api/authorConsole', () => ({
  getAuthorTags: vi.fn(),
  createAuthorTag: vi.fn(),
  updateAuthorTag: vi.fn(),
  deleteAuthorTag: vi.fn(),
}))

describe('AuthorTagList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAuthorTags.mockResolvedValue({ data: [{ id: 2, name: 'Spring Boot' }] })
  })

  it('loads only the current author tags on mount', async () => {
    const wrapper = shallowMount(AuthorTagList, {
      global: {
        directives: { loading: () => {} },
        components: { ElTable, ElTableColumn },
        stubs: {
          ElButton: true, ElDialog: true, ElForm: true,
          ElFormItem: true, ElInput: true,
        },
      },
    })
    await flushPromises()

    expect(getAuthorTags).toHaveBeenCalledOnce()
    expect(wrapper.vm.tags).toEqual([{ id: 2, name: 'Spring Boot' }])
  })
})
