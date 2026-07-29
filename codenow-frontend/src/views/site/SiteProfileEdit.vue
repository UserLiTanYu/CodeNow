<template>
  <el-card class="profile-card" shadow="never" v-loading="loading">
    <template #header>
      <div class="card-header">
        <div>
          <h2>站点资料</h2>
          <p>统一维护博客侧栏和“关于本站”页面公开展示的内容。</p>
        </div>
        <el-button type="primary" :loading="saving" @click="submit">保存资料</el-button>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="profile-form">
      <section class="form-section">
        <div class="section-heading">
          <h3>基础信息</h3>
          <p>用于关于页标题区域和站点身份展示。</p>
        </div>
        <div class="two-column-grid">
          <el-form-item label="站点名称" prop="siteName">
            <el-input v-model="form.siteName" maxlength="50" show-word-limit placeholder="例如：码上记" />
          </el-form-item>
          <el-form-item label="站点标语" prop="slogan">
            <el-input v-model="form.slogan" maxlength="100" show-word-limit placeholder="一句话说明本站理念" />
          </el-form-item>
        </div>
        <el-form-item label="关于页简介" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="简要说明网站面向的人群和主要内容"
          />
        </el-form-item>
      </section>

      <section class="form-section">
        <div class="section-heading">
          <h3>公开介绍</h3>
          <p>侧栏使用简短简介，关于页正文支持 Markdown。</p>
        </div>
        <el-form-item label="侧栏简介" prop="bio">
          <el-input
            v-model="form.bio"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="用一至三句话介绍站长或本站关注的技术方向"
          />
        </el-form-item>
        <el-form-item label="关于页正文" prop="aboutContent">
          <el-input
            v-model="form.aboutContent"
            type="textarea"
            :rows="10"
            maxlength="5000"
            show-word-limit
            placeholder="详细介绍本站的由来、定位和内容方向，支持 Markdown"
          />
        </el-form-item>
      </section>

      <section class="form-section">
        <div class="section-heading">
          <h3>联系信息</h3>
          <p>以下字段均为选填，填写后会公开显示在关于页。</p>
        </div>
        <div class="two-column-grid">
          <el-form-item label="联系邮箱" prop="contactEmail">
            <el-input v-model="form.contactEmail" maxlength="100" placeholder="contact@example.com" />
          </el-form-item>
          <el-form-item label="GitHub 地址" prop="githubUrl">
            <el-input v-model="form.githubUrl" maxlength="255" placeholder="https://github.com/username" />
          </el-form-item>
        </div>
        <el-form-item label="建站日期" prop="foundedAt">
          <el-date-picker
            v-model="form.foundedAt"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择建站日期"
            :disabled-date="disableFutureDate"
          />
        </el-form-item>
      </section>
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminSiteProfile, updateAdminSiteProfile } from '@/api/siteProfile'

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  siteName: '',
  slogan: '',
  description: '',
  bio: '',
  aboutContent: '',
  contactEmail: '',
  githubUrl: '',
  foundedAt: '',
})

const rules = {
  siteName: [
    { required: true, message: '请输入站点名称', trigger: 'blur' },
    { min: 2, max: 50, message: '站点名称长度应为 2-50 个字符', trigger: 'blur' },
  ],
  slogan: [
    { required: true, message: '请输入站点标语', trigger: 'blur' },
    { min: 2, max: 100, message: '站点标语长度应为 2-100 个字符', trigger: 'blur' },
  ],
  description: [
    { required: true, message: '请输入关于页简介', trigger: 'blur' },
    { min: 10, max: 500, message: '关于页简介长度应为 10-500 个字符', trigger: 'blur' },
  ],
  bio: [
    { required: true, message: '请输入侧栏简介', trigger: 'blur' },
    { min: 10, max: 500, message: '侧栏简介长度应为 10-500 个字符', trigger: 'blur' },
  ],
  aboutContent: [
    { required: true, message: '请输入关于页正文', trigger: 'blur' },
    { min: 20, max: 5000, message: '关于页正文长度应为 20-5000 个字符', trigger: 'blur' },
  ],
  contactEmail: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' },
  ],
  githubUrl: [
    {
      pattern: /^$|^https:\/\/github\.com\/[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})(?:\/[A-Za-z0-9_.-]+)?\/?$/,
      message: '请输入正确的 GitHub 地址',
      trigger: 'blur',
    },
  ],
}

function assignProfile(profile = {}) {
  Object.assign(form, {
    siteName: profile.siteName || '',
    slogan: profile.slogan || '',
    description: profile.description || '',
    bio: profile.bio || '',
    aboutContent: profile.aboutContent || '',
    contactEmail: profile.contactEmail || '',
    githubUrl: profile.githubUrl || '',
    foundedAt: profile.foundedAt || '',
  })
}

async function loadProfile() {
  loading.value = true
  try {
    const response = await getAdminSiteProfile()
    assignProfile(response.data)
  } finally {
    loading.value = false
  }
}

function normalizedPayload() {
  return {
    siteName: form.siteName.trim(),
    slogan: form.slogan.trim(),
    description: form.description.trim(),
    bio: form.bio.trim(),
    aboutContent: form.aboutContent.trim(),
    contactEmail: form.contactEmail.trim(),
    githubUrl: form.githubUrl.trim(),
    foundedAt: form.foundedAt || null,
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const response = await updateAdminSiteProfile(normalizedPayload())
    assignProfile(response.data)
    ElMessage.success('站点资料已更新')
  } finally {
    saving.value = false
  }
}

function disableFutureDate(date) {
  return date.getTime() > Date.now()
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-card { min-height: calc(100vh - 126px); border: 1px solid #e5eaf0; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.card-header h2 { margin: 0 0 5px; color: #1f2a3a; font-size: 18px; }
.card-header p { margin: 0; color: #7b8798; font-size: 13px; }
.profile-form { max-width: 980px; }
.form-section + .form-section { margin-top: 30px; padding-top: 26px; border-top: 1px solid #edf0f4; }
.section-heading { margin-bottom: 20px; }
.section-heading h3 { margin: 0 0 5px; color: #263244; font-size: 16px; }
.section-heading p { margin: 0; color: #8a96a7; font-size: 12px; line-height: 1.5; }
.two-column-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
@media (max-width: 720px) {
  .card-header { align-items: flex-start; flex-direction: column; }
  .two-column-grid { grid-template-columns: 1fr; gap: 0; }
}
</style>
