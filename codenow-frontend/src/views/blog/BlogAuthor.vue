<template>
  <div class="author-page">
    <!-- 加载中状态 -->
    <div v-if="loadingProfile" class="state-panel">正在加载作者主页…</div>
    <!-- 加载失败或作者不存在时的错误状态 -->
    <div v-else-if="profileError || !author" class="state-panel error-state" role="alert">
      <strong>作者主页不可用</strong>
      <span>该作者可能已暂停公开展示，或页面暂时无法访问。</span>
      <router-link to="/blog/authors">返回作者发现</router-link>
    </div>
    <template v-else>
      <!-- 与博客主界面一致的作者、搜索和排序工具栏 -->
      <section class="list-toolbar" aria-label="作者文章筛选与排序">
        <div class="current-author" aria-label="当前文章作者">
          <img
            :src="avatarUrl(author.avatar)"
            :alt="`${author.displayName}头像`"
            class="current-author-avatar"
            @error="useDefaultAvatar"
          />
          <span class="current-author-copy">
            <span class="current-author-label">当前作者</span>
            <strong>{{ author.displayName }}</strong>
          </span>
          <span class="current-author-count">{{ articleTotal }}篇</span>
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
              :class="['sort-option', { active: articleSort === option.value }]"
              :aria-pressed="articleSort === option.value"
              @click="selectSort(option.value)"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
      </section>

      <div v-if="activeKeyword" class="search-result-heading" aria-live="polite">
        <div>
          <span class="search-eyebrow">搜索结果</span>
          <strong>“{{ activeKeyword }}”</strong>
          <span class="search-count">{{ articleTotal }} 篇文章</span>
        </div>
        <button type="button" class="clear-search-button" @click="clearSearch">清除搜索</button>
      </div>

      <!-- 文章列表区域 -->
      <div v-if="loadingArticles" class="loading-box"><el-skeleton :rows="3" animated /></div>
      <div v-else-if="articleError" class="state-panel small error-state" role="alert">
        <strong>文章列表加载失败</strong>
        <button type="button" @click="fetchArticles">重新加载</button>
      </div>
      <div v-else-if="articles.length === 0" class="empty-box">
        <el-empty description="暂无符合条件的文章" />
      </div>
      <template v-else>
        <BlogArticleCard
          v-for="item in articles"
          :key="item.article.id"
          :item="item"
          :context-author-id="route.params.id"
        />
        <nav v-if="articleTotal > pageSize" class="pagination-box" aria-label="作者文章分页">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="articleTotal"
            layout="prev, pager, next"
            @current-change="fetchArticles"
          />
        </nav>
      </template>
    </template>
  </div>
</template>

<script setup>
/** 作者公共主页 - 展示作者资料、文章列表，支持分类/标签筛选和排序 */
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import BlogArticleCard from '@/components/blog/BlogArticleCard.vue'
import { getPublicAuthor, getPublicAuthorArticles } from '@/api/blog'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()
const author = ref(null)
const articles = ref([])
const articleTotal = ref(0)
const pageNum = ref(1)
const pageSize = 10
const articleSort = ref('latest')
const selectedCategoryId = ref(null)
const selectedTagId = ref(null)
const loadingProfile = ref(true)
const loadingArticles = ref(false)
const profileError = ref(false)
const articleError = ref(false)
/** 作者资料与文章列表分别隔离竞态；切换作者时还会主动使旧文章请求失效 */
let profileRequestId = 0
let articleRequestId = 0
const activeKeyword = ref('')
const searchDraft = ref('')
const sortOptions = [
  { label: '最新发布', value: 'latest' },
  { label: '阅读最多', value: 'mostViewed' },
]

/** 加载作者文章列表，支持分类、标签筛选和排序 */
async function fetchArticles() {
  if (!author.value) return
  const currentRequest = ++articleRequestId
  loadingArticles.value = true
  articleError.value = false
  try {
    const params = { pageNum: pageNum.value, pageSize, sort: articleSort.value }
    if (activeKeyword.value) params.keyword = activeKeyword.value
    if (selectedCategoryId.value) params.categoryId = selectedCategoryId.value
    if (selectedTagId.value) params.tagId = selectedTagId.value
    const response = await getPublicAuthorArticles(route.params.id, params)
    if (currentRequest !== articleRequestId) return
    articles.value = response.data.records || []
    articleTotal.value = Number(response.data.total || 0)
  } catch {
    if (currentRequest !== articleRequestId) return
    articles.value = []
    articleTotal.value = 0
    articleError.value = true
  } finally {
    if (currentRequest === articleRequestId) loadingArticles.value = false
  }
}

/** 加载作者个人资料 */
async function fetchProfile() {
  const currentRequest = ++profileRequestId
  ++articleRequestId
  loadingProfile.value = true
  profileError.value = false
  author.value = null
  articles.value = []
  articleTotal.value = 0
  selectedCategoryId.value = route.query.categoryId || null
  selectedTagId.value = route.query.tagId || null
  try {
    const response = await getPublicAuthor(route.params.id)
    if (currentRequest !== profileRequestId) return
    author.value = response.data
    document.title = `${author.value.displayName} - 码上记`
    await fetchArticles()
  } catch {
    if (currentRequest !== profileRequestId) return
    profileError.value = true
  } finally {
    if (currentRequest === profileRequestId) loadingProfile.value = false
  }
}

/** 切换文章排序方式 */
function selectSort(sort) {
  if (articleSort.value === sort) return
  router.push({ path: route.path, query: buildQuery({ sort }) })
}

/** 保留作者分类、标签上下文并构建新的查询参数。 */
function buildQuery(overrides = {}) {
  const query = {}
  const keyword = Object.hasOwn(overrides, 'keyword') ? overrides.keyword : activeKeyword.value
  const sort = Object.hasOwn(overrides, 'sort') ? overrides.sort : articleSort.value
  if (keyword) query.keyword = keyword
  if (sort === 'mostViewed') query.sort = sort
  if (route.query.categoryId) query.categoryId = route.query.categoryId
  if (route.query.tagId) query.tagId = route.query.tagId
  return query
}

/** 提交当前作者范围内的文章搜索。 */
function submitSearch() {
  const keyword = searchDraft.value.trim().slice(0, 100)
  searchDraft.value = keyword
  router.push({ path: route.path, query: buildQuery({ keyword }) })
}

/** 清除搜索但保留排序、分类和标签筛选。 */
function clearSearch() {
  router.push({ path: route.path, query: buildQuery({ keyword: '' }) })
}

/** 监听作者及筛选参数变化；筛选变化只重载当前作者文章，不离开作者上下文。 */
watch(
  () => [route.params.id, route.query.categoryId, route.query.tagId, route.query.keyword, route.query.sort],
  ([id, categoryId, tagId, keyword, sort], previous = []) => {
    const [previousId] = previous
    pageNum.value = 1
    selectedCategoryId.value = categoryId || null
    selectedTagId.value = tagId || null
    activeKeyword.value = typeof keyword === 'string' ? keyword.trim().slice(0, 100) : ''
    searchDraft.value = activeKeyword.value
    articleSort.value = sort === 'mostViewed' ? 'mostViewed' : 'latest'
    if (!author.value || String(id) !== String(previousId ?? '')) {
      fetchProfile()
    } else {
      fetchArticles()
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.author-page { display: block; }

.list-toolbar {
  margin-bottom: var(--blog-space-4);
  padding: 10px; display: flex; align-items: center; gap: 12px;
  border: 1px solid var(--blog-color-border); border-radius: 14px;
  background: var(--blog-color-surface); box-shadow: 0 2px 10px rgba(31,45,61,.035);
}
.current-author {
  min-width: 152px; padding: 2px 14px 2px 2px; display: flex; align-items: center; gap: 9px;
  flex-shrink: 0; color: var(--blog-color-text);
}
.current-author-avatar {
  width: 38px; height: 38px; flex-shrink: 0; border: 2px solid #fff; border-radius: 50%;
  object-fit: cover; box-shadow: 0 2px 8px rgba(31,45,61,.12);
}
.current-author-copy { min-width: 0; display: grid; gap: 1px; }
.current-author-label { color: var(--blog-color-text-muted); font-size: 11px; line-height: 1.2; }
.current-author-copy strong {
  max-width: 72px; overflow: hidden; font-size: 14px; line-height: 1.35;
  text-overflow: ellipsis; white-space: nowrap;
}
.current-author-count {
  align-self: flex-end; margin-bottom: 4px; color: var(--blog-color-text-muted);
  font-size: 11px; white-space: nowrap;
}
.toolbar-actions {
  min-width: 0; display: flex; align-items: center; justify-content: flex-end; gap: 12px; flex: 1;
}
.article-search {
  width: min(100%, 400px); height: 46px; min-width: 0; padding: 4px; display: flex; align-items: center;
  border: 1px solid #dbe5f0; border-radius: 12px; background: #f8fafc; box-sizing: border-box;
  transition: border-color .18s ease, background-color .18s ease, box-shadow .18s ease;
}
.article-search:hover { border-color: #c7d6e7; background: #fff; }
.article-search:focus-within {
  border-color: var(--blog-color-primary); background: #fff; box-shadow: 0 0 0 3px rgba(51,126,204,.12);
}
.article-search-icon { margin-left: 9px; flex: 0 0 auto; color: #8492a6; font-size: 16px; }
.article-search-input {
  min-width: 0; height: 100%; padding: 0 12px 0 9px; flex: 1; border: 0; outline: 0;
  color: var(--blog-color-text); background: transparent; font: inherit; font-size: 14px;
}
.article-search-input::placeholder { color: #98a4b3; }
.search-submit {
  min-width: 76px; height: 36px; padding: 0 17px; border: 0; border-radius: 9px; color: #fff;
  background: linear-gradient(135deg, #3d8bd8, #2d73bf); box-shadow: 0 4px 10px rgba(45,115,191,.2);
  cursor: pointer; font: inherit; font-weight: 600;
}
.search-submit:hover { filter: brightness(1.04); box-shadow: 0 5px 13px rgba(45,115,191,.28); }
.sort-switch {
  height: 46px; padding: 4px; display: inline-flex; border: 1px solid var(--blog-color-border);
  border-radius: 8px; background: var(--blog-color-background); box-sizing: border-box;
}
.sort-option {
  min-height: 36px; padding: 0 12px; border: 0; border-radius: 5px; color: var(--blog-color-text-muted);
  background: transparent; cursor: pointer; font: inherit; white-space: nowrap;
}
.sort-option:hover { color: var(--blog-color-primary); }
.sort-option.active {
  color: var(--blog-color-primary); background: var(--blog-color-surface);
  box-shadow: 0 1px 4px rgba(31,45,61,.1); font-weight: 600;
}
.search-result-heading {
  margin-bottom: var(--blog-space-4);
  padding: var(--blog-space-4); display: flex; align-items: center; justify-content: space-between;
  gap: var(--blog-space-4); border: 1px solid var(--blog-color-border-hover);
  border-radius: var(--blog-radius-card); color: var(--blog-color-text); background: var(--blog-color-primary-soft);
  font-size: 14px;
}
.search-result-heading > div { min-width: 0; display: flex; align-items: baseline; flex-wrap: wrap; gap: var(--blog-space-2); }
.search-eyebrow { color: var(--blog-color-primary); font-size: 12px; font-weight: 600; letter-spacing: .08em; }
.search-result-heading strong { max-width: 100%; overflow: hidden; font-size: 16px; text-overflow: ellipsis; white-space: nowrap; }
.search-count { color: var(--blog-color-text-muted); }
.clear-search-button {
  flex-shrink: 0; padding: 6px 10px; border: 0; border-radius: var(--blog-radius-button);
  color: var(--blog-color-primary); background: transparent; cursor: pointer; font: inherit;
}
.clear-search-button:hover { background: var(--blog-color-primary-soft); }

/* 状态 */
.loading-box, .empty-box { padding: 32px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: var(--blog-color-surface); }
.state-panel { min-height: 200px; padding: 32px; display: grid; place-content: center; gap: 10px; text-align: center; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); color: var(--blog-color-text-muted); background: var(--blog-color-surface); }
.state-panel.small { min-height: 120px; }
.error-state strong { color: var(--blog-color-text); font-size: 17px; }
.error-state a, .error-state button { justify-self: center; padding: 8px 14px; border: 0; border-radius: 8px; color: #fff; background: var(--blog-color-primary); cursor: pointer; text-decoration: none; }
.pagination-box { display: flex; justify-content: center; padding: 18px 0; }
a:focus-visible, button:focus-visible { outline: 3px solid rgba(51,126,204,.24); outline-offset: 2px; }

@media (max-width: 760px) {
  .list-toolbar { flex-direction: column; align-items: stretch; }
  .current-author {
    width: 100%; padding: 2px 2px 10px; border-right: 0;
    border-bottom: 1px solid var(--blog-color-border); box-sizing: border-box;
  }
  .current-author-count { margin-left: auto; }
  .toolbar-actions { width: 100%; flex-direction: column; }
  .article-search, .toolbar-actions { width: 100%; }
  .sort-switch { width: 100%; }
  .sort-option { flex: 1; }
}
</style>
