<template>
  <el-card class="profile-card" shadow="never" v-loading="loading">
    <template #header>
      <div class="card-header">
        <div>
          <h2>公开资料</h2>
          <p>这些信息会展示在你的作者主页，可随时更新。</p>
        </div>
        <el-button type="primary" :loading="saving" @click="submit">保存资料</el-button>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="profile-form">
      <el-form-item label="个人简介" prop="bio">
        <el-input
          v-model="form.bio"
          type="textarea"
          :rows="6"
          maxlength="500"
          show-word-limit
          placeholder="介绍你的技术方向、创作经历和关注领域（20-500字）"
        />
      </el-form-item>

      <el-form-item label="擅长领域" prop="expertise">
        <el-select
          v-model="form.expertise"
          multiple
          filterable
          allow-create
          default-first-option
          :multiple-limit="10"
          placeholder="输入领域后按回车添加，最多10项"
          class="full-width"
        >
          <el-option v-for="item in form.expertise" :key="item" :label="item" :value="item" />
        </el-select>
        <div class="field-tip">例如：Java、Spring Boot、MySQL；单项不超过30个字符。</div>
      </el-form-item>

      <div class="link-grid">
        <el-form-item label="个人网站" prop="websiteUrl">
          <el-input v-model="form.websiteUrl" maxlength="500" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="作品集链接" prop="portfolioUrl">
          <el-input v-model="form.portfolioUrl" maxlength="500" placeholder="https://github.com/username" />
        </el-form-item>
      </div>
    </el-form>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuthorProfile, updateAuthorProfile } from '@/api/authorConsole'

const formRef = ref()
const loading = ref(false)
const saving = ref(false)
const form = reactive({
  bio: '',
  expertise: [],
  websiteUrl: '',
  portfolioUrl: '',
})

const urlRule = {
  pattern: /^https?:\/\/\S+$/,
  message: '请输入有效的 HTTP/HTTPS 地址',
  trigger: 'blur',
}
const rules = {
  bio: [
    { required: true, message: '请输入个人简介', trigger: 'blur' },
    { min: 20, max: 500, message: '个人简介长度应为20-500个字符', trigger: 'blur' },
  ],
  expertise: [
    { type: 'array', required: true, min: 1, message: '请至少填写一个擅长领域', trigger: 'change' },
    {
      validator: (_rule, values, callback) => {
        if (values.some((item) => !item.trim() || item.trim().length > 30)) {
          callback(new Error('单个擅长领域不能为空且不能超过30个字符'))
        } else callback()
      },
      trigger: 'change',
    },
  ],
  websiteUrl: [urlRule],
  portfolioUrl: [urlRule],
}

function splitExpertise(value) {
  if (!value) return []
  return value.split(',').map((item) => item.trim()).filter(Boolean)
}

async function loadProfile() {
  loading.value = true
  try {
    const response = await getAuthorProfile()
    const profile = response.data || {}
    form.bio = profile.bio || ''
    form.expertise = splitExpertise(profile.expertise)
    form.websiteUrl = profile.websiteUrl || ''
    form.portfolioUrl = profile.portfolioUrl || ''
  } finally {
    loading.value = false
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const expertise = [...new Set(form.expertise.map((item) => item.trim()).filter(Boolean))]
    await updateAuthorProfile({
      bio: form.bio.trim(),
      expertise,
      websiteUrl: form.websiteUrl.trim() || null,
      portfolioUrl: form.portfolioUrl.trim() || null,
    })
    form.expertise = expertise
    ElMessage.success('作者资料已更新')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-card { min-height: calc(100vh - 126px); border: 1px solid #e5eaf0; }
.card-header { display: flex; align-items: center; justify-content: space-between; gap: 24px; }
.card-header h2 { margin: 0 0 5px; color: #1f2a3a; font-size: 18px; }
.card-header p { margin: 0; color: #7b8798; font-size: 13px; }
.profile-form { max-width: 920px; }
.full-width { width: 100%; }
.field-tip { margin-top: 7px; color: #8a96a7; font-size: 12px; line-height: 1.5; }
.link-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; }
@media (max-width: 720px) {
  .card-header { align-items: flex-start; flex-direction: column; }
  .link-grid { grid-template-columns: 1fr; gap: 0; }
}
</style>
