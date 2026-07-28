<template>
  <el-card class="profile-card" shadow="never" v-loading="loading">
    <template #header>
      <div class="card-header">
        <div>
          <h2>个人简介</h2>
          <p>该内容会显示在博客首页右侧的“个人简介”区域。</p>
        </div>
        <el-button type="primary" :loading="saving" @click="submit">保存简介</el-button>
      </div>
    </template>

    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="profile-form">
      <el-form-item label="公开简介" prop="bio">
        <el-input
          v-model="form.bio"
          type="textarea"
          :rows="8"
          maxlength="500"
          show-word-limit
          placeholder="介绍你的技术方向、博客内容和关注领域"
        />
      </el-form-item>
      <p class="field-tip">建议使用简洁的一至三句话介绍自己，长度为 10-500 个字符。</p>
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
const form = reactive({ bio: '' })
const rules = {
  bio: [
    { required: true, message: '请输入个人简介', trigger: 'blur' },
    { min: 10, max: 500, message: '个人简介长度应为10-500个字符', trigger: 'blur' },
  ],
}

async function loadProfile() {
  loading.value = true
  try {
    const response = await getAdminSiteProfile()
    form.bio = response.data?.bio || ''
  } finally {
    loading.value = false
  }
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    const bio = form.bio.trim()
    await updateAdminSiteProfile({ bio })
    form.bio = bio
    ElMessage.success('个人简介已更新')
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
.field-tip { margin: -8px 0 0; color: #8a96a7; font-size: 12px; line-height: 1.5; }
@media (max-width: 720px) {
  .card-header { align-items: flex-start; flex-direction: column; }
}
</style>
