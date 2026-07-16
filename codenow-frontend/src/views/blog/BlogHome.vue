<template>
  <div class="blog-home">
    <section class="list-toolbar" aria-label="文章列表筛选与排序">
      <div class="toolbar-heading">
        <span class="toolbar-title">{{ toolbarTitle }}</span>
        <span class="toolbar-count">{{ total }} 篇</span>
      </div>
      <div class="toolbar-controls">
        <label class="control-group">
          <span>分类</span>
          <el-select v-model="selectedCategory" placeholder="全部分类" aria-label="按分类筛选" @change="applyFilters">
            <el-option label="全部分类" value="" />
            <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="String(category.id)" />
          </el-select>
        </label>
        <label class="control-group">
          <span>排序</span>
          <el-select v-model="selectedSort" aria-label="文章排序" @change="applyFilters">
            <el-option label="最新发布" value="latest" />
            <el-option label="最多阅读" value="mostViewed" />
          </el-select>
        </label>
      </div>
    </section>

    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="5" animated />
    </div>
    <template v-else>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" class="error-alert" />
      <div v-if="activeKeyword" class="search-result-heading" aria-live="polite">
        <div>
          <span class="search-eyebrow">搜索结果</span>
          <strong>“{{ activeKeyword }}”</strong>
          <span class="search-count">{{ total }} 篇文章</span>
        </div>
        <button type="button" class="clear-search-button" @click="clearSearch">清除搜索</button>
      </div>
      <div v-if="articles.length === 0" class="empty-box">
        <el-empty :description="activeKeyword ? `没有找到与“${activeKeyword}”相关的文章` : '当前筛选条件下暂无文章'" />
      </div>
      <template v-else>
        <BlogArticleCard v-for="item in articles" :key="item.article.id" :item="item" />

        <nav v-if="total > pageSize" class="pagination-box" aria-label="文章分页">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="fetchArticles"
          />
        </nav>
      </template>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BlogArticleCard from '@/components/blog/BlogArticleCard.vue'
import { getBlogArticles, getBlogCategories } from '@/api/blog'

const router = useRouter()
const route = useRoute()
const articles = ref([])
const categories = ref([])
const loading = ref(true)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const errorMessage = ref('')
const activeKeyword = ref('')
const selectedCategory = ref('')
const selectedSort = ref('latest')
const toolbarTitle = computed(() => {
  if (!selectedCategory.value) return '全部文章'
  return categories.value.find((category) => String(category.id) === selectedCategory.value)?.name || '分类文章'
})

async function fetchArticles() {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sort: selectedSort.value,
    }
    if (activeKeyword.value) params.keyword = activeKeyword.value
    if (selectedCategory.value) params.categoryId = selectedCategory.value

    const res = await getBlogArticles(params)
    articles.value = res.data.records
    total.value = res.data.total
  } catch {
    articles.value = []
    total.value = 0
    errorMessage.value = '文章列表加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

function buildQuery({ includeKeyword = true } = {}) {
  const query = {}
  if (includeKeyword && activeKeyword.value) query.keyword = activeKeyword.value
  if (selectedCategory.value) query.categoryId = selectedCategory.value
  if (selectedSort.value !== 'latest') query.sort = selectedSort.value
  return query
}

function applyFilters() {
  router.push({ path: '/blog', query: buildQuery() })
}

function clearSearch() {
  router.push({ path: '/blog', query: buildQuery({ includeKeyword: false }) })
}

async function fetchCategories() {
  try {
    const res = await getBlogCategories()
    categories.value = res.data
  } catch {
    categories.value = []
  }
}

watch(
  () => [route.query.keyword, route.query.categoryId, route.query.sort],
  ([keyword, categoryId, sort]) => {
    activeKeyword.value = typeof keyword === 'string' ? keyword.trim().slice(0, 100) : ''
    selectedCategory.value = typeof categoryId === 'string' ? categoryId : ''
    selectedSort.value = sort === 'mostViewed' ? 'mostViewed' : 'latest'
    pageNum.value = 1
    fetchArticles()
  },
  { immediate: true },
)

onMounted(fetchCategories)
</script>

<style scoped>
.list-toolbar {
  margin-bottom: 16px;
  padding: 15px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}
.toolbar-heading {
  display: flex;
  align-items: baseline;
  gap: 8px;
  white-space: nowrap;
}
.toolbar-title {
  color: #303133;
  font-size: 17px;
  font-weight: 600;
}
.toolbar-count {
  color: #a0a6af;
  font-size: 13px;
}
.toolbar-controls {
  display: flex;
  align-items: center;
  gap: 14px;
}
.control-group {
  display: flex;
  align-items: center;
  gap: 7px;
  color: #7d8592;
  font-size: 13px;
}
.control-group :deep(.el-select) {
  width: 132px;
}
.loading-box,
.empty-box {
  padding: 40px;
  border-radius: 10px;
  background: #fff;
}
.error-alert {
  margin-bottom: 16px;
}
.pagination-box {
  padding: 24px 0;
  display: flex;
  justify-content: center;
}
.search-result-heading {
  margin-bottom: 16px;
  padding: 15px 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  border: 1px solid #d9ecff;
  border-radius: 9px;
  color: #303133;
  background: #f4f9ff;
  font-size: 14px;
}
.search-result-heading > div {
  min-width: 0;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 8px;
}
.search-eyebrow {
  color: #337ecc;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}
.search-result-heading strong {
  max-width: 100%;
  overflow: hidden;
  color: #1f2d3d;
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.search-count {
  color: #909399;
}
.clear-search-button {
  flex-shrink: 0;
  padding: 6px 10px;
  border: 0;
  border-radius: 6px;
  color: #337ecc;
  background: transparent;
  cursor: pointer;
  font: inherit;
}
.clear-search-button:hover {
  background: #e6f2ff;
}
.clear-search-button:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.3);
  outline-offset: 2px;
}

@media (max-width: 700px) {
  .list-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }
  .toolbar-controls {
    width: 100%;
  }
  .control-group {
    min-width: 0;
    flex: 1;
  }
  .control-group :deep(.el-select) {
    min-width: 0;
    width: 100%;
  }
}

@media (max-width: 430px) {
  .toolbar-controls {
    align-items: stretch;
    flex-direction: column;
  }
  .control-group span {
    width: 32px;
  }
  .search-result-heading {
    align-items: flex-start;
  }
}
</style>
