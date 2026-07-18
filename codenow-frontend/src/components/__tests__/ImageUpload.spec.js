import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ImageUpload from '../ImageUpload.vue'
import { uploadImage } from '@/api/upload'
import { ElMessage } from 'element-plus'

vi.mock('@/api/upload', () => ({ uploadImage: vi.fn() }))
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    success: vi.fn(),
  },
}))

const global = {
  stubs: {
    'el-icon': { template: '<span><slot /></span>' },
    'el-button': { template: '<button><slot /></button>' },
  },
}

function selectFile(wrapper, file) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', {
    configurable: true,
    value: [file],
  })
  return input.trigger('change')
}

describe('ImageUpload', () => {
  beforeEach(() => vi.clearAllMocks())

  it('rejects unsupported file types before upload', async () => {
    const wrapper = mount(ImageUpload, { global })

    await selectFile(wrapper, new File(['text'], 'payload.txt', { type: 'text/plain' }))

    expect(ElMessage.error).toHaveBeenCalledWith('仅支持 JPG、PNG、GIF、WebP 图片')
    expect(uploadImage).not.toHaveBeenCalled()
  })

  it('rejects files larger than 5MB', async () => {
    const wrapper = mount(ImageUpload, { global })
    const file = new File([new Uint8Array(5 * 1024 * 1024 + 1)], 'large.png', { type: 'image/png' })

    await selectFile(wrapper, file)

    expect(ElMessage.error).toHaveBeenCalledWith('图片大小不能超过 5MB')
    expect(uploadImage).not.toHaveBeenCalled()
  })

  it('uploads a valid image and emits its URL', async () => {
    uploadImage.mockResolvedValue({ data: { url: '/api/blog/files/2026/07/15/test.png' } })
    const wrapper = mount(ImageUpload, { global })

    await selectFile(wrapper, new File(['png'], 'cover.png', { type: 'image/png' }))
    await flushPromises()

    expect(uploadImage).toHaveBeenCalledOnce()
    expect(wrapper.emitted('update:modelValue')).toEqual([["/api/blog/files/2026/07/15/test.png"]])
    expect(ElMessage.success).toHaveBeenCalledWith('上传成功')
  })

  it('uses an injected author upload request instead of the admin endpoint', async () => {
    const uploadRequest = vi.fn().mockResolvedValue({
      data: { url: '/api/blog/files/2026/07/19/author.png' },
    })
    const wrapper = mount(ImageUpload, { props: { uploadRequest }, global })

    await selectFile(wrapper, new File(['png'], 'author.png', { type: 'image/png' }))
    await flushPromises()

    expect(uploadRequest).toHaveBeenCalledOnce()
    expect(uploadImage).not.toHaveBeenCalled()
    expect(wrapper.emitted('update:modelValue')).toEqual([["/api/blog/files/2026/07/19/author.png"]])
  })
})
