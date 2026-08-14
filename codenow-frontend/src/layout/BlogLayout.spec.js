import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BlogLayout from './BlogLayout.vue'
import { getBlogArticles, getBlogCategories, getBlogTags, getHotArticles, getPublicAuthor, getPublicAuthorArticles, getPublicAuthorCategories, getPublicAuthorTags } from '@/api/blog'
import { currentArticleAuthorId } from '@/utils/blogArticleAuthor'

const route = {
  path: '/blog/author/7',
  fullPath: '/blog/author/7',
  params: { id: '7' },
  query: {},
}
const router = { push: vi.fn() }

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => router,
}))
vi.mock('@/stores/user', () => ({
  useUserStore: () => ({
    isLoggedIn: false,
    isAdmin: false,
    canEnterAuthorConsole: false,
    token: '',
    userInfo: null,
  }),
}))
vi.mock('@/api/member', () => ({ getUnreadNotificationCount: vi.fn() }))
vi.mock('@/api/blog', () => ({
  getBlogArticles: vi.fn(),
  getBlogCategories: vi.fn(),
  getBlogTags: vi.fn(),
  getHotArticles: vi.fn(),
  getSiteProfile: vi.fn(),
  getPublicAuthor: vi.fn(),
  getPublicAuthorCategories: vi.fn(),
  getPublicAuthorTags: vi.fn(),
  getPublicAuthorArticles: vi.fn(),
}))
vi.mock('element-plus', () => ({ ElMessage: { success: vi.fn() } }))

const RouterLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
}

function mountLayout() {
  return mount(BlogLayout, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
        RouterView: true,
        ElInput: true,
        ElDropdown: true,
        ElDropdownMenu: true,
        ElDropdownItem: true,
        ElIcon: { template: '<i><slot /></i>' },
      },
    },
  })
}

describe('BlogLayout author category navigation', () => {
  beforeEach(() => {
    route.path = '/blog/author/7'
    route.fullPath = '/blog/author/7'
    route.params = { id: '7' }
    route.query = {}
    currentArticleAuthorId.value = null
    vi.clearAllMocks()
    getPublicAuthor.mockResolvedValue({ data: { bio: '专注 Spring Boot 与工程实践' } })
    getPublicAuthorCategories.mockResolvedValue({ data: [{ id: 2, name: '作者 Spring', children: [] }] })
    getPublicAuthorTags.mockResolvedValue({ data: [{ id: 9, name: 'Spring 标签' }] })
    getPublicAuthorArticles.mockResolvedValue({ data: { records: [] } })
  })

  it('keeps top category navigation inside the current author page', async () => {
    const wrapper = mountLayout()
    await flushPromises()

    const link = wrapper.findAllComponents(RouterLinkStub).find(item => item.text() === '作者 Spring')
    expect(link.props('to')).toEqual({ path: '/blog/author/7', query: { categoryId: 2 } })
    expect(wrapper.text()).toContain('个人简介')
    expect(wrapper.text()).toContain('专注 Spring Boot 与工程实践')
  })

  it('keeps tag navigation inside the current author page', async () => {
    const wrapper = mountLayout()
    await flushPromises()

    const link = wrapper.findAllComponents(RouterLinkStub).find(item => item.text() === 'Spring 标签')
    expect(link.props('to')).toEqual({ path: '/blog/author/7', query: { tagId: 9 } })
  })

  it('expands articles under a root category that has no children', async () => {
    getBlogArticles.mockResolvedValue({ data: { records: [{ article: { id: 295, title: '中科大在读物理学博士在线找兼职' } }] } })
    const wrapper = mountLayout()
    await flushPromises()

    await wrapper.get('button[aria-label="展开作者 Spring的文章"]').trigger('click')
    await flushPromises()

    expect(getBlogArticles).toHaveBeenCalledWith({
      pageNum: 1,
      pageSize: 50,
      categoryId: 2,
      sort: 'learning',
      authorId: '7',
    })
    const articleLink = wrapper.findAllComponents(RouterLinkStub)
      .find(item => item.text() === '中科大在读物理学博士在线找兼职')
    expect(articleLink.props('to')).toBe('/blog/article/295')
  })

  it('does not request author sidebar data after the public profile is unavailable', async () => {
    getPublicAuthor.mockRejectedValueOnce(new Error('作者不存在'))

    mountLayout()
    await flushPromises()

    expect(getPublicAuthor).toHaveBeenCalledWith('7', { silentError: true })
    expect(getPublicAuthorCategories).not.toHaveBeenCalled()
    expect(getPublicAuthorTags).not.toHaveBeenCalled()
    expect(getPublicAuthorArticles).not.toHaveBeenCalled()
  })

  it('uses a full-width content layout on the author plaza', async () => {
    route.path = '/blog/authors'
    route.fullPath = '/blog/authors'
    route.params = {}

    const wrapper = mountLayout()
    await flushPromises()

    expect(wrapper.get('.blog-body').classes()).toContain('sidebarless-layout')
    expect(wrapper.find('.blog-category-sidebar').exists()).toBe(false)
    expect(wrapper.find('.blog-sidebar').exists()).toBe(false)
  })

  it('links both desktop and mobile navigation to the standalone about page', async () => {
    route.path = '/blog/about'
    route.fullPath = '/blog/about'
    route.params = {}

    const wrapper = mountLayout()
    await flushPromises()
    await wrapper.get('.menu-trigger').trigger('click')

    const links = wrapper.findAllComponents(RouterLinkStub)
      .filter(item => item.text() === '关于本站')
    expect(links).toHaveLength(2)
    expect(links.every(item => item.props('to') === '/blog/about')).toBe(true)
    expect(wrapper.get('.blog-body').classes()).toContain('sidebarless-layout')
    expect(wrapper.find('.blog-category-sidebar').exists()).toBe(false)
    expect(wrapper.find('.blog-sidebar').exists()).toBe(false)
  })

  it('hides article sidebars on member pages', async () => {
    route.path = '/blog/profile'
    route.fullPath = '/blog/profile'
    route.params = {}

    const wrapper = mountLayout()
    await flushPromises()

    expect(wrapper.get('.blog-body').classes()).toContain('sidebarless-layout')
    expect(wrapper.find('.blog-category-sidebar').exists()).toBe(false)
    expect(wrapper.find('.blog-sidebar').exists()).toBe(false)
  })
})

describe('BlogLayout article page author scope', () => {
  beforeEach(() => {
    route.path = '/blog/article/99'
    route.fullPath = '/blog/article/99'
    route.params = { id: '99' }
    route.query = {}
    currentArticleAuthorId.value = null
    vi.clearAllMocks()
    getPublicAuthor.mockResolvedValue({ data: { bio: '专注 Spring Boot 与工程实践' } })
    getPublicAuthorCategories.mockResolvedValue({ data: [{ id: 2, name: '作者 Spring', children: [] }] })
    getPublicAuthorTags.mockResolvedValue({ data: [{ id: 9, name: 'Spring 标签' }] })
    getPublicAuthorArticles.mockResolvedValue({ data: { records: [] } })
  })

  it('scopes sidebar categories and hot articles to the article author', async () => {
    currentArticleAuthorId.value = 7
    const wrapper = mountLayout()
    await flushPromises()

    expect(getPublicAuthor).toHaveBeenCalledWith(7, { silentError: true })
    expect(getPublicAuthorCategories).toHaveBeenCalledWith(7)
    expect(getPublicAuthorTags).toHaveBeenCalledWith(7)
    expect(getPublicAuthorArticles).toHaveBeenCalledWith(7, { pageNum: 1, pageSize: 3, sort: 'mostViewed' })
    expect(getBlogCategories).not.toHaveBeenCalled()

    const allLink = wrapper.findAllComponents(RouterLinkStub).find(item => item.text().includes('全部文章'))
    expect(allLink.props('to')).toBe('/blog/author/7')
    const categoryLink = wrapper.findAllComponents(RouterLinkStub).find(item => item.text() === '作者 Spring')
    expect(categoryLink.props('to')).toEqual({ path: '/blog/author/7', query: { categoryId: 2 } })
    const tagLink = wrapper.findAllComponents(RouterLinkStub).find(item => item.text() === 'Spring 标签')
    expect(tagLink.props('to')).toEqual({ path: '/blog/author/7', query: { tagId: 9 } })
    expect(wrapper.text()).toContain('作者热门文章')
    expect(wrapper.text()).toContain('个人简介')
  })

  it('does not load global sidebar data before the article author is resolved', async () => {
    mountLayout()
    await flushPromises()

    expect(getBlogCategories).not.toHaveBeenCalled()
    expect(getBlogTags).not.toHaveBeenCalled()
    expect(getHotArticles).not.toHaveBeenCalled()
    expect(getPublicAuthorCategories).not.toHaveBeenCalled()
  })
})
