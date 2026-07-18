import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import AuthorArticleList from './AuthorArticleList.vue'
import { getAuthorArticles, getAuthorTags } from '@/api/authorConsole'
import { getBlogCategories } from '@/api/blog'

vi.mock('vue-router', () => ({ useRouter: () => ({ push: vi.fn() }) }))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn() },
  ElMessageBox: { confirm: vi.fn() },
}))
vi.mock('@/api/authorConsole', () => ({
  getAuthorArticles: vi.fn(),
  getAuthorTags: vi.fn(),
  deleteAuthorArticle: vi.fn(),
  toggleAuthorArticleStatus: vi.fn(),
}))
vi.mock('@/api/blog', () => ({
  getBlogCategories: vi.fn(),
}))

const ElTable = { name: 'ElTable', props: ['data'], template: '<div><slot /></div>' }
const ElTableColumn = {
  name: 'ElTableColumn',
  props: ['label', 'width', 'minWidth', 'fixed'],
  template: '<div />',
}
const ElAlert = { name: 'ElAlert', props: ['title'], template: '<div>{{ title }}</div>' }
const ElButton = { name: 'ElButton', template: '<button><slot /></button>' }

function deferred() {
  let resolve
  let reject
  const promise = new Promise((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })
  return { promise, resolve, reject }
}

function mountList() {
  return mount(AuthorArticleList, {
    global: {
      components: { ElTable, ElTableColumn, ElAlert, ElButton },
      directives: { loading: () => {} },
      stubs: {

        ElCascader: true,
        ElSelect: true,
        ElOption: true,
        ElPagination: true,
        ElTag: true,
      },
    },
  })
}

describe('AuthorArticleList', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAuthorArticles.mockResolvedValue({ data: { records: [], total: 0 } })
    getBlogCategories.mockResolvedValue({ data: [] })
    getAuthorTags.mockResolvedValue({ data: [] })
  })

  it('keeps desktop table columns within the author content width budget', async () => {
    const wrapper = mountList()
    await flushPromises()
    const columns = wrapper.findAllComponents(ElTableColumn)
    const allocatedWidth = columns.reduce(
      (sum, column) => sum + Number(column.props('width') || column.props('minWidth') || 0),
      0,
    )

    expect(allocatedWidth).toBeLessThanOrEqual(920)
    expect(columns.at(-1).props('fixed')).toBe('right')
  })

  it('does not expose an author top action', () => {
    expect(mountList().text()).not.toContain('置顶')
  })

  it('loads articles even when optional filter metadata fails', async () => {
    getBlogCategories.mockRejectedValueOnce(new Error('metadata unavailable'))
    mountList()
    await flushPromises()

    expect(getAuthorArticles).toHaveBeenCalled()
  })

  it('keeps the newest article response when requests finish out of order', async () => {
    const older = deferred()
    const newer = deferred()
    getAuthorArticles.mockImplementationOnce(() => older.promise).mockImplementationOnce(() => newer.promise)
    const wrapper = mountList()
    const newestRequest = wrapper.vm.loadArticles()
    newer.resolve({ data: { records: [{ article: { id: 2, title: 'newer' }, tags: [] }], total: 1 } })
    await newestRequest
    older.resolve({ data: { records: [{ article: { id: 1, title: 'older' }, tags: [] }], total: 1 } })
    await flushPromises()

    expect(wrapper.findComponent(ElTable).props('data')[0].article.title).toBe('newer')
  })

  it('shows a retryable error when the article request fails', async () => {
    getAuthorArticles.mockRejectedValueOnce(new Error('network unavailable'))
    const wrapper = mountList()
    await flushPromises()

    expect(wrapper.text()).toContain('文章加载失败')
    expect(wrapper.text()).toContain('重新加载')
  })
})
