import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BlogLayout from './BlogLayout.vue'
import { getPublicAuthor, getPublicAuthorArticles, getPublicAuthorCategories, getPublicAuthorTags } from '@/api/blog'

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
