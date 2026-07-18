<template>
  <div class="auth-page">
    <router-link to="/blog" class="back-link">← 返回博客</router-link>
    <el-card class="auth-card" shadow="never">
      <div class="brand">码上记</div>
      <h1>{{ pageTitle }}</h1>
      <p class="subtitle">{{ pageSubtitle }}</p>

      <el-tabs v-if="mode !== 'reset'" v-model="mode" stretch>
        <el-tab-pane label="登录" name="login" />
        <el-tab-pane label="注册" name="register" />
      </el-tabs>

      <el-form v-if="mode === 'login'" ref="loginFormRef" :model="loginForm" :rules="loginRules" @keyup.enter="handleLogin">
        <el-form-item prop="account">
          <el-input v-model="loginForm.account" placeholder="用户名或邮箱" size="large" maxlength="100" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="loginForm.password" placeholder="密码" type="password" size="large" show-password />
        </el-form-item>
        <el-form-item prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="loginForm.captchaCode" placeholder="计算图形验证码" size="large" maxlength="4" />
            <button type="button" class="captcha-image" title="点击刷新验证码" @click="loadCaptcha">
              <img v-if="captchaImage" :src="captchaImage" alt="图形验证码" />
              <span v-else>加载中</span>
            </button>
          </div>
        </el-form-item>
        <div class="form-helper"><button type="button" @click="mode = 'reset'">忘记密码？</button></div>
        <el-button type="primary" size="large" class="submit-button" :loading="loading" @click="handleLogin">登录</el-button>
      </el-form>

      <el-form v-else-if="mode === 'register'" ref="registerFormRef" :model="registerForm" :rules="registerRules">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="用户名（4-30 位字母、数字或下划线）" size="large" maxlength="30" />
        </el-form-item>
        <el-form-item prop="email">
          <el-input v-model="registerForm.email" placeholder="邮箱" size="large" maxlength="100" />
        </el-form-item>
        <el-form-item prop="verificationCode">
          <div class="code-row">
            <el-input v-model="registerForm.verificationCode" placeholder="6 位邮箱验证码" size="large" maxlength="6" />
            <el-button size="large" :disabled="codeSeconds > 0" :loading="codeLoading" @click="handleSendCode('register')">
              {{ codeSeconds > 0 ? `${codeSeconds}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" placeholder="密码（至少 8 位）" type="password" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" placeholder="确认密码" type="password" size="large" show-password />
        </el-form-item>
        <el-form-item prop="agreementAccepted">
          <el-checkbox v-model="registerForm.agreementAccepted">
            我已阅读并同意
            <router-link to="/blog/terms" target="_blank">用户协议</router-link>
            和
            <router-link to="/blog/privacy" target="_blank">隐私政策</router-link>
          </el-checkbox>
        </el-form-item>
        <el-button type="primary" size="large" class="submit-button" :loading="loading" @click="handleRegister">注册</el-button>
      </el-form>

      <el-form v-else ref="resetFormRef" :model="resetForm" :rules="resetRules">
        <el-form-item prop="email">
          <el-input v-model="resetForm.email" placeholder="注册邮箱" size="large" maxlength="100" />
        </el-form-item>
        <el-form-item prop="verificationCode">
          <div class="code-row">
            <el-input v-model="resetForm.verificationCode" placeholder="6 位邮箱验证码" size="large" maxlength="6" />
            <el-button size="large" :disabled="codeSeconds > 0" :loading="codeLoading" @click="handleSendCode('reset')">
              {{ codeSeconds > 0 ? `${codeSeconds}s` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item prop="newPassword">
          <el-input v-model="resetForm.newPassword" placeholder="新密码（至少 8 位）" type="password" size="large" show-password />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" placeholder="确认新密码" type="password" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" class="submit-button" :loading="loading" @click="handleReset">重置密码</el-button>
        <button type="button" class="return-login" @click="mode = 'login'">返回登录</button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCaptcha, register, resetPassword, sendRegisterCode, sendResetCode } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const mode = ref(route.query.mode === 'register' ? 'register' : 'login')
const loading = ref(false)
const codeLoading = ref(false)
const codeSeconds = ref(0)
const loginFormRef = ref()
const registerFormRef = ref()
const resetFormRef = ref()
let timer

const loginForm = reactive({ account: '', password: '', captchaId: '', captchaCode: '' })
const captchaImage = ref('')
const registerForm = reactive({ username: '', email: '', verificationCode: '', password: '', confirmPassword: '', agreementAccepted: false })
const resetForm = reactive({ email: '', verificationCode: '', newPassword: '', confirmPassword: '' })
const emailRule = { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }

const loginRules = {
  account: [{ required: true, message: '请输入用户名或邮箱', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  captchaCode: [{ required: true, message: '请输入图形验证码', trigger: 'blur' }],
}
const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9_]{4,30}$/, message: '用户名需为 4-30 位字母、数字或下划线', trigger: 'blur' },
  ],
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, emailRule],
  verificationCode: [{ required: true, pattern: /^\d{6}$/, message: '请输入 6 位验证码', trigger: 'blur' }],
  password: [{ required: true, min: 8, max: 64, message: '密码长度需为 8-64 位', trigger: 'blur' }],
  confirmPassword: [{ validator: (_, value, callback) => value === registerForm.password ? callback() : callback(new Error('两次密码不一致')), trigger: 'blur' }],
  agreementAccepted: [{ validator: (_, value, callback) => value ? callback() : callback(new Error('请先同意用户协议和隐私政策')), trigger: 'change' }],
}
const resetRules = {
  email: [{ required: true, message: '请输入邮箱', trigger: 'blur' }, emailRule],
  verificationCode: [{ required: true, pattern: /^\d{6}$/, message: '请输入 6 位验证码', trigger: 'blur' }],
  newPassword: [{ required: true, min: 8, max: 64, message: '密码长度需为 8-64 位', trigger: 'blur' }],
  confirmPassword: [{ validator: (_, value, callback) => value === resetForm.newPassword ? callback() : callback(new Error('两次密码不一致')), trigger: 'blur' }],
}

const pageTitle = computed(() => mode.value === 'register' ? '创建读者账号' : mode.value === 'reset' ? '重置密码' : '欢迎回来')
const pageSubtitle = computed(() => mode.value === 'register' ? '注册后即可评论和收藏文章' : mode.value === 'reset' ? '通过注册邮箱验证身份' : '登录后继续阅读与交流')

watch(mode, () => {
  clearInterval(timer)
  codeSeconds.value = 0
})

function startCountdown() {
  codeSeconds.value = 60
  clearInterval(timer)
  timer = setInterval(() => {
    codeSeconds.value -= 1
    if (codeSeconds.value <= 0) clearInterval(timer)
  }, 1000)
}

async function handleSendCode(scene) {
  const email = scene === 'register' ? registerForm.email : resetForm.email
  if (!/^\S+@\S+\.\S+$/.test(email)) {
    ElMessage.warning('请先输入正确的邮箱地址')
    return
  }
  codeLoading.value = true
  try {
    if (scene === 'register') await sendRegisterCode(email)
    else await sendResetCode(email)
    ElMessage.success('验证码已发送，请检查邮箱')
    startCountdown()
  } finally {
    codeLoading.value = false
  }
}

async function handleLogin() {
  if (!await loginFormRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    const res = await userStore.login(loginForm.account.trim(), loginForm.password, loginForm.captchaId, loginForm.captchaCode)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/') && !route.query.redirect.startsWith('//')
      ? route.query.redirect
      : res.data.role?.toUpperCase() === 'ADMIN'
        ? '/'
        : res.data.role?.toUpperCase() === 'AUTHOR' ? '/author-console/articles' : '/blog'
    router.replace(redirect)
  } catch {
    loginForm.captchaCode = ''
    await loadCaptcha()
  } finally {
    loading.value = false
  }
}

async function loadCaptcha() {
  captchaImage.value = ''
  try {
    const res = await getCaptcha()
    loginForm.captchaId = res.data.captchaId
    captchaImage.value = res.data.image
  } catch {
    loginForm.captchaId = ''
  }
}

async function handleRegister() {
  if (!await registerFormRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    await register({
      username: registerForm.username.trim(),
      email: registerForm.email.trim(),
      password: registerForm.password,
      verificationCode: registerForm.verificationCode,
      agreementAccepted: registerForm.agreementAccepted,
      agreementVersion: '2026-01',
    })
    ElMessage.success('注册成功，请登录')
    loginForm.account = registerForm.username.trim()
    mode.value = 'login'
  } finally {
    loading.value = false
  }
}

async function handleReset() {
  if (!await resetFormRef.value.validate().catch(() => false)) return
  loading.value = true
  try {
    await resetPassword({ email: resetForm.email.trim(), verificationCode: resetForm.verificationCode, newPassword: resetForm.newPassword })
    ElMessage.success('密码已重置，请重新登录')
    loginForm.account = resetForm.email.trim()
    mode.value = 'login'
  } finally {
    loading.value = false
  }
}

onBeforeUnmount(() => clearInterval(timer))
onMounted(loadCaptcha)
</script>

<style scoped>
.auth-page { min-height: 100vh; padding: 48px 16px; display: grid; place-items: center; box-sizing: border-box; background: #f5f7fa; }
.back-link { position: fixed; top: 24px; left: 28px; color: #606873; text-decoration: none; }
.auth-card { width: min(440px, 100%); border-radius: 14px; }
.auth-card :deep(.el-card__body) { padding: 32px; }
.brand { color: var(--blog-color-primary); font-size: 22px; font-weight: 750; text-align: center; }
h1 { margin: 14px 0 6px; color: var(--blog-color-text); font-size: 25px; text-align: center; }
.subtitle { margin: 0 0 22px; color: var(--blog-color-text-muted); font-size: 14px; text-align: center; }
.code-row { width: 100%; display: grid; grid-template-columns: minmax(0, 1fr) 124px; gap: 10px; }
.captcha-row { width: 100%; display: grid; grid-template-columns: minmax(0, 1fr) 150px; gap: 10px; }
.captcha-image { height: 40px; padding: 0; overflow: hidden; border: 1px solid #dcdfe6; border-radius: 6px; background: #f4f7fb; cursor: pointer; }
.captcha-image img { width: 100%; height: 100%; display: block; }
.submit-button { width: 100%; }
.form-helper { margin: -4px 0 14px; text-align: right; }
.form-helper button, .return-login { border: 0; color: var(--blog-color-primary); background: transparent; cursor: pointer; }
.return-login { width: 100%; margin-top: 16px; }
.auth-card a { color: var(--blog-color-primary); text-decoration: none; }
@media (max-width: 480px) { .auth-card :deep(.el-card__body) { padding: 24px 20px; } .back-link { position: absolute; top: 18px; left: 18px; } }
</style>
