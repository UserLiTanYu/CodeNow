import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ArticleEditor from './ArticleEditor.vue'

let route
let leaveGuard
const mocks = vi.hoisted(() => ({ confirm: vi.fn() }))

vi.mock('vue-router', () => ({
  useRoute: () => route,
  useRouter: () => ({ back: vi.fn(), push: vi.fn() }),
  onBeforeRouteLeave: (guard) => { leaveGuard = guard },
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn(), warning: vi.fn(), error: vi.fn() },
  ElMessageBox: { confirm: mocks.confirm },
}))
vi.mock('md-editor-v3', () => ({ MdEditor: { name: 'MdEditor', template: '<div />' } }))
vi.mock('md-editor-v3/lib/style.css', () => ({}))
vi.mock('@/components/ImageUpload.vue', () => ({
  default: { name: 'ImageUpload', props: ['uploadRequest'], template: '<div />' },
}))
vi.mock('@/api/upload', () => ({ importArticlePackage: vi.fn() }))

const ElForm = { name: 'ElForm', props: ['disabled'], template: '<form><slot /></form>' }
const SlotStub = { template: '<div><slot /></div>' }

function deferred() {
  let resolve
  const promise = new Promise((resolvePromise) => { resolve = resolvePromise })
  return { promise, resolve }
}

function mountEditor(overrides = {}) {
  return mount(ArticleEditor, {
    props: {
      articleApi: { get: vi.fn(), create: vi.fn(), update: vi.fn() },
      loadCategories: vi.fn().mockResolvedValue({ data: [] }),
      loadTags: vi.fn().mockResolvedValue({ data: [] }),
      redirectPath: '/author-console/articles',
      ...overrides,
    },
    global: {
      components: { ElForm },
      directives: { loading: () => {} },
      stubs: {
        ElFormItem: SlotStub,
        ElInput: true,
        ElCascader: true,
        ElSelect: true,
        ElOption: true,
        ElInputNumber: true,
        ElButton: true,
        ElIcon: true,
        ElAlert: true,
        ElDialog: SlotStub,
      },
    },
  })
}

describe('ArticleEditor initialization', () => {
  beforeEach(() => {
    route = { params: {} }
    leaveGuard = undefined
    mocks.confirm.mockReset().mockResolvedValue()
  })

  it('disables a new article form until metadata initialization settles', async () => {
    const categories = deferred()
    const tags = deferred()
    const wrapper = mountEditor({
      loadCategories: () => categories.promise,
      loadTags: () => tags.promise,
    })

    expect(wrapper.findComponent(ElForm).props('disabled')).toBe(true)
    categories.resolve({ data: [] })
    tags.resolve({ data: [] })
    await flushPromises()
    expect(wrapper.findComponent(ElForm).props('disabled')).toBe(false)
  })

  it('does not absorb initialization-time edits into the clean baseline', async () => {
    const categories = deferred()
    const tags = deferred()
    const wrapper = mountEditor({
      loadCategories: () => categories.promise,
      loadTags: () => tags.promise,
    })
    wrapper.vm.form.title = 'unsaved draft'
    categories.resolve({ data: [] })
    tags.resolve({ data: [] })
    await flushPromises()

    await leaveGuard()
    expect(mocks.confirm).toHaveBeenCalled()
  })

  it('enables author cover and inline image upload without enabling ZIP package import', async () => {
    const uploadImageRequest = vi.fn()
    const wrapper = mountEditor({ imageTools: true, uploadImageRequest })
    await flushPromises()

    const uploads = wrapper.findAllComponents({ name: 'ImageUpload' })
    expect(uploads).toHaveLength(2)
    expect(uploads.every((upload) => upload.props('uploadRequest') === uploadImageRequest)).toBe(true)
    expect(wrapper.find('input[accept*="application/zip"]').exists()).toBe(false)
  })
})
