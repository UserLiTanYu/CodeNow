import { flushPromises, shallowMount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthorCategoryList from './AuthorCategoryList.vue'
import { getAuthorCategories } from '@/api/authorConsole'

const ElTable = { template: '<div><slot /></div>' }
const ElTableColumn = { template: '<div />' }

vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))
vi.mock('@/api/authorConsole', () => ({
  getAuthorCategories: vi.fn(),
  createAuthorCategory: vi.fn(),
  updateAuthorCategory: vi.fn(),
  deleteAuthorCategory: vi.fn(),
}))

describe('AuthorCategoryList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAuthorCategories.mockResolvedValue({ data: [{ id: 1, name: 'Java', children: [] }] })
  })

  it('loads the current author category tree on mount', async () => {
    const wrapper = shallowMount(AuthorCategoryList, {
      global: {
        directives: { loading: () => {} },
        components: { ElTable, ElTableColumn },
        stubs: {
          ElButton: true, ElCascader: true, ElDialog: true, ElForm: true,
          ElFormItem: true, ElInput: true, ElInputNumber: true,
        },
      },
    })
    await flushPromises()

    expect(getAuthorCategories).toHaveBeenCalledOnce()
    expect(wrapper.vm.categories).toEqual([{ id: 1, name: 'Java', children: [] }])
  })
})
