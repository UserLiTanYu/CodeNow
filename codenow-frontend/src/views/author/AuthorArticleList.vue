<template>
  <section class="article-list-card">
    <div class="list-heading">
      <div>
        <h2>我的文章</h2>
        <p>管理自己的草稿与已发布内容</p>
      </div>
      <el-button type="primary" @click="router.push('/author-console/articles/edit')">新建文章</el-button>
    </div>

    <div class="filters">
      <el-cascader
        aria-label="按分类筛选"
        v-model="filterCategoryId"
        :options="categoryOptions"
        :props="{ emitPath: false, checkStrictly: true, expandTrigger: 'hover' }"
        placeholder="按分类筛选"
        clearable
        @change="handleFilterChange"
      />
      <el-select v-model="filterTagId" aria-label="按标签筛选" placeholder="按标签筛选" clearable @change="handleFilterChange">
        <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
      </el-select>
    </div>

    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" show-icon :closable="false" />
      <el-button type="primary" @click="loadArticles">重新加载</el-button>
    </div>

    <el-table v-loading="loading" :data="articles" stripe>
      <el-table-column label="标题" min-width="200">
        <template #default="{ row }">{{ row.article.title }}</template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="100" />
      <el-table-column label="标签" min-width="110">
        <template #default="{ row }">
          <el-tag v-for="tag in row.tags" :key="tag.id" size="small" class="tag">{{ tag.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.article.status === 1 ? 'success' : 'info'">
            {{ row.article.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="article.createTime" label="创建时间" width="170" :formatter="formatDateCell" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="router.push(`/author-console/articles/edit/${row.article.id}`)">编辑</el-button>
          <el-button size="small" :type="row.article.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row.article.id)">
            {{ row.article.status === 1 ? '下架' : '发布' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.article.id)">删除</el-button>
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
      @current-change="loadArticles"
    />
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAuthorArticle, getAuthorArticles, toggleAuthorArticleStatus } from '@/api/authorConsole'
import { getBlogCategories, getBlogTags } from '@/api/blog'
import { categoryCascaderOptions } from '@/utils/categoryTree'
import { formatDateCell } from '@/utils/format'

const router = useRouter()
const articles = ref([])
const categories = ref([])
const tags = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 10
const filterCategoryId = ref(null)
const filterTagId = ref(null)
const loading = ref(false)
const loadError = ref('')
let latestRequestId = 0
const categoryOptions = computed(() => categoryCascaderOptions(categories.value))

async function loadArticles() {
  const requestId = ++latestRequestId
  loading.value = true
  loadError.value = ''
  try {
    const params = { pageNum: pageNum.value, pageSize }
    if (filterCategoryId.value) params.categoryId = filterCategoryId.value
    if (filterTagId.value) params.tagId = filterTagId.value
    const response = await getAuthorArticles(params)
    if (requestId !== latestRequestId) return
    if (response.data.records.length === 0 && pageNum.value > 1 && response.data.total > 0) {
      pageNum.value = Math.ceil(response.data.total / pageSize)
      return loadArticles()
    }
    articles.value = response.data.records
    total.value = response.data.total
  } catch {
    if (requestId === latestRequestId) loadError.value = '文章加载失败，请检查网络后重试'
  } finally {
    if (requestId === latestRequestId) loading.value = false
  }
}

function handleFilterChange() {
  pageNum.value = 1
  loadArticles()
}

async function toggleStatus(id) {
  await toggleAuthorArticleStatus(id)
  ElMessage.success('状态切换成功')
  loadArticles()
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该文章？', '提示', { type: 'warning' })
  await deleteAuthorArticle(id)
  ElMessage.success('删除成功')
  loadArticles()
}

onMounted(async () => {
  const articlesPromise = loadArticles()
  const [categoryResult, tagResult] = await Promise.allSettled([getBlogCategories(), getBlogTags()])
  if (categoryResult.status === 'fulfilled') categories.value = categoryResult.value.data
  if (tagResult.status === 'fulfilled') tags.value = tagResult.value.data
  await articlesPromise
})
</script>

<style scoped>
.article-list-card { min-width: 0; padding: 24px; border: 1px solid #e5eaf0; border-radius: 12px; background: #fff; box-shadow: 0 8px 24px rgba(31, 42, 58, 0.04); }
.list-heading { margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between; gap: 16px; }
h2 { margin: 0 0 5px; color: #1f2a3a; font-size: 22px; }
p { margin: 0; color: #8490a0; font-size: 13px; }
.filters { margin-bottom: 16px; display: flex; gap: 10px; }
.load-error { margin-bottom: 16px; display: flex; align-items: center; gap: 12px; }
.load-error .el-alert { flex: 1; }
.filters :deep(.el-cascader), .filters :deep(.el-select) { width: 210px; }
.tag { margin: 2px; }
.pagination { margin-top: 20px; justify-content: center; }
@media (max-width: 720px) { .article-list-card { padding: 16px; } .filters { flex-direction: column; } .filters :deep(.el-cascader), .filters :deep(.el-select) { width: 100%; } }
</style>
