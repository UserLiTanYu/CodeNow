import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthorCommentList from './AuthorCommentList.vue'
import { deleteAuthorComment, getAuthorArticles, getAuthorComments } from '@/api/authorConsole'

const confirm = vi.hoisted(() => vi.fn())
vi.mock('@/api/authorConsole', () => ({
  deleteAuthorComment: vi.fn(),
  getAuthorArticles: vi.fn(),
  getAuthorComments: vi.fn(),
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn() },
  ElMessageBox: { confirm },
}))

const SlotStub = { template: '<div><slot /></div>' }
const global = {
  directives: { loading: () => {} },
  stubs: {
    ElAlert: true,
    ElButton: true,
    ElSelect: SlotStub,
    ElOption: true,
    ElTable: SlotStub,
    ElTableColumn: true,
    ElPagination: true,
  },
}

describe('AuthorCommentList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    confirm.mockResolvedValue()
    getAuthorComments.mockResolvedValue({
      data: { records: [{ id: 5, articleId: 9, articleTitle: '我的文章', content: '很好' }], total: 1 },
    })
    getAuthorArticles.mockResolvedValue({
      data: { records: [{ article: { id: 9, title: '我的文章' } }], total: 1 },
    })
  })

  it('loads ownership-scoped comments and author article filter options', async () => {
    const wrapper = mount(AuthorCommentList, { global })
    await flushPromises()

    expect(getAuthorComments).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10 })
    expect(getAuthorArticles).toHaveBeenCalledWith({ pageNum: 1, pageSize: 100 })
    expect(wrapper.vm.comments).toHaveLength(1)
    expect(wrapper.vm.articles).toHaveLength(1)
  })

  it('filters comments by selected author article', async () => {
    const wrapper = mount(AuthorCommentList, { global })
    await flushPromises()
    getAuthorComments.mockClear()

    wrapper.vm.filterArticleId = 9
    wrapper.vm.handleFilterChange()
    await flushPromises()

    expect(getAuthorComments).toHaveBeenCalledWith({ pageNum: 1, pageSize: 10, articleId: 9 })
  })

  it('deletes a comment through the author endpoint and refreshes the page', async () => {
    const wrapper = mount(AuthorCommentList, { global })
    await flushPromises()
    getAuthorComments.mockClear()

    await wrapper.vm.handleDelete(5)

    expect(deleteAuthorComment).toHaveBeenCalledWith(5)
    expect(getAuthorComments).toHaveBeenCalled()
  })
})
