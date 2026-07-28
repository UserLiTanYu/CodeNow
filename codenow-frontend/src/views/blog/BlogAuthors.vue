<template>
  <div class="authors-page">
    <!-- 搜索和排序工具栏 -->
    <section class="authors-toolbar" aria-label="作者筛选与排序">
      <form role="search" class="author-search" @submit.prevent="submitSearch">
        <input
          v-model="searchInput"
          type="search"
          maxlength="100"
          name="author-keyword"
          aria-label="搜索作者或擅长领域"
          placeholder="搜索作者、简介或擅长领域"
        />
        <button type="submit">搜索</button>
      </form>
      <div class="sort-switch" role="group" aria-label="作者排序">
        <button type="button" :class="{ active: selectedSort === 'popular' }" @click="selectSort('popular')">热门作者</button>
        <button type="button" :class="{ active: selectedSort === 'latest' }" @click="selectSort('latest')">最近活跃</button>
        <button type="button" :class="{ active: selectedSort === 'articles' }" @click="selectSort('articles')">文章最多</button>
      </div>
    </section>

    <!-- 加载状态和错误提示 -->
    <div v-if="loading" class="state-panel" aria-live="polite">正在加载作者…</div>
    <div v-else-if="errorMessage" class="state-panel error-state" role="alert">
      <strong>作者列表加载失败</strong>
      <span>{{ errorMessage }}</span>
      <button type="button" @click="fetchAuthors">重新加载</button>
    </div>
    <div v-else-if="authors.length === 0" class="state-panel">暂无符合条件的作者</div>
    <!-- 作者卡片网格列表 -->
    <section v-else class="author-grid" aria-label="作者列表">
      <router-link
        v-for="author in authors"
        :key="author.userId"
        :to="`/blog/author/${author.userId}`"
        class="author-card"
      >
        <div class="author-card-head">
          <img :src="avatarUrl(author.avatar)" :alt="`${author.displayName}头像`" loading="lazy" @error="useDefaultAvatar" />
          <div>
            <h2>{{ author.displayName }}</h2>
            <span>{{ author.articleCount || 0 }} 篇文章 · {{ formatNumber(author.totalViews) }} 次阅读</span>
          </div>
        </div>
        <p>{{ author.bio }}</p>
        <div class="expertise-list">
          <span v-for="item in (author.expertise || []).slice(0, 4)" :key="item">{{ item }}</span>
        </div>
        <span class="view-profile">查看作者主页 <b>→</b></span>
      </router-link>
    </section>

    <nav v-if="total > pageSize" class="pagination-box" aria-label="作者分页">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="fetchAuthors"
      />
    </nav>
  </div>
</template>

<script setup>
/** 作者发现页面 - 展示所有作者列表，支持搜索和多种排序方式 */
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPublicAuthors } from '@/api/blog'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'

const route = useRoute()
const router = useRouter()
const authors = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 12
const loading = ref(true)
const errorMessage = ref('')
const searchInput = ref('')
const activeKeyword = ref('')
const selectedSort = ref('popular')
/** 搜索、排序和分页可能连续触发请求，用序号丢弃晚到的旧响应 */
let requestId = 0

/** 数字格式化为千分位分隔 */
function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

/** 加载作者列表数据 */
async function fetchAuthors() {
  const currentRequest = ++requestId
  loading.value = true
  errorMessage.value = ''
  const params = { pageNum: pageNum.value, pageSize, sort: selectedSort.value }
  if (activeKeyword.value) params.keyword = activeKeyword.value
  try {
    const response = await getPublicAuthors(params)
    if (currentRequest !== requestId) return
    authors.value = response.data.records || []
    total.value = Number(response.data.total || 0)
  } catch {
    if (currentRequest !== requestId) return
    authors.value = []
    total.value = 0
    errorMessage.value = '请检查网络后重试'
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

/** 构建路由查询参数 */
function routeQuery(keyword = activeKeyword.value, sort = selectedSort.value) {
  const query = {}
  if (keyword) query.keyword = keyword
  if (sort !== 'popular') query.sort = sort
  return query
}

/** 提交搜索，更新 URL 查询参数 */
function submitSearch() {
  const keyword = searchInput.value.trim().slice(0, 100)
  searchInput.value = keyword
  router.push({ path: '/blog/authors', query: routeQuery(keyword) })
}

/** 切换排序方式 */
function selectSort(sort) {
  if (selectedSort.value === sort) return
  router.push({ path: '/blog/authors', query: routeQuery(activeKeyword.value, sort) })
}

/** 监听 URL 查询参数变化，同步搜索和排序状态 */
watch(
  () => [route.query.keyword, route.query.sort],
  ([keyword, sort]) => {
    activeKeyword.value = typeof keyword === 'string' ? keyword.trim().slice(0, 100) : ''
    searchInput.value = activeKeyword.value
    selectedSort.value = ['popular', 'latest', 'articles'].includes(sort) ? sort : 'popular'
    pageNum.value = 1
    fetchAuthors()
  },
  { immediate: true },
)
</script>

<style scoped>
.authors-page { display: grid; gap: var(--blog-space-4); }
.authors-toolbar {
  padding: 12px 14px; display: flex; align-items: center; justify-content: space-between; gap: 16px;
  border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: var(--blog-color-surface);
}
.author-search { min-width: 0; flex: 1; display: flex; gap: 8px; }
.author-search input {
  min-width: 0; width: min(100%, 430px); height: 38px; padding: 0 13px;
  border: 1px solid var(--blog-color-border); border-radius: 8px; color: var(--blog-color-text); background: var(--blog-color-background); font: inherit;
}
.author-search button, .error-state button {
  padding: 0 16px; border: 0; border-radius: 8px; color: #fff; background: var(--blog-color-primary); cursor: pointer;
}
.sort-switch { padding: 3px; display: flex; border: 1px solid var(--blog-color-border); border-radius: 8px; background: var(--blog-color-background); }
.sort-switch button { min-height: 30px; padding: 0 12px; border: 0; border-radius: 5px; color: var(--blog-color-text-muted); background: transparent; cursor: pointer; }
.sort-switch button.active { color: var(--blog-color-primary); background: var(--blog-color-surface); box-shadow: 0 1px 4px rgba(31,45,61,.1); font-weight: 600; }
.author-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: var(--blog-space-4); }
.author-card {
  min-width: 0; padding: 20px; display: flex; flex-direction: column; gap: 13px;
  border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); color: inherit;
  background: var(--blog-color-surface); text-decoration: none; transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease;
}
.author-card:hover { transform: translateY(-2px); border-color: var(--blog-color-border-hover); box-shadow: var(--blog-shadow-hover); }
.author-card-head { display: flex; align-items: center; gap: 13px; min-width: 0; }
.author-card-head > div { min-width: 0; }
.author-card-head img { width: 54px; height: 54px; flex-shrink: 0; border-radius: 50%; object-fit: cover; background: var(--blog-color-background); }
.author-card h2 { margin: 0 0 4px; color: var(--blog-color-text); font-size: 18px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.author-card-head span { color: var(--blog-color-text-muted); font-size: 12px; }
.author-card p { min-height: 48px; margin: 0; overflow: hidden; display: -webkit-box; color: var(--blog-color-text-secondary); font-size: 14px; line-height: 1.7; -webkit-box-orient: vertical; -webkit-line-clamp: 2; }
.expertise-list span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 180px; }
.expertise-list { min-height: 25px; display: flex; flex-wrap: wrap; gap: 6px; }
.expertise-list span { padding: 4px 9px; border-radius: 999px; color: var(--blog-color-primary); background: var(--blog-color-primary-soft); font-size: 12px; }
.view-profile { margin-top: auto; color: var(--blog-color-primary); font-size: 13px; font-weight: 600; }
.view-profile b { margin-left: 3px; }
.author-card:last-child:nth-child(odd) {
  grid-column: 1 / -1;
  width: calc((100% - var(--blog-space-4)) / 2);
  justify-self: start;
  box-sizing: border-box;
}
.author-card:only-child {
  width: min(100%, 900px);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  grid-template-areas: "head expertise" "bio profile";
  align-items: center;
}
.author-card:only-child .author-card-head { grid-area: head; }
.author-card:only-child p { grid-area: bio; min-height: 0; }
.author-card:only-child .expertise-list { grid-area: expertise; justify-content: flex-end; }
.author-card:only-child .view-profile { grid-area: profile; justify-self: end; margin-top: 0; }
.state-panel { min-height: 180px; padding: 32px; display: grid; place-content: center; gap: 10px; text-align: center; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); color: var(--blog-color-text-muted); background: var(--blog-color-surface); }
.error-state strong { color: var(--blog-color-text); }
.error-state button { min-height: 34px; }
.pagination-box { display: flex; justify-content: center; padding: 18px 0; }
.author-search input:focus-visible, button:focus-visible, .author-card:focus-visible { outline: 3px solid rgba(51,126,204,.24); outline-offset: 2px; }
@media (max-width: 900px) {
  .author-grid { grid-template-columns: 1fr; }
  .author-card:last-child:nth-child(odd) { width: 100%; grid-column: auto; }
  .author-card:only-child { display: flex; }
  .author-card:only-child .expertise-list { justify-content: flex-start; }
  .author-card:only-child .view-profile { align-self: flex-start; margin-top: auto; }
}
@media (max-width: 760px) {
  .authors-toolbar { align-items: stretch; flex-direction: column; }
  .author-search input { width: 100%; }
  .sort-switch { align-self: flex-start; }
}
@media (max-width: 480px) {
  .author-search { flex-direction: column; }
  .author-search button { min-height: 38px; }
}
</style>
