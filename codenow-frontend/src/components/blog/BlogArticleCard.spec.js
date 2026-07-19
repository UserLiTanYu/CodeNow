import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BlogArticleCard from './BlogArticleCard.vue'

const RouterLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>',
}

describe('BlogArticleCard author discovery', () => {
  it('links the public author summary without exposing account fields', () => {
    const wrapper = mount(BlogArticleCard, {
      props: {
        item: {
          article: { id: 21, title: 'Java 实践', summary: '摘要', authorId: 7, viewCount: 10 },
          tags: [],
          author: {
            userId: 7,
            displayName: '林舟',
            avatar: '/avatar.png',
            email: 'private@example.com',
            username: 'private-login',
          },
        },
      },
      global: {
        stubs: {
          RouterLink: RouterLinkStub,
          ElIcon: { template: '<i><slot /></i>' },
          ElTag: { template: '<span><slot /></span>' },
        },
      },
    })

    expect(wrapper.find('a[href="/blog/author/7"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('林舟')
    expect(wrapper.text()).not.toContain('private@example.com')
    expect(wrapper.text()).not.toContain('private-login')
  })
})
