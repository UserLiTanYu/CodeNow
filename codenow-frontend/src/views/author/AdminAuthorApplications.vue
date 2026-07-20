<template>
  <div class="page-container">
    <el-card shadow="never">
      <!-- 页面标题和刷新按钮 -->
      <div class="toolbar">
        <div>
          <h2>作者申请审批</h2>
          <p>审核用户的创作经历和申请资料，审批结果会通过站内消息通知申请人。</p>
        </div>
        <el-button :icon="Refresh" :loading="loading" @click="load">刷新</el-button>
      </div>
      <!-- 筛选表单：按状态和关键词筛选申请记录 -->
      <el-form :inline="true" class="filters" @submit.prevent="search">
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px" @change="search">
            <el-option label="审核中" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="未通过" value="REJECTED" />
            <el-option label="已撤回" value="CANCELED" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" clearable placeholder="用户名、昵称、邮箱或领域" style="width: 260px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item><el-button type="primary" @click="search">查询</el-button></el-form-item>
      </el-form>

      <!-- 申请列表表格 -->
      <el-table v-loading="loading" :data="rows" stripe>
        <el-table-column label="申请人" min-width="180">
          <template #default="{ row }">
            <strong>{{ row.nickname || row.username }}</strong>
            <div class="subtext">{{ row.username }} · {{ row.email || '未填写邮箱' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="擅长领域" min-width="220">
          <template #default="{ row }"><el-tag v-for="item in row.expertise" :key="item" class="skill-tag" effect="plain">{{ item }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="submittedAt" label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }"><el-tag :type="statusMeta[row.status]?.type">{{ statusMeta[row.status]?.label || row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="255" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="showDetail(row)">查看</el-button>
            <template v-if="row.status === 'PENDING'">
              <el-button link type="success" @click="approve(row)">通过</el-button>
              <el-button link type="danger" @click="reject(row)">驳回</el-button>
            </template>
            <el-button v-if="row.status === 'APPROVED'" link type="danger" @click="revoke(row)">撤销资格</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页器 -->
      <div class="pagination">
        <el-pagination v-model:current-page="query.pageNum" v-model:page-size="query.pageSize" layout="total, sizes, prev, pager, next" :total="total" @current-change="load" @size-change="search" />
      </div>
    </el-card>

    <!-- 申请详情抽屉 -->
    <el-drawer v-model="drawerOpen" title="作者申请详情" size="560px">
      <template v-if="detail">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="申请人">{{ detail.nickname || detail.username }}（{{ detail.username }}）</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detail.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="statusMeta[detail.status]?.type">{{ statusMeta[detail.status]?.label }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="擅长领域">{{ detail.expertise?.join('、') || '-' }}</el-descriptions-item>
          <el-descriptions-item label="申请理由"><div class="long-text">{{ detail.reason }}</div></el-descriptions-item>
          <el-descriptions-item label="个人简介"><div class="long-text">{{ detail.bio }}</div></el-descriptions-item>
          <el-descriptions-item label="作品链接"><a v-if="detail.portfolioUrl" :href="detail.portfolioUrl" target="_blank" rel="noopener noreferrer">{{ detail.portfolioUrl }}</a><span v-else>-</span></el-descriptions-item>
          <el-descriptions-item label="个人网站"><a v-if="detail.websiteUrl" :href="detail.websiteUrl" target="_blank" rel="noopener noreferrer">{{ detail.websiteUrl }}</a><span v-else>-</span></el-descriptions-item>
          <el-descriptions-item v-if="detail.reviewRemark" label="审核意见">{{ detail.reviewRemark }}</el-descriptions-item>
        </el-descriptions>
        <div v-if="detail.status === 'PENDING'" class="drawer-actions">
          <el-button type="success" @click="approve(detail)">通过申请</el-button>
          <el-button type="danger" plain @click="reject(detail)">驳回申请</el-button>
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
/** 管理员作者申请审批页面 - 审核、通过、驳回和撤销作者申请 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { approveAuthorApplication, getAuthorApplication, getAuthorApplications, rejectAuthorApplication, revokeAuthorRole } from '@/api/authorApplication'

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const drawerOpen = ref(false)
const detail = ref(null)
/** 查询参数：页码、每页条数、状态筛选、关键词搜索 */
const query = reactive({ pageNum: 1, pageSize: 10, status: 'PENDING', keyword: '' })
/** 申请状态对应的显示文本和标签类型映射 */
const statusMeta = {
  PENDING: { label: '审核中', type: 'warning' }, APPROVED: { label: '已通过', type: 'success' },
  REJECTED: { label: '未通过', type: 'danger' }, CANCELED: { label: '已撤回', type: 'info' },
}

/** 格式化时间为中文本地格式 */
function formatTime(value) { return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-' }

/** 重置到第一页并重新加载列表 */
function search() { query.pageNum = 1; load() }

/** 加载申请列表数据 */
async function load() {
  loading.value = true
  try {
    const res = await getAuthorApplications({ ...query, status: query.status || undefined, keyword: query.keyword.trim() || undefined })
    rows.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

/** 查看申请详情，打开详情抽屉 */
async function showDetail(row) {
  const res = await getAuthorApplication(row.id)
  detail.value = res.data
  drawerOpen.value = true
}

/**
 * 通过作者申请
 * 审批操作完成后关闭详情并重新读取列表，以服务端最终状态为准，不在本地猜测状态机结果
 */
async function approve(row) {
  const { value } = await ElMessageBox.prompt('可填写通过意见（选填）', '通过作者申请', {
    confirmButtonText: '确认通过', cancelButtonText: '取消', inputPlaceholder: '资料完整，欢迎加入',
    inputValidator: (text) => !text || text.length <= 500 || '审核意见不能超过 500 字',
  })
  await approveAuthorApplication(row.id, { reviewRemark: value?.trim() || null })
  ElMessage.success('作者申请已通过，身份已生效')
  drawerOpen.value = false
  await load()
}

/** 驳回作者申请，需填写驳回原因 */
async function reject(row) {
  const { value } = await ElMessageBox.prompt('请说明未通过原因，申请人可以修改后重新提交。', '驳回作者申请', {
    confirmButtonText: '确认驳回', cancelButtonText: '取消', inputType: 'textarea', inputPlaceholder: '请输入具体审核意见',
    inputValidator: (text) => (text?.trim() && text.trim().length <= 500) || '请输入 1-500 字驳回原因',
  })
  await rejectAuthorApplication(row.id, { reviewRemark: value.trim() })
  ElMessage.success('申请已驳回')
  drawerOpen.value = false
  await load()
}

/** 撤销已通过的作者资格，需填写撤销原因 */
async function revoke(row) {
  const { value } = await ElMessageBox.prompt('撤销后用户将恢复为普通读者，历史文章不会下线。', '撤销作者资格', {
    confirmButtonText: '确认撤销', cancelButtonText: '取消', inputType: 'textarea', inputPlaceholder: '请输入撤销原因',
    inputValidator: (text) => (text?.trim() && text.trim().length <= 500) || '请输入 1-500 字撤销原因',
  })
  await revokeAuthorRole(row.userId, { reason: value.trim() })
  ElMessage.success('作者资格已撤销')
  drawerOpen.value = false
  await load()
}

/** 页面挂载时加载申请列表 */
onMounted(load)
</script>

<style scoped>
.page-container { min-height: 100%; }
.toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; }
.toolbar h2 { margin: 0 0 8px; font-size: 22px; }
.toolbar p, .subtext { margin: 0; color: #909399; font-size: 13px; }
.filters { margin-top: 22px; padding: 18px 18px 0; background: #f8fafc; border-radius: 10px; }
.skill-tag { margin: 2px 5px 2px 0; }
.pagination { display: flex; justify-content: flex-end; padding-top: 20px; }
.long-text { white-space: pre-wrap; line-height: 1.7; }
.drawer-actions { margin-top: 24px; display: flex; justify-content: flex-end; }
</style>
