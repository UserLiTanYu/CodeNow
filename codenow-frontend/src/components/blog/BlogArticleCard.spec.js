import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import BlogArticleCard from './BlogArticleCard.vue'

const RouterLinkStub = {
  name: 'RouterLink',
  props: ['to'],
  template: '<a><slot /></a>',
}

function mountCard(props) {
  return mount(BlogArticleCard, {
    props,
    global: {
      stubs: {
        RouterLink: RouterLinkStub,
        ElIcon: { template: '<i><slot /></i>' },
        ElTag: { template: '<span><slot /></span>' },
      },
    },
  })
}

describe('BlogArticleCard', () => {
  it('does not repeat the author name or avatar in article lists', () => {
    const wrapper = mountCard({
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
    })

    expect(wrapper.text()).not.toContain('林舟')
    expect(wrapper.find('img').exists()).toBe(false)
    expect(wrapper.text()).not.toContain('private@example.com')
    expect(wrapper.text()).not.toContain('private-login')
  })

  it('keeps category and tag links in the author context when requested', () => {
    const wrapper = mountCard({
      contextAuthorId: 7,
      item: {
        article: { id: 21, title: 'Java 实践', summary: '摘要', categoryId: 2 },
        categoryName: '作者 Spring',
        tags: [{ id: 3, name: '作者标签' }],
        author: { userId: 7, displayName: '林舟' },
      },
    })

    expect(wrapper.getComponent('.category-link').props('to')).toEqual({
      path: '/blog/author/7',
      query: { categoryId: 2 },
    })
    expect(wrapper.getComponent('.tag-link').props('to')).toEqual({
      path: '/blog/author/7',
      query: { tagId: 3 },
    })
  })
})
