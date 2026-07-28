import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import BlogAuthor from './BlogAuthor.vue'
import { getPublicAuthor, getPublicAuthorArticles } from '@/api/blog'

const route = { path: '/blog/author/7', params: { id: '7' }, query: {} }
const router = { push: vi.fn() }
vi.mock('vue-router', () => ({ useRoute: () => route, useRouter: () => router }))
vi.mock('@/api/blog', () => ({
  getPublicAuthor: vi.fn(),
  getPublicAuthorArticles: vi.fn(),
}))

const BlogArticleCardStub = {
  props: ['item'],
  template: '<article class="article-stub">{{ item.article.title }}</article>',
}
const ElPaginationStub = { template: '<nav class="pagination-stub" />' }
const RouterLinkStub = { props: ['to'], template: '<a :href="to"><slot /></a>' }
const ElSkeletonStub = { template: '<div />' }
const ElEmptyStub = { template: '<div />' }

function mountAuthor() {
  return mount(BlogAuthor, {
    global: { stubs: { BlogArticleCard: BlogArticleCardStub, ElPagination: ElPaginationStub, RouterLink: RouterLinkStub, ElSkeleton: ElSkeletonStub, ElEmpty: ElEmptyStub } },
  })
}

describe('BlogAuthor', () => {
  beforeEach(() => {
    route.params.id = '7'
    route.path = '/blog/author/7'
    route.query = {}
    vi.clearAllMocks()
    getPublicAuthor.mockResolvedValue({ data: {
      userId: 7,
      displayName: '林舟',
      avatar: '/avatar.png',
      bio: '专注 Java 后端与工程实践',
      expertise: ['Java', 'Spring Boot'],
      websiteUrl: 'https://example.com',
      portfolioUrl: 'https://example.com/works',
      articleCount: 2,
      totalViews: 500,
    } })
    getPublicAuthorArticles.mockResolvedValue({ data: {
      records: [{ article: { id: 21, title: 'Spring Boot 实践' }, tags: [] }],
      total: 1,
    } })
  })

  it('renders the current-author toolbar and published articles', async () => {
    const wrapper = mountAuthor()
    await flushPromises()

    expect(getPublicAuthor).toHaveBeenCalledWith('7')
    expect(getPublicAuthorArticles).toHaveBeenCalledWith('7', { pageNum: 1, pageSize: 10, sort: 'latest' })
    expect(wrapper.text()).toContain('林舟')
    expect(wrapper.text()).toContain('当前作者')
    expect(wrapper.text()).toContain('1篇')
    expect(wrapper.text()).toContain('Spring Boot 实践')
    expect(wrapper.get('input[type="search"]').attributes('placeholder')).toBe('搜索当前作者的文章')
  })

  it('shows a non-enumerating unavailable state when the profile cannot be loaded', async () => {
    getPublicAuthor.mockRejectedValueOnce(new Error('404'))

    const wrapper = mountAuthor()
    await flushPromises()

    expect(wrapper.text()).toContain('作者主页不可用')
    expect(getPublicAuthorArticles).not.toHaveBeenCalled()
  })

  it('loads only the current author articles when an author category query is selected', async () => {
    route.query = { categoryId: '2' }

    mountAuthor()
    await flushPromises()

    expect(getPublicAuthorArticles).toHaveBeenCalledWith('7', {
      pageNum: 1,
      pageSize: 10,
      sort: 'latest',
      categoryId: '2',
    })
  })

  it('searches within the current author while preserving active filters', async () => {
    route.query = { categoryId: '2' }
    const wrapper = mountAuthor()
    await flushPromises()

    await wrapper.get('input[type="search"]').setValue('  Spring  ')
    await wrapper.get('form[role="search"]').trigger('submit')

    expect(router.push).toHaveBeenCalledWith({
      path: '/blog/author/7',
      query: { keyword: 'Spring', categoryId: '2' },
    })
  })
})
