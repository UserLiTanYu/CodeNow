<template>
  <section class="comment-list-card">
    <div class="list-heading">
      <div>
        <h2>文章评论</h2>
        <p>查看并管理自己文章下的评论与回复</p>
      </div>
    </div>

    <div class="filters">
      <el-select
        v-model="filterArticleId"
        aria-label="按文章筛选"
        placeholder="全部文章"
        clearable
        filterable
        @change="handleFilterChange"
      >
        <el-option v-for="article in articles" :key="article.id" :label="article.title" :value="article.id" />
      </el-select>
    </div>

    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" show-icon :closable="false" />
      <el-button type="primary" @click="loadComments">重新加载</el-button>
    </div>

    <el-table v-loading="loading" :data="comments" stripe>
      <el-table-column prop="articleTitle" label="所属文章" min-width="180" show-overflow-tooltip />
      <el-table-column prop="nickname" label="评论者" width="120">
        <template #default="{ row }">{{ row.nickname || '匿名访客' }}</template>
      </el-table-column>
      <el-table-column prop="content" label="评论内容" min-width="280" show-overflow-tooltip />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">{{ row.parentId ? '回复' : '评论' }}</template>
      </el-table-column>
      <el-table-column prop="createTime" label="评论时间" width="170" :formatter="formatDateCell" />
      <el-table-column label="操作" width="100" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      class="pagination"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      @current-change="loadComments"
    />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAuthorComment, getAuthorArticles, getAuthorComments } from '@/api/authorConsole'
import { formatDateCell } from '@/utils/format'

const comments = ref([])
const articles = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const filterArticleId = ref(null)
const loading = ref(false)
const loadError = ref('')
let latestRequestId = 0

async function loadComments() {
  const requestId = ++latestRequestId
  loading.value = true
  loadError.value = ''
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (filterArticleId.value) params.articleId = filterArticleId.value
    const response = await getAuthorComments(params)
    if (requestId !== latestRequestId) return
    if (response.data.records.length === 0 && pageNum.value > 1 && response.data.total > 0) {
      pageNum.value = Math.ceil(response.data.total / pageSize)
      return loadComments()
    }
    comments.value = response.data.records
    total.value = response.data.total
  } catch {
    if (requestId === latestRequestId) loadError.value = '评论加载失败，请检查网络后重试'
  } finally {
    if (requestId === latestRequestId) loading.value = false
  }
}

async function loadArticles() {
  try {
    const response = await getAuthorArticles({ pageNum: 1, pageSize: 100 })
    articles.value = response.data.records.map((item) => item.article)
  } catch {
    articles.value = []
  }
}

function handleFilterChange() {
  pageNum.value = 1
  loadComments()
}

async function handleDelete(id) {
  await ElMessageBox.confirm('删除该评论会同时删除它的全部回复，是否继续？', '提示', { type: 'warning' })
  await deleteAuthorComment(id)
  ElMessage.success('评论已删除')
  await loadComments()
}

onMounted(() => {
  loadComments()
  loadArticles()
})
</script>

<style scoped>
.comment-list-card { min-width: 0; padding: 24px; border: 1px solid #e5eaf0; border-radius: 12px; background: #fff; box-shadow: 0 8px 24px rgba(31, 42, 58, 0.04); }
.list-heading { margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
h2 { margin: 0 0 5px; color: #1f2a3a; font-size: 22px; }
p { margin: 0; color: #8490a0; font-size: 13px; }
.filters { margin-bottom: 16px; display: flex; gap: 10px; }
.filters :deep(.el-select) { width: min(360px, 100%); }
.load-error { margin-bottom: 16px; display: flex; align-items: center; gap: 12px; }
.load-error .el-alert { flex: 1; }
.pagination { margin-top: 20px; justify-content: center; }
@media (max-width: 720px) { .comment-list-card { padding: 16px; } .load-error { align-items: stretch; flex-direction: column; } }
</style>
