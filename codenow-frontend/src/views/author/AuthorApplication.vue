<template>
  <section class="author-page" v-loading="loading">
    <div class="hero">
      <div>
        <p class="eyebrow">AUTHOR PROGRAM</p>
        <h1>成为码上记作者</h1>
        <p>分享真实的技术实践、项目复盘和学习经验。申请通过后，作者身份会立即生效。</p>
      </div>
      <el-tag v-if="userStore.isAuthor" type="success" size="large">已成为作者</el-tag>
    </div>

    <el-alert
      v-if="userStore.isAuthor"
      title="你的作者身份已生效"
      description="作者内容管理将在下一阶段开放；当前可以继续完善个人资料并关注站内通知。"
      type="success"
      :closable="false"
      show-icon
    />

    <el-card v-else-if="latest?.status === 'PENDING'" class="status-card" shadow="never">
      <template #header><strong>申请审核中</strong></template>
      <el-steps :active="1" finish-status="success" align-center>
        <el-step title="提交申请" :description="formatTime(latest.submittedAt)" />
        <el-step title="管理员审核" description="请留意消息中心" />
        <el-step title="身份生效" />
      </el-steps>
      <div class="status-actions">
        <span>审核期间不能重复提交。如需修改资料，请先撤回当前申请。</span>
        <el-button type="danger" plain :loading="submitting" @click="cancelPending">撤回申请</el-button>
      </div>
    </el-card>

    <el-card v-else class="form-card" shadow="never">
      <template #header>
        <div class="card-title">
          <strong>{{ latest?.status === 'REJECTED' ? '重新提交作者申请' : '作者申请资料' }}</strong>
          <el-tag v-if="latest?.status === 'REJECTED'" type="danger">上次申请未通过</el-tag>
          <el-tag v-if="latest?.status === 'CANCELED'" type="info">上次申请已撤回</el-tag>
        </div>
      </template>
      <el-alert
        v-if="latest?.status === 'REJECTED' && latest.reviewRemark"
        :title="`审核意见：${latest.reviewRemark}`"
        type="warning"
        :closable="false"
        show-icon
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="application-form">
        <el-form-item label="申请理由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="5" maxlength="1000" show-word-limit placeholder="请说明为什么希望成为作者，以及你准备分享哪些内容（至少 50 字）" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="擅长领域" prop="expertiseText">
            <el-input v-model="form.expertiseText" maxlength="300" placeholder="例如：Java, Spring Boot, MySQL" />
            <span class="field-hint">使用逗号分隔，最多 10 项</span>
          </el-form-item>
          <el-form-item label="作品链接（选填）" prop="portfolioUrl">
            <el-input v-model="form.portfolioUrl" placeholder="https://example.com/portfolio" />
          </el-form-item>
        </div>
        <el-form-item label="个人简介" prop="bio">
          <el-input v-model="form.bio" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="用于作者资料的简介（至少 20 字）" />
        </el-form-item>
        <el-form-item label="个人网站（选填）" prop="websiteUrl">
          <el-input v-model="form.websiteUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item prop="agreementAccepted">
          <el-checkbox v-model="form.agreementAccepted">我承诺提交真实资料，并同意遵守码上记作者创作规范</el-checkbox>
        </el-form-item>
        <el-button type="primary" size="large" :loading="submitting" @click="submit">提交申请</el-button>
      </el-form>
    </el-card>

    <el-card class="history-card" shadow="never">
      <template #header><strong>申请记录</strong></template>
      <el-table :data="history" empty-text="暂无申请记录">
        <el-table-column prop="submittedAt" label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column prop="expertise" label="擅长领域" min-width="220">
          <template #default="{ row }">{{ row.expertise?.join(' / ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusMeta[row.status]?.type">{{ statusMeta[row.status]?.label || row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="reviewRemark" label="审核意见" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { cancelAuthorApplication, getAuthorApplicationHistory, getAuthorApplicationLatest, submitAuthorApplication } from '@/api/authorApplication'

const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)
const submitting = ref(false)
const latest = ref(null)
const history = ref([])
const form = reactive({ reason: '', expertiseText: '', bio: '', portfolioUrl: '', websiteUrl: '', agreementAccepted: false })
const statusMeta = {
  PENDING: { label: '审核中', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '未通过', type: 'danger' },
  CANCELED: { label: '已撤回', type: 'info' },
}
const optionalUrl = (_rule, value, callback) => {
  if (!value || /^https?:\/\/\S+$/.test(value)) callback()
  else callback(new Error('请输入有效的 HTTP/HTTPS 地址'))
}
const rules = {
  reason: [{ required: true, message: '请输入申请理由', trigger: 'blur' }, { min: 50, max: 1000, message: '长度应为 50-1000 个字符', trigger: 'blur' }],
  expertiseText: [{ required: true, message: '请填写擅长领域', trigger: 'blur' }],
  bio: [{ required: true, message: '请输入个人简介', trigger: 'blur' }, { min: 20, max: 500, message: '长度应为 20-500 个字符', trigger: 'blur' }],
  portfolioUrl: [{ validator: optionalUrl, trigger: 'blur' }],
  websiteUrl: [{ validator: optionalUrl, trigger: 'blur' }],
  agreementAccepted: [{ validator: (_r, value, callback) => value ? callback() : callback(new Error('请先同意作者创作规范')), trigger: 'change' }],
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

async function load() {
  loading.value = true
  try {
    const [latestRes, historyRes] = await Promise.all([
      getAuthorApplicationLatest(),
      getAuthorApplicationHistory({ pageNum: 1, pageSize: 20 }),
    ])
    latest.value = latestRes.data
    history.value = historyRes.data?.records || []
    if (latest.value?.status === 'APPROVED') await userStore.fetchUserInfo()
  } finally {
    loading.value = false
  }
}

async function submit() {
  await formRef.value.validate()
  const expertise = [...new Set(form.expertiseText.split(/[,，]/).map((item) => item.trim()).filter(Boolean))]
  if (!expertise.length || expertise.length > 10) return ElMessage.warning('擅长领域应为 1-10 项')
  submitting.value = true
  try {
    await submitAuthorApplication({
      reason: form.reason.trim(), expertise, bio: form.bio.trim(),
      portfolioUrl: form.portfolioUrl.trim() || null,
      websiteUrl: form.websiteUrl.trim() || null,
      agreementAccepted: form.agreementAccepted,
    })
    ElMessage.success('作者申请已提交')
    await load()
  } finally {
    submitting.value = false
  }
}

async function cancelPending() {
  await ElMessageBox.confirm('撤回后可以修改资料并重新提交，确定撤回吗？', '撤回作者申请', { type: 'warning' })
  submitting.value = true
  try {
    await cancelAuthorApplication(latest.value.id)
    ElMessage.success('申请已撤回')
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  if (!userStore.userInfo) await userStore.fetchUserInfo()
  await load()
})
</script>

<style scoped>
.author-page { display: grid; gap: 20px; max-width: 1050px; margin: 0 auto; }
.hero { padding: 34px; border-radius: 18px; color: #fff; background: linear-gradient(135deg, #172554, #1d4ed8 58%, #06b6d4); display: flex; justify-content: space-between; align-items: center; box-shadow: 0 18px 40px rgba(29, 78, 216, .18); }
.hero h1 { margin: 6px 0 10px; font-size: 30px; }
.hero p { margin: 0; line-height: 1.7; opacity: .88; }
.eyebrow { font-size: 12px; letter-spacing: .16em; font-weight: 700; }
.form-card, .status-card, .history-card { border-radius: 14px; }
.card-title, .status-actions { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.application-form { margin-top: 18px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.field-hint { color: #909399; font-size: 12px; }
.status-actions { margin-top: 30px; color: #606266; }
@media (max-width: 720px) { .hero, .status-actions { align-items: flex-start; flex-direction: column; } .form-grid { grid-template-columns: 1fr; gap: 0; } }
</style>
