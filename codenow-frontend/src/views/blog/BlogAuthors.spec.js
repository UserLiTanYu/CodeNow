import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import BlogAuthors from './BlogAuthors.vue'
import { getPublicAuthors } from '@/api/blog'

const push = vi.fn()
const route = { query: {} }

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ push }),
}))
vi.mock('@/api/blog', () => ({ getPublicAuthors: vi.fn() }))

const RouterLinkStub = { props: ['to'], template: '<a :href="typeof to === `string` ? to : to.path"><slot /></a>' }
const ElPaginationStub = { template: '<nav class="pagination-stub" />' }

function author(overrides = {}) {
  return {
    userId: 7,
    displayName: '林舟',
    avatar: '/avatar.png',
    bio: '专注 Java 后端与工程实践',
    expertise: ['Java', 'Spring Boot'],
    articleCount: 12,
    totalViews: 3200,
    email: 'private@example.com',
    username: 'private-login',
    ...overrides,
  }
}

describe('BlogAuthors', () => {
  beforeEach(() => {
    route.query = {}
    push.mockReset()
    getPublicAuthors.mockReset()
    getPublicAuthors.mockResolvedValue({ data: { records: [author()], total: 1 } })
  })

  it('loads discoverable authors and renders only the public card projection', async () => {
    const wrapper = mount(BlogAuthors, {
      global: { stubs: { RouterLink: RouterLinkStub, ElPagination: ElPaginationStub } },
    })
    await flushPromises()

    expect(getPublicAuthors).toHaveBeenCalledWith({ pageNum: 1, pageSize: 12, sort: 'popular' })
    expect(wrapper.text()).toContain('林舟')
    expect(wrapper.text()).toContain('Java')
    expect(wrapper.text()).toContain('12 篇文章')
    expect(wrapper.find('a[href="/blog/author/7"]').exists()).toBe(true)
    expect(wrapper.text()).not.toContain('private@example.com')
    expect(wrapper.text()).not.toContain('private-login')
  })

  it('submits a trimmed author keyword through the public route query', async () => {
    const wrapper = mount(BlogAuthors, {
      global: { stubs: { RouterLink: RouterLinkStub, ElPagination: ElPaginationStub } },
    })
    await flushPromises()
    await wrapper.get('input[aria-label="搜索作者或擅长领域"]').setValue('  Java  ')
    await wrapper.get('form[role="search"]').trigger('submit')

    expect(push).toHaveBeenCalledWith({ path: '/blog/authors', query: { keyword: 'Java' } })
  })
})
