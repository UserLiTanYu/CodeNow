import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import BlogAuthor from './BlogAuthor.vue'
import { getPublicAuthor, getPublicAuthorArticles } from '@/api/blog'

const route = { params: { id: '7' } }
vi.mock('vue-router', () => ({ useRoute: () => route }))
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

describe('BlogAuthor', () => {
  beforeEach(() => {
    route.params.id = '7'
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

  it('renders the public profile, safe external links and published articles', async () => {
    const wrapper = mount(BlogAuthor, {
      global: { stubs: { BlogArticleCard: BlogArticleCardStub, ElPagination: ElPaginationStub, RouterLink: RouterLinkStub } },
    })
    await flushPromises()

    expect(getPublicAuthor).toHaveBeenCalledWith('7')
    expect(getPublicAuthorArticles).toHaveBeenCalledWith('7', { pageNum: 1, pageSize: 10, sort: 'latest' })
    expect(wrapper.text()).toContain('林舟')
    expect(wrapper.text()).toContain('2篇文章')
    expect(wrapper.text()).toContain('500次阅读')
    expect(wrapper.text()).toContain('Spring Boot 实践')
    const website = wrapper.get('a[href="https://example.com"]')
    expect(website.attributes('target')).toBe('_blank')
    expect(website.attributes('rel')).toContain('noopener')
  })

  it('shows a non-enumerating unavailable state when the profile cannot be loaded', async () => {
    getPublicAuthor.mockRejectedValueOnce(new Error('404'))

    const wrapper = mount(BlogAuthor, {
      global: { stubs: { BlogArticleCard: BlogArticleCardStub, ElPagination: ElPaginationStub, RouterLink: RouterLinkStub } },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('作者主页不可用')
    expect(getPublicAuthorArticles).not.toHaveBeenCalled()
  })
})
