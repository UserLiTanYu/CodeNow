import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CommentForm from '../CommentForm.vue'
import { createComment } from '@/api/comment'
import { ElMessage } from 'element-plus'

vi.mock('@/api/comment', () => ({ createComment: vi.fn() }))
vi.mock('element-plus', () => ({
  ElMessage: {
    warning: vi.fn(),
    success: vi.fn(),
  },
}))

describe('CommentForm', () => {
  beforeEach(() => vi.clearAllMocks())

  it('requires a nickname and content', async () => {
    const wrapper = mount(CommentForm, { props: { articleId: 9 } })

    await wrapper.find('.btn-submit').trigger('click')

    expect(ElMessage.warning).toHaveBeenCalledWith('请输入昵称')
    expect(createComment).not.toHaveBeenCalled()
  })

  it('submits normalized comment data and emits success', async () => {
    createComment.mockResolvedValue({})
    const wrapper = mount(CommentForm, { props: { articleId: 9, parentId: 3 } })
    const inputs = wrapper.findAll('.form-input')
    await inputs[0].setValue('  测试用户  ')
    await inputs[1].setValue('test@example.com')
    await wrapper.find('.form-textarea').setValue('  自动化评论  ')

    await wrapper.find('.btn-submit').trigger('click')
    await flushPromises()

    expect(createComment).toHaveBeenCalledWith({
      articleId: 9,
      parentId: 3,
      content: '自动化评论',
      nickname: '测试用户',
      email: 'test@example.com',
    })
    expect(wrapper.emitted('success')).toHaveLength(1)
    expect(ElMessage.success).toHaveBeenCalledWith('评论成功')
  })
})
