import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CommentForm from '../CommentForm.vue'
import { createComment } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const push = vi.fn()
vi.mock('vue-router', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    useRoute: () => ({ fullPath: '/blog/article/9' }),
    useRouter: () => ({ push }),
  }
})
vi.mock('@/api/comment', () => ({ createComment: vi.fn() }))
vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    success: vi.fn(),
  },
}))

describe('CommentForm', () => {
  let pinia

  beforeEach(() => {
    vi.clearAllMocks()
    pinia = createPinia()
    setActivePinia(pinia)
    const userStore = useUserStore()
    userStore.token = 'valid-token'
    userStore.userInfo = { id: 1, nickname: '测试用户', role: 'USER' }
  })

  function mountForm(props = { articleId: 9 }) {
    return mount(CommentForm, { props, global: { plugins: [pinia] } })
  }

  it('requires comment content', async () => {
    const wrapper = mountForm()
    await wrapper.find('.btn-submit').trigger('click')
    expect(ElMessage.warning).toHaveBeenCalledWith('请输入评论内容')
    expect(createComment).not.toHaveBeenCalled()
  })

  it('submits comment data from the logged-in user and emits success', async () => {
    createComment.mockResolvedValue({})
    const wrapper = mountForm({ articleId: 9, parentId: 3 })
    await wrapper.find('.form-textarea').setValue('  自动化评论  ')
    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(createComment).toHaveBeenCalledWith({
      articleId: 9,
      parentId: 3,
      content: '自动化评论',
    })
    expect(wrapper.emitted('success')).toHaveLength(1)
    expect(ElMessage.success).toHaveBeenCalledWith('评论成功')
  })

  it('prompts guests to log in', async () => {
    const userStore = useUserStore()
    userStore.token = ''
    const wrapper = mountForm()
    await wrapper.find('.login-required button').trigger('click')
    expect(push).toHaveBeenCalledWith({ path: '/login', query: { redirect: '/blog/article/9' } })
  })
})
