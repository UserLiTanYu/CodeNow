<template>
  <section class="member-card">
    <div class="member-heading">
      <div>
        <h1>个人中心</h1>
        <p>管理你的公开昵称和账号信息</p>
      </div>
      <router-link to="/blog/favorites" class="secondary-link">我的收藏</router-link>
    </div>
    <el-skeleton v-if="loading" :rows="5" animated />
    <el-form v-else label-position="top" class="profile-form">
      <el-form-item label="用户名"><el-input :model-value="profile.username" disabled /></el-form-item>
      <el-form-item label="邮箱"><el-input :model-value="profile.email" disabled /></el-form-item>
      <el-form-item label="昵称">
        <el-input v-model="profile.nickname" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="角色"><el-tag>{{ profile.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag></el-form-item>
      <el-button type="primary" :loading="saving" @click="saveProfile">保存修改</el-button>
    </el-form>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(true)
const saving = ref(false)
const profile = reactive({ username: '', email: '', nickname: '', role: '' })

async function loadProfile() {
  loading.value = true
  try {
    const res = await getProfile()
    Object.assign(profile, res.data)
  } finally {
    loading.value = false
  }
}

async function saveProfile() {
  if (!profile.nickname.trim()) return ElMessage.warning('昵称不能为空')
  saving.value = true
  try {
    const res = await updateProfile({ nickname: profile.nickname.trim() })
    Object.assign(profile, res.data)
    userStore.userInfo = { ...userStore.userInfo, nickname: res.data.nickname }
    ElMessage.success('个人资料已更新')
  } finally {
    saving.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.member-card { padding: 28px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: #fff; }
.member-heading { margin-bottom: 28px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
h1 { margin: 0 0 6px; font-size: 24px; } p { margin: 0; color: var(--blog-color-text-muted); }
.profile-form { max-width: 560px; }
.secondary-link { padding: 8px 13px; border: 1px solid var(--blog-color-border); border-radius: 6px; color: var(--blog-color-primary); text-decoration: none; }
</style>
