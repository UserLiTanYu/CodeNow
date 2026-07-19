<template>
  <div class="author-page">
    <div v-if="loadingProfile" class="state-panel">正在加载作者主页…</div>
    <div v-else-if="profileError || !author" class="state-panel error-state" role="alert">
      <strong>作者主页不可用</strong>
      <span>该作者可能已暂停公开展示，或页面暂时无法访问。</span>
      <router-link to="/blog/authors">返回作者发现</router-link>
    </div>
    <template v-else>
      <!-- 作者资料头 -->
      <section class="author-hero">
        <img :src="avatarUrl(author.avatar)" :alt="`${author.displayName}头像`" class="hero-avatar" @error="useDefaultAvatar" />
        <div class="hero-info">
          <span class="eyebrow">AUTHOR</span>
          <h1>{{ author.displayName }}</h1>
          <p class="hero-bio">{{ author.bio }}</p>
          <div class="hero-meta">
            <span class="stat"><strong>{{ author.articleCount || 0 }}</strong> 篇文章</span>
            <span class="stat"><strong>{{ formatNumber(author.totalViews) }}</strong> 次阅读</span>
          </div>
          <div v-if="author.expertise?.length" class="expertise-list">
            <span v-for="item in author.expertise" :key="item">{{ item }}</span>
          </div>
          <div v-if="safeWebsite || safePortfolio" class="hero-links">
            <a v-if="safeWebsite" :href="safeWebsite" target="_blank" rel="noopener noreferrer nofollow">个人网站</a>
            <a v-if="safePortfolio" :href="safePortfolio" target="_blank" rel="noopener noreferrer nofollow">作品集</a>
          </div>
        </div>
      </section>

      <!-- 筛选排序 -->
      <section class="list-toolbar" aria-label="作者文章筛选">
        <div class="toolbar-heading">
          <span class="toolbar-title">{{ filterLabel }}</span>
          <span class="toolbar-count">共 {{ articleTotal }} 篇</span>
        </div>
        <div class="toolbar-controls">
          <el-select v-model="selectedCategoryId" placeholder="全部分类" clearable size="small" @change="onFilterChange">
            <el-option v-for="cat in authorCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
          <el-select v-model="selectedTagId" placeholder="全部标签" clearable size="small" @change="onFilterChange">
            <el-option v-for="tag in authorTags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
          <div class="sort-switch" role="group" aria-label="排序">
            <button type="button" :class="{ active: articleSort === 'latest' }" @click="selectSort('latest')">最新发布</button>
            <button type="button" :class="{ active: articleSort === 'mostViewed' }" @click="selectSort('mostViewed')">阅读最多</button>
          </div>
        </div>
      </section>

      <!-- 文章列表 -->
      <div v-if="loadingArticles" class="loading-box"><el-skeleton :rows="3" animated /></div>
      <div v-else-if="articleError" class="state-panel small error-state" role="alert">
        <strong>文章列表加载失败</strong>
        <button type="button" @click="fetchArticles">重新加载</button>
      </div>
      <div v-else-if="articles.length === 0" class="empty-box">
        <el-empty description="暂无符合条件的文章" />
      </div>
      <template v-else>
        <BlogArticleCard v-for="item in articles" :key="item.article.id" :item="item" />
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
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import BlogArticleCard from '@/components/blog/BlogArticleCard.vue'
import { getPublicAuthor, getPublicAuthorArticles, getPublicAuthorCategories, getPublicAuthorTags } from '@/api/blog'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'

const route = useRoute()
const author = ref(null)
const articles = ref([])
const articleTotal = ref(0)
const pageNum = ref(1)
const pageSize = 10
const articleSort = ref('latest')
const selectedCategoryId = ref(null)
const selectedTagId = ref(null)
const authorCategories = ref([])
const authorTags = ref([])
const loadingProfile = ref(true)
const loadingArticles = ref(false)
const profileError = ref(false)
const articleError = ref(false)
let profileRequestId = 0
let articleRequestId = 0

function safeExternalUrl(value) {
  if (typeof value !== 'string' || !/^https?:\/\//i.test(value)) return ''
  return value
}

const safeWebsite = computed(() => safeExternalUrl(author.value?.websiteUrl))
const safePortfolio = computed(() => safeExternalUrl(author.value?.portfolioUrl))

const filterLabel = computed(() => {
  const cat = authorCategories.value.find(c => c.id === selectedCategoryId.value)
  const tag = authorTags.value.find(t => t.id === selectedTagId.value)
  if (cat) return cat.name
  if (tag) return tag.name
  return '全部文章'
})

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

async function fetchArticles() {
  if (!author.value) return
  const currentRequest = ++articleRequestId
  loadingArticles.value = true
  articleError.value = false
  try {
    const params = { pageNum: pageNum.value, pageSize, sort: articleSort.value }
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

async function fetchAuthorFilters() {
  try {
    const [catRes, tagRes] = await Promise.all([
      getPublicAuthorCategories(route.params.id),
      getPublicAuthorTags(route.params.id),
    ])
    authorCategories.value = catRes.data || []
    authorTags.value = tagRes.data || []
  } catch {
    authorCategories.value = []
    authorTags.value = []
  }
}

async function fetchProfile() {
  const currentRequest = ++profileRequestId
  ++articleRequestId
  loadingProfile.value = true
  profileError.value = false
  author.value = null
  articles.value = []
  articleTotal.value = 0
  selectedCategoryId.value = null
  selectedTagId.value = null
  try {
    const response = await getPublicAuthor(route.params.id)
    if (currentRequest !== profileRequestId) return
    author.value = response.data
    document.title = `${author.value.displayName} - 码上记`
    await Promise.all([fetchArticles(), fetchAuthorFilters()])
  } catch {
    if (currentRequest !== profileRequestId) return
    profileError.value = true
  } finally {
    if (currentRequest === profileRequestId) loadingProfile.value = false
  }
}

function selectSort(sort) {
  if (articleSort.value === sort) return
  articleSort.value = sort
  pageNum.value = 1
  fetchArticles()
}

function onFilterChange() {
  pageNum.value = 1
  fetchArticles()
}

watch(
  () => route.params.id,
  () => {
    pageNum.value = 1
    articleSort.value = 'latest'
    fetchProfile()
  },
  { immediate: true },
)
</script>

<style scoped>
.author-page { display: grid; gap: var(--blog-space-4); }

/* 作者资料头 */
.author-hero {
  padding: 24px 28px; display: flex; gap: 20px; align-items: center;
  border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card);
  background: linear-gradient(135deg, #f7fbff 0%, var(--blog-color-surface) 66%, #f3f8ff 100%);
}
.hero-avatar { width: 72px; height: 72px; flex-shrink: 0; border-radius: 50%; object-fit: cover; border: 3px solid #fff; box-shadow: 0 4px 12px rgba(41,72,108,.1); }
.hero-info { min-width: 0; }
.eyebrow { color: var(--blog-color-primary); font-size: 11px; font-weight: 700; letter-spacing: .16em; }
.hero-info h1 { margin: 2px 0 6px; color: var(--blog-color-text); font-size: 24px; }
.hero-bio { margin: 0 0 8px; color: var(--blog-color-text-secondary); font-size: 14px; line-height: 1.6; }
.hero-meta { display: flex; gap: 20px; margin-bottom: 8px; }
.stat { color: var(--blog-color-text-muted); font-size: 13px; }
.stat strong { color: var(--blog-color-text); font-size: 16px; margin-right: 3px; }
.expertise-list { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 8px; }
.expertise-list span { padding: 3px 10px; border-radius: 999px; color: var(--blog-color-primary); background: var(--blog-color-primary-soft); font-size: 12px; }
.hero-links { display: flex; gap: 14px; }
.hero-links a { color: var(--blog-color-primary); font-size: 13px; text-decoration: none; }

/* 筛选排序工具栏 */
.list-toolbar {
  padding: 12px 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px;
  border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: var(--blog-color-surface);
}
.toolbar-heading { display: flex; align-items: baseline; gap: 10px; }
.toolbar-title { color: var(--blog-color-text); font-size: 16px; font-weight: 600; }
.toolbar-count { color: var(--blog-color-text-muted); font-size: 13px; }
.toolbar-controls { display: flex; align-items: center; gap: 10px; }
.sort-switch { padding: 3px; display: flex; border: 1px solid var(--blog-color-border); border-radius: 8px; background: var(--blog-color-background); }
.sort-switch button { min-height: 28px; padding: 0 10px; border: 0; border-radius: 5px; color: var(--blog-color-text-muted); background: transparent; cursor: pointer; font-size: 13px; }
.sort-switch button.active { color: var(--blog-color-primary); background: var(--blog-color-surface); box-shadow: 0 1px 4px rgba(31,45,61,.1); font-weight: 600; }

/* 状态 */
.loading-box, .empty-box { padding: 32px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: var(--blog-color-surface); }
.state-panel { min-height: 200px; padding: 32px; display: grid; place-content: center; gap: 10px; text-align: center; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); color: var(--blog-color-text-muted); background: var(--blog-color-surface); }
.state-panel.small { min-height: 120px; }
.error-state strong { color: var(--blog-color-text); font-size: 17px; }
.error-state a, .error-state button { justify-self: center; padding: 8px 14px; border: 0; border-radius: 8px; color: #fff; background: var(--blog-color-primary); cursor: pointer; text-decoration: none; }
.pagination-box { display: flex; justify-content: center; padding: 18px 0; }
a:focus-visible, button:focus-visible { outline: 3px solid rgba(51,126,204,.24); outline-offset: 2px; }

@media (max-width: 760px) {
  .author-hero { flex-direction: column; align-items: flex-start; }
  .hero-avatar { width: 56px; height: 56px; }
  .hero-info h1 { font-size: 20px; }
  .list-toolbar { flex-direction: column; align-items: stretch; }
  .toolbar-controls { flex-wrap: wrap; }
}
</style>
