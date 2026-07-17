<template>
  <div class="blog-home">
    <section class="list-toolbar" aria-label="文章列表筛选与排序">
      <div class="toolbar-heading">
        <span class="toolbar-title">{{ toolbarTitle }}</span>
        <span class="toolbar-count">共 {{ total }} 篇</span>
      </div>
      <div class="toolbar-controls">
        <label class="control-group">
          <span>分类</span>
          <el-select v-model="selectedCategory" placeholder="全部分类" aria-label="按分类筛选" @change="applyFilters">
            <el-option label="全部分类" value="" />
            <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="String(category.id)" />
          </el-select>
        </label>
        <div class="control-group sort-control">
          <span>排序</span>
          <div class="sort-switch" role="group" aria-label="文章排序">
            <button
              v-for="option in sortOptions"
              :key="option.value"
              type="button"
              :class="['sort-option', { active: selectedSort === option.value }]"
              :aria-pressed="selectedSort === option.value"
              @click="selectSort(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
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
const sortOptions = [
  { label: '最新发布', value: 'latest' },
  { label: '阅读最多', value: 'mostViewed' },
]
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

function selectSort(sort) {
  if (selectedSort.value === sort) return
  selectedSort.value = sort
  applyFilters()
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
  margin-bottom: var(--blog-space-4);
  padding: 14px var(--blog-space-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--blog-space-4);
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}
.toolbar-heading {
  display: flex;
  align-items: baseline;
  gap: var(--blog-space-2);
  white-space: nowrap;
}
.toolbar-title {
  color: var(--blog-color-text);
  font-size: 17px;
  font-weight: 600;
}
.toolbar-count {
  padding: 3px 9px;
  border-radius: 999px;
  color: var(--blog-color-text-muted);
  background: var(--blog-color-background);
  font-size: 13px;
}
.toolbar-controls {
  display: flex;
  align-items: center;
  gap: var(--blog-space-4);
}
.control-group {
  display: flex;
  align-items: center;
  gap: var(--blog-space-2);
  color: var(--blog-color-text-secondary);
  font-size: 13px;
}
.control-group :deep(.el-select) {
  width: 132px;
}
.control-group :deep(.el-select__wrapper) {
  min-height: 38px;
}
.sort-switch {
  padding: 3px;
  display: inline-flex;
  border: 1px solid var(--blog-color-border);
  border-radius: 8px;
  background: var(--blog-color-background);
}
.sort-option {
  min-height: 30px;
  padding: 0 12px;
  border: 0;
  border-radius: 5px;
  color: var(--blog-color-text-muted);
  background: transparent;
  cursor: pointer;
  font: inherit;
  white-space: nowrap;
  transition: color 0.16s ease, background-color 0.16s ease, box-shadow 0.16s ease;
}
.sort-option:hover {
  color: var(--blog-color-primary);
}
.sort-option.active {
  color: var(--blog-color-primary);
  background: var(--blog-color-surface);
  box-shadow: 0 1px 4px rgba(31, 45, 61, 0.1);
  font-weight: 600;
}
.sort-option:focus-visible {
  outline: 3px solid rgba(51, 126, 204, 0.24);
  outline-offset: 1px;
}
.loading-box,
.empty-box {
  padding: 40px;
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}
.error-alert {
  margin-bottom: var(--blog-space-4);
}
.pagination-box {
  padding: 24px 0;
  display: flex;
  justify-content: center;
}
.search-result-heading {
  margin-bottom: var(--blog-space-4);
  padding: var(--blog-space-4);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--blog-space-4);
  border: 1px solid var(--blog-color-border-hover);
  border-radius: var(--blog-radius-card);
  color: var(--blog-color-text);
  background: var(--blog-color-primary-soft);
  font-size: 14px;
}
.search-result-heading > div {
  min-width: 0;
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: var(--blog-space-2);
}
.search-eyebrow {
  color: var(--blog-color-primary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}
.search-result-heading strong {
  max-width: 100%;
  overflow: hidden;
  color: var(--blog-color-text);
  font-size: 16px;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.search-count {
  color: var(--blog-color-text-muted);
}
.clear-search-button {
  flex-shrink: 0;
  padding: 6px 10px;
  border: 0;
  border-radius: var(--blog-radius-button);
  color: var(--blog-color-primary);
  background: transparent;
  cursor: pointer;
  font: inherit;
}
.clear-search-button:hover {
  background: var(--blog-color-primary-soft);
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
    align-items: stretch;
    flex-direction: column;
  }
  .control-group {
    min-width: 0;
    flex: 1;
  }
  .control-group :deep(.el-select) {
    min-width: 0;
    width: 100%;
  }
  .sort-switch {
    flex: 1;
  }
  .sort-option {
    flex: 1;
  }
}

@media (max-width: 430px) {
  .control-group span {
    width: 32px;
  }
  .search-result-heading {
    align-items: flex-start;
  }
}
</style>
