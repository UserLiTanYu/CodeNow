import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import BlogAbout from './BlogAbout.vue'
import { getSiteProfile, getSiteStats } from '@/api/blog'

vi.mock('@/api/blog', () => ({
  getSiteProfile: vi.fn(),
  getSiteStats: vi.fn(),
}))

const RouterLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
}

function mountAbout() {
  return mount(BlogAbout, {
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
      },
    },
  })
}

describe('BlogAbout', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getSiteStats.mockResolvedValue({ data: {} })
  })

  it('loads and displays the managed public site profile', async () => {
    getSiteProfile.mockResolvedValue({
      data: {
        siteName: '开发手记',
        slogan: '让经验可以复用',
        description: '面向开发者的知识分享站点。',
        aboutContent: '## 我们是谁\n\n专注于可靠、系统的技术内容。',
        contactEmail: 'hello@example.com',
        githubUrl: 'https://github.com/example',
        foundedAt: '2024-05-01',
      },
    })

    const wrapper = mountAbout()
    expect(wrapper.text()).toContain('正在加载站点简介...')
    await flushPromises()

    expect(getSiteProfile).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('关于开发手记')
    expect(wrapper.text()).toContain('让经验可以复用')
    expect(wrapper.text()).toContain('专注于可靠、系统的技术内容。')
    expect(wrapper.text()).toContain('自 2024 年开始')
    expect(wrapper.get('a[href="mailto:hello@example.com"]').exists()).toBe(true)
  })

  it('shows fallback copy when the public profile is unavailable', async () => {
    getSiteProfile.mockRejectedValue(new Error('network error'))

    const wrapper = mountAbout()
    await flushPromises()

    expect(wrapper.text()).toContain('在这里，我们整理可靠的技术知识')
  })

  it('displays public stats and falls back to zero when needed', async () => {
    getSiteProfile.mockResolvedValue({ data: {} })
    getSiteStats.mockResolvedValue({
      data: { articleCount: 150, authorCount: 8, categoryCount: 12, totalViews: 32860 },
    })

    const wrapper = mountAbout()
    await flushPromises()

    expect(wrapper.text()).toContain('150')
    expect(wrapper.text()).toContain('8')
    expect(wrapper.text()).toContain('12')
    expect(wrapper.text()).toContain('32,860')
  })

  it('sanitizes managed Markdown before rendering it', async () => {
    getSiteProfile.mockResolvedValue({
      data: { aboutContent: '安全内容<script>alert("xss")</script>' },
    })

    const wrapper = mountAbout()
    await flushPromises()

    expect(wrapper.find('.about-content').html()).not.toContain('<script>')
    expect(wrapper.text()).toContain('安全内容')
  })

  it('provides article, author, application and policy entries', async () => {
    getSiteProfile.mockResolvedValue({ data: {} })

    const wrapper = mountAbout()
    await flushPromises()

    const targets = wrapper.findAllComponents(RouterLinkStub).map(link => link.props('to'))
    expect(targets).toEqual([
      '/blog',
      '/blog/authors',
      '/blog/author-application',
      '/blog/terms',
      '/blog/privacy',
    ])
  })
})
