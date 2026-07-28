<template>
  <div class="blog-home">
    <!-- 筛选排序工具栏 -->
    <section class="list-toolbar" aria-label="文章列表筛选与排序">
      <div class="current-author" aria-label="当前文章拥有者">
        <img
          :src="avatarUrl()"
          :alt="`${SITE_OWNER_NAME}头像`"
          class="current-author-avatar"
          @error="useDefaultAvatar"
        />
        <span class="current-author-copy">
          <span class="current-author-label">当前作者</span>
          <strong>{{ SITE_OWNER_NAME }}</strong>
        </span>
        <span class="current-author-count">{{ total }}篇</span>
      </div>
      <div class="toolbar-actions">
        <form class="article-search" role="search" @submit.prevent="submitSearch">
          <el-icon class="article-search-icon"><Search /></el-icon>
          <input
            v-model="searchDraft"
            type="search"
            class="article-search-input"
            placeholder="搜索当前作者的文章"
            aria-label="搜索当前作者的文章"
            maxlength="100"
            autocomplete="off"
            @search="submitSearch"
          />
          <button type="submit" class="search-submit">搜索</button>
        </form>
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
    </section>

    <!-- 加载中骨架屏 -->
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="5" animated />
    </div>
    <template v-else>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" class="error-alert" />
      <!-- 搜索结果提示区域 -->
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
        <BlogArticleCard
          v-for="item in articles"
          :key="item.article.id"
          :item="item"
          context-root-path="/blog"
        />

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
/** 博客首页 - 展示管理员公开文章列表，支持搜索和多种排序方式 */
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import BlogArticleCard from '@/components/blog/BlogArticleCard.vue'
import { getBlogArticles } from '@/api/blog'
import { SITE_OWNER_ID, SITE_OWNER_NAME } from '@/config/site'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'

const router = useRouter()
const route = useRoute()
const articles = ref([])
const loading = ref(true)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const errorMessage = ref('')
const activeKeyword = ref('')
const searchDraft = ref('')
const selectedSort = ref('learning')

/** 排序选项配置 */
const sortOptions = [
  { label: '学习顺序', value: 'learning' },
  { label: '最新发布', value: 'latest' },
  { label: '阅读最多', value: 'mostViewed' },
]


/** 加载文章列表数据 */
async function fetchArticles() {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      sort: selectedSort.value,
      authorId: SITE_OWNER_ID,
    }
    if (activeKeyword.value) params.keyword = activeKeyword.value
    if (route.query.categoryId) params.categoryId = route.query.categoryId
    if (route.query.tagId) params.tagId = route.query.tagId

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

/** 构建路由查询参数 */
function buildQuery({ includeKeyword = true } = {}) {
  const query = {}
  if (includeKeyword && activeKeyword.value) query.keyword = activeKeyword.value
  if (selectedSort.value !== 'learning') query.sort = selectedSort.value
  if (route.query.categoryId) query.categoryId = route.query.categoryId
  if (route.query.tagId) query.tagId = route.query.tagId
  return query
}

/** 应用筛选条件到 URL */
function applyFilters() {
  router.push({ path: '/blog', query: buildQuery() })
}

/** 提交搜索关键词，并与当前排序共同写入 URL。 */
function submitSearch() {
  const keyword = searchDraft.value.trim().slice(0, 100)
  searchDraft.value = keyword
  const query = buildQuery({ includeKeyword: false })
  if (keyword) query.keyword = keyword
  router.push({ path: '/blog', query })
}

/** 切换排序方式 */
function selectSort(sort) {
  if (selectedSort.value === sort) return
  selectedSort.value = sort
  applyFilters()
}

/** 清除搜索关键词 */
function clearSearch() {
  router.push({ path: '/blog', query: buildQuery({ includeKeyword: false }) })
}

/**
 * 监听 URL 查询参数变化，同步筛选状态
 * URL 查询参数是筛选条件的事实来源，确保刷新、前进后退和分享链接都能恢复同一列表状态
 */
watch(
  () => [route.query.keyword, route.query.sort, route.query.categoryId, route.query.tagId],
  ([keyword, sort]) => {
    activeKeyword.value = typeof keyword === 'string' ? keyword.trim().slice(0, 100) : ''
    searchDraft.value = activeKeyword.value
    selectedSort.value = ['latest', 'mostViewed'].includes(sort) ? sort : 'learning'
    pageNum.value = 1
    fetchArticles()
  },
  { immediate: true },
)

</script>

<style scoped>
.list-toolbar {
  margin-bottom: var(--blog-space-4);
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid var(--blog-color-border);
  border-radius: 14px;
  background: var(--blog-color-surface);
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.035);
}
.current-author {
  min-width: 152px;
  padding: 2px 14px 2px 2px;
  display: flex;
  align-items: center;
  gap: 9px;
  flex-shrink: 0;
  color: var(--blog-color-text);
  text-decoration: none;
}
.current-author-avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  border: 2px solid #fff;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(31, 45, 61, 0.12);
}
.current-author-copy {
  min-width: 0;
  display: grid;
  gap: 1px;
}
.current-author-label {
  color: var(--blog-color-text-muted);
  font-size: 11px;
  line-height: 1.2;
}
.current-author-copy strong {
  max-width: 72px;
  overflow: hidden;
  font-size: 14px;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.current-author-count {
  align-self: flex-end;
  margin-bottom: 4px;
  color: var(--blog-color-text-muted);
  font-size: 11px;
  white-space: nowrap;
}
.current-author:hover strong {
  color: var(--blog-color-primary);
}
.current-author:focus-visible {
  border-radius: 9px;
  outline: 3px solid rgba(51, 126, 204, 0.24);
  outline-offset: 2px;
}
.toolbar-actions {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  flex: 1;
}
.article-search {
  height: 46px;
  min-width: 0;
  padding: 4px;
  display: flex;
  align-items: center;
  width: min(100%, 400px);
  border: 1px solid #dbe5f0;
  border-radius: 12px;
  background: #f8fafc;
  box-sizing: border-box;
  transition: border-color 0.18s ease, background-color 0.18s ease, box-shadow 0.18s ease;
}
.article-search:hover {
  border-color: #c7d6e7;
  background: #fff;
}
.article-search:focus-within {
  border-color: var(--blog-color-primary);
  background: #fff;
  box-shadow: 0 0 0 3px rgba(51, 126, 204, 0.12);
}
.article-search-icon {
  margin-left: 9px;
  flex: 0 0 auto;
  color: #8492a6;
  font-size: 16px;
}
.article-search-input {
  min-width: 0;
  height: 100%;
  padding: 0 12px 0 9px;
  flex: 1;
  border: 0;
  outline: 0;
  color: var(--blog-color-text);
  background: transparent;
  font: inherit;
  font-size: 14px;
}
.article-search-input::placeholder {
  color: #98a4b3;
}
.article-search-input::-webkit-search-cancel-button {
  opacity: 0.55;
  cursor: pointer;
}
.search-submit {
  min-width: 76px;
  height: 36px;
  padding: 0 17px;
  border: 0;
  border-radius: 9px;
  color: #fff;
  background: linear-gradient(135deg, #3d8bd8, #2d73bf);
  box-shadow: 0 4px 10px rgba(45, 115, 191, 0.2);
  cursor: pointer;
  font: inherit;
  font-weight: 600;
  transition: transform 0.16s ease, box-shadow 0.16s ease, filter 0.16s ease;
}
.search-submit:hover {
  filter: brightness(1.04);
  box-shadow: 0 5px 13px rgba(45, 115, 191, 0.28);
  transform: translateY(-1px);
}
.search-submit:active {
  box-shadow: 0 2px 6px rgba(45, 115, 191, 0.2);
  transform: translateY(0);
}
.search-submit:focus-visible {
  outline: 3px solid rgba(51, 126, 204, 0.28);
  outline-offset: -3px;
}
.sort-switch {
  height: 46px;
  padding: 4px;
  display: inline-flex;
  box-sizing: border-box;
  border: 1px solid var(--blog-color-border);
  border-radius: 8px;
  background: var(--blog-color-background);
}
.sort-option {
  min-height: 36px;
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
    flex-direction: column;
    align-items: stretch;
  }
  .current-author {
    width: 100%;
    padding: 2px 2px 10px;
    border-right: 0;
    border-bottom: 1px solid var(--blog-color-border);
    box-sizing: border-box;
  }
  .current-author-count {
    margin-left: auto;
  }
  .toolbar-actions {
    width: 100%;
    flex-direction: column;
  }
  .article-search,
  .toolbar-actions {
    width: 100%;
  }
  .sort-switch {
    width: 100%;
    flex: 1;
  }
  .sort-option {
    flex: 1;
  }
}

@media (max-width: 430px) {
  .search-result-heading {
    align-items: flex-start;
  }
}
</style>
