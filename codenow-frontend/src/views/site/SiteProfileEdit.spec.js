import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import SiteProfileEdit from './SiteProfileEdit.vue'
import { getAdminSiteProfile, updateAdminSiteProfile } from '@/api/siteProfile'

vi.mock('@/api/siteProfile', () => ({
  getAdminSiteProfile: vi.fn(),
  updateAdminSiteProfile: vi.fn(),
}))
vi.mock('element-plus', () => ({
  ElMessage: { success: vi.fn() },
}))

const profile = {
  siteName: '码上记',
  slogan: '记录实践，分享知识',
  description: '面向开发者的技术学习与知识分享平台。',
  bio: '专注技术内容创作与学习经验分享。',
  aboutContent: '详细介绍本站的定位、内容方向与长期维护原则。',
  contactEmail: 'contact@example.com',
  githubUrl: 'https://github.com/example',
  foundedAt: '2024-05-01',
}

const ElFormStub = {
  template: '<form><slot /></form>',
  setup(_, { expose }) {
    expose({ validate: vi.fn().mockResolvedValue(true) })
  },
}
const ValueStub = {
  props: ['modelValue'],
  template: '<span class="field-value">{{ modelValue }}</span>',
}
const ButtonStub = {
  template: '<button type="button" @click="$emit(\'click\')"><slot /></button>',
}

function mountEditor() {
  return mount(SiteProfileEdit, {
    global: {
      directives: { loading: () => {} },
      stubs: {
        ElCard: { template: '<section><slot name="header" /><slot /></section>' },
        ElForm: ElFormStub,
        ElFormItem: { template: '<label><slot /></label>' },
        ElInput: ValueStub,
        ElDatePicker: ValueStub,
        ElButton: ButtonStub,
      },
    },
  })
}

describe('SiteProfileEdit', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    getAdminSiteProfile.mockResolvedValue({ data: profile })
    updateAdminSiteProfile.mockResolvedValue({ data: profile })
  })

  it('loads all managed site profile fields', async () => {
    const wrapper = mountEditor()
    await flushPromises()

    expect(wrapper.text()).toContain('站点资料')
    expect(wrapper.text()).toContain('码上记')
    expect(wrapper.text()).toContain('记录实践，分享知识')
    expect(wrapper.text()).toContain('contact@example.com')
    expect(wrapper.text()).toContain('2024-05-01')
  })

  it('submits the normalized site profile payload', async () => {
    const wrapper = mountEditor()
    await flushPromises()
    await wrapper.get('button').trigger('click')
    await flushPromises()

    expect(updateAdminSiteProfile).toHaveBeenCalledWith(profile)
  })
})
