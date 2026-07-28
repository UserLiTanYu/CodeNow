<template>
  <section class="member-card">
    <!-- 页面标题和快捷导航链接 -->
    <div class="member-heading">
      <div><h1>个人中心</h1><p>管理头像、公开资料和账号安全</p></div>
      <div class="member-links">
        <router-link to="/blog/comments">我的评论</router-link>
        <router-link to="/blog/favorites">我的收藏</router-link>
        <router-link v-if="userStore.canEnterAuthorConsole" to="/author-console/articles">作者工作台</router-link>
      </div>
    </div>

    <!-- 加载中骨架屏 -->
    <el-skeleton v-if="loading" :rows="8" animated />
    <!-- 个人中心标签页：个人资料、修改邮箱、修改密码 -->
    <el-tabs v-else v-model="activeTab">
      <!-- 个人资料标签页 -->
      <el-tab-pane label="个人资料" name="profile">
        <div class="avatar-row">
          <el-avatar :size="72" :src="avatarUrl(profile.avatar)" @error="handleProfileAvatarError" />
          <el-upload :show-file-list="false" accept="image/jpeg,image/png,image/gif,image/webp" :http-request="handleAvatarUpload">
            <el-button :loading="avatarLoading">上传头像</el-button>
          </el-upload>
          <span>支持 JPG、PNG、GIF、WebP，最大 5MB</span>
        </div>
        <el-form label-position="top" class="profile-form">
          <el-form-item label="用户名"><el-input :model-value="profile.username" disabled /></el-form-item>
          <el-form-item label="昵称"><el-input v-model="profile.nickname" maxlength="50" show-word-limit /></el-form-item>
          <el-form-item label="角色"><el-tag :type="roleMeta.type">{{ roleMeta.label }}</el-tag></el-form-item>
          <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
        </el-form>
      </el-tab-pane>

      <!-- 修改邮箱标签页 -->
      <el-tab-pane label="修改邮箱" name="email">
        <el-form label-position="top" class="profile-form">
          <el-form-item label="当前邮箱"><el-input :model-value="profile.email" disabled /></el-form-item>
          <el-form-item label="新邮箱"><el-input v-model="emailForm.email" maxlength="100" /></el-form-item>
          <el-form-item label="邮箱验证码">
            <div class="code-row">
              <el-input v-model="emailForm.verificationCode" maxlength="6" />
              <el-button :disabled="emailSeconds > 0" :loading="emailCodeLoading" @click="sendEmailCode">
                {{ emailSeconds ? `${emailSeconds}s` : '发送验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-button type="primary" :loading="emailSaving" @click="saveEmail">确认修改邮箱</el-button>
        </el-form>
      </el-tab-pane>

      <!-- 修改密码标签页 -->
      <el-tab-pane label="修改密码" name="password">
        <el-form label-position="top" class="profile-form">
          <el-form-item label="当前密码"><el-input v-model="passwordForm.currentPassword" type="password" show-password /></el-form-item>
          <el-form-item label="新密码"><el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="8-72 位" /></el-form-item>
          <el-form-item label="确认新密码"><el-input v-model="passwordForm.confirmPassword" type="password" show-password /></el-form-item>
          <el-button type="primary" :loading="passwordSaving" @click="savePassword">修改密码</el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
/** 会员个人中心页面 - 管理个人资料、头像、邮箱和密码 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getProfile, updateProfile } from '@/api/auth'
import { changeEmail, changePassword, sendChangeEmailCode, uploadAvatar } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { avatarUrl } from '@/utils/avatar'

const router = useRouter()
const userStore = useUserStore()
const activeTab = ref('profile')
const loading = ref(true)
const saving = ref(false)
const avatarLoading = ref(false)
const emailSaving = ref(false)
const emailCodeLoading = ref(false)
const passwordSaving = ref(false)
const emailSeconds = ref(0)
/** 个人资料数据 */
const profile = reactive({ username: '', email: '', nickname: '', avatar: '', role: '' })
/** 角色显示元数据 */
const roleMeta = computed(() => ({
  ADMIN: { label: '管理员', type: 'danger' },
  AUTHOR: { label: '作者', type: 'success' },
  USER: { label: '普通用户', type: 'info' },
}[profile.role?.toUpperCase()] || { label: '普通用户', type: 'info' }))
/** 修改邮箱表单数据 */
const emailForm = reactive({ email: '', verificationCode: '' })
/** 修改密码表单数据 */
const passwordForm = reactive({ currentPassword: '', newPassword: '', confirmPassword: '' })
/** 邮件验证码倒计时只用于交互提示，真正的发送冷却由后端 Redis 原子限制 */
let timer

/** 处理头像加载失败 */
function handleProfileAvatarError() {
  profile.avatar = ''
}

/** 加载个人资料数据 */
async function loadProfile() {
  loading.value = true
  try { const res = await getProfile(); Object.assign(profile, res.data) } finally { loading.value = false }
}

/** 保存个人资料（昵称） */
async function saveProfile() {
  if (!profile.nickname.trim()) return ElMessage.warning('昵称不能为空')
  saving.value = true
  try {
    const res = await updateProfile({ nickname: profile.nickname.trim() })
    Object.assign(profile, res.data)
    userStore.userInfo = { ...userStore.userInfo, nickname: res.data.nickname }
    ElMessage.success('个人资料已更新')
  } finally { saving.value = false }
}

/** 上传头像 */
async function handleAvatarUpload({ file }) {
  if (file.size > 5 * 1024 * 1024) return ElMessage.warning('头像不能超过 5MB')
  avatarLoading.value = true
  try {
    const data = new FormData(); data.append('file', file)
    const res = await uploadAvatar(data)
    profile.avatar = res.data.url
    userStore.userInfo = { ...userStore.userInfo, avatar: res.data.url }
    ElMessage.success('头像已更新')
  } finally { avatarLoading.value = false }
}

/** 发送邮箱验证码 */
async function sendEmailCode() {
  if (!/^\S+@\S+\.\S+$/.test(emailForm.email)) return ElMessage.warning('请输入正确的新邮箱')
  emailCodeLoading.value = true
  try {
    await sendChangeEmailCode(emailForm.email.trim())
    ElMessage.success('验证码已发送')
    emailSeconds.value = 60
    timer = setInterval(() => { if (--emailSeconds.value <= 0) clearInterval(timer) }, 1000)
  } finally { emailCodeLoading.value = false }
}

/** 保存新邮箱 */
async function saveEmail() {
  if (!/^\S+@\S+\.\S+$/.test(emailForm.email) || !/^\d{6}$/.test(emailForm.verificationCode)) {
    return ElMessage.warning('请填写正确的新邮箱和6位验证码')
  }
  emailSaving.value = true
  try {
    const res = await changeEmail({ email: emailForm.email.trim(), verificationCode: emailForm.verificationCode })
    Object.assign(profile, res.data); emailForm.email = ''; emailForm.verificationCode = ''
    ElMessage.success('邮箱已修改')
  } finally { emailSaving.value = false }
}

/** 修改密码（修改后需重新登录） */
async function savePassword() {
  if (passwordForm.newPassword.length < 8 || passwordForm.newPassword.length > 72) return ElMessage.warning('新密码长度应为8-72位')
  if (passwordForm.newPassword !== passwordForm.confirmPassword) return ElMessage.warning('两次新密码不一致')
  passwordSaving.value = true
  try {
    await changePassword({ currentPassword: passwordForm.currentPassword, newPassword: passwordForm.newPassword })
    ElMessage.success('密码已修改，请重新登录')
    await userStore.logout()
    router.replace('/login')
  } finally { passwordSaving.value = false }
}

/** 页面挂载时加载个人资料 */
onMounted(loadProfile)
/** 组件卸载前清除倒计时定时器 */
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.member-card { padding: 28px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: #fff; }
.member-heading { margin-bottom: 24px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
h1 { margin: 0 0 6px; font-size: 24px; } p { margin: 0; color: var(--blog-color-text-muted); }
.member-links { display: flex; gap: 10px; } .member-links a { padding: 8px 13px; border: 1px solid var(--blog-color-border); border-radius: 6px; color: var(--blog-color-primary); text-decoration: none; }
.profile-form { max-width: 560px; padding-top: 18px; }
.avatar-row { margin: 20px 0 8px; display: flex; align-items: center; gap: 14px; } .avatar-row span { color: var(--blog-color-text-muted); font-size: 12px; }
.code-row { width: 100%; display: grid; grid-template-columns: 1fr 120px; gap: 10px; }
@media (max-width: 600px) { .member-heading, .avatar-row { align-items: flex-start; flex-direction: column; } }
</style>
