<template>
  <div class="author-page">
    <div v-if="loadingProfile" class="state-panel">正在加载作者主页…</div>
    <div v-else-if="profileError || !author" class="state-panel error-state" role="alert">
      <strong>作者主页不可用</strong>
      <span>该作者可能已暂停公开展示，或页面暂时无法访问。</span>
      <router-link to="/blog/authors">返回作者发现</router-link>
    </div>
    <template v-else>
      <section class="profile-panel">
        <div class="profile-main">
          <img :src="avatarUrl(author.avatar)" :alt="`${author.displayName}头像`" @error="useDefaultAvatar" />
          <div class="profile-copy">
            <span class="eyebrow">PUBLIC AUTHOR</span>
            <h1>{{ author.displayName }}</h1>
            <p>{{ author.bio }}</p>
            <div class="expertise-list">
              <span v-for="item in author.expertise || []" :key="item">{{ item }}</span>
            </div>
          </div>
        </div>
        <div class="profile-side">
          <div class="stats-grid">
            <div><strong>{{ author.articleCount || 0 }}</strong><span>篇文章</span></div>
            <div><strong>{{ formatNumber(author.totalViews) }}</strong><span>次阅读</span></div>
          </div>
          <div v-if="safeWebsite || safePortfolio" class="profile-links">
            <a v-if="safeWebsite" :href="safeWebsite" target="_blank" rel="noopener noreferrer nofollow" :aria-label="`访问${author.displayName}的个人网站（新窗口打开）`">个人网站</a>
            <a v-if="safePortfolio" :href="safePortfolio" target="_blank" rel="noopener noreferrer nofollow" :aria-label="`访问${author.displayName}的作品集（新窗口打开）`">作品集</a>
          </div>
        </div>
      </section>

      <section class="article-toolbar" aria-label="作者文章筛选与排序">
        <div class="toolbar-left">
          <h2>公开文章</h2>
          <span>共 {{ articleTotal }} 篇</span>
        </div>
        <div class="toolbar-filters">
          <el-select v-model="selectedCategoryId" placeholder="全部分类" clearable @change="onFilterChange">
            <el-option v-for="cat in authorCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
          <el-select v-model="selectedTagId" placeholder="全部标签" clearable @change="onFilterChange">
            <el-option v-for="tag in authorTags" :key="tag.id" :label="tag.name" :value="tag.id" />
          </el-select>
          <div class="sort-switch" role="group" aria-label="作者文章排序">
            <button type="button" :class="{ active: articleSort === 'latest' }" @click="selectSort('latest')">最新发布</button>
            <button type="button" :class="{ active: articleSort === 'mostViewed' }" @click="selectSort('mostViewed')">阅读最多</button>
          </div>
        </div>
      </section>

      <div v-if="loadingArticles" class="state-panel small">正在加载文章…</div>
      <div v-else-if="articleError" class="state-panel small error-state" role="alert">
        <strong>文章列表加载失败</strong>
        <button type="button" @click="fetchArticles">重新加载</button>
      </div>
      <div v-else-if="articles.length === 0" class="state-panel small">暂无符合条件的文章</div>
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

function formatNumber(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

async function fetchArticles() {
  if (!author.value) return
  const currentRequest = ++articleRequestId
  loadingArticles.value = true
  articleError.value = false
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize,
      sort: articleSort.value,
    }
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
.profile-panel {
  padding: 26px 28px; display: flex; align-items: stretch; justify-content: space-between; gap: 28px;
  border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card);
  background: linear-gradient(135deg, #f7fbff 0%, var(--blog-color-surface) 66%, #f3f8ff 100%);
}
.profile-main { min-width: 0; flex: 1; display: flex; gap: 20px; }
.profile-main > img { width: 88px; height: 88px; flex-shrink: 0; border: 4px solid #fff; border-radius: 50%; object-fit: cover; background: var(--blog-color-background); box-shadow: 0 6px 18px rgba(41,72,108,.12); }
.profile-copy { min-width: 0; }
.eyebrow { color: var(--blog-color-primary); font-size: 11px; font-weight: 700; letter-spacing: .16em; }
.profile-copy h1 { margin: 4px 0 8px; color: var(--blog-color-text); font-size: 28px; line-height: 1.3; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.profile-copy p { max-width: 620px; margin: 0 0 12px; color: var(--blog-color-text-secondary); font-size: 14px; line-height: 1.75; overflow-wrap: break-word; }
.expertise-list { display: flex; flex-wrap: wrap; gap: 7px; }
.expertise-list span { padding: 4px 10px; border-radius: 999px; color: var(--blog-color-primary); background: var(--blog-color-primary-soft); font-size: 12px; }
.profile-side { width: 205px; flex-shrink: 0; display: flex; flex-direction: column; justify-content: center; gap: 14px; }
.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); overflow: hidden; border: 1px solid var(--blog-color-border); border-radius: 10px; background: rgba(255,255,255,.7); }
.stats-grid div { padding: 13px 8px; display: flex; align-items: center; flex-direction: column; gap: 3px; text-align: center; }
.stats-grid div + div { border-left: 1px solid var(--blog-color-border); }
.stats-grid strong { color: var(--blog-color-text); font-size: 19px; }
.stats-grid span { color: var(--blog-color-text-muted); font-size: 11px; }
.profile-links { display: flex; justify-content: center; gap: 12px; }
.profile-links a { color: var(--blog-color-primary); font-size: 13px; text-decoration: none; }
.article-toolbar { padding: 13px 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: var(--blog-color-surface); }
.toolbar-left { display: flex; align-items: baseline; gap: 9px; }
.toolbar-left h2 { margin: 0; color: var(--blog-color-text); font-size: 18px; }
.toolbar-left span { color: var(--blog-color-text-muted); font-size: 13px; }
.toolbar-filters { display: flex; align-items: center; gap: 10px; }
.sort-switch { padding: 3px; display: flex; border: 1px solid var(--blog-color-border); border-radius: 8px; background: var(--blog-color-background); }
.sort-switch button { min-height: 30px; padding: 0 12px; border: 0; border-radius: 5px; color: var(--blog-color-text-muted); background: transparent; cursor: pointer; }
.sort-switch button.active { color: var(--blog-color-primary); background: var(--blog-color-surface); box-shadow: 0 1px 4px rgba(31,45,61,.1); font-weight: 600; }
.state-panel { min-height: 240px; padding: 32px; display: grid; place-content: center; gap: 10px; text-align: center; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); color: var(--blog-color-text-muted); background: var(--blog-color-surface); }
.state-panel.small { min-height: 150px; }
.error-state strong { color: var(--blog-color-text); font-size: 17px; }
.error-state a, .error-state button { justify-self: center; padding: 8px 14px; border: 0; border-radius: 8px; color: #fff; background: var(--blog-color-primary); cursor: pointer; text-decoration: none; }
.pagination-box { display: flex; justify-content: center; padding: 18px 0; }
a:focus-visible, button:focus-visible { outline: 3px solid rgba(51,126,204,.24); outline-offset: 2px; }
@media (max-width: 760px) {
  .profile-panel { padding: 22px; flex-direction: column; }
  .profile-side { width: 100%; }
  .stats-grid { max-width: 330px; }
  .article-toolbar { flex-direction: column; align-items: stretch; }
  .toolbar-filters { flex-wrap: wrap; }
}
@media (max-width: 520px) {
  .profile-main { align-items: flex-start; flex-direction: column; }
  .profile-main > img { width: 76px; height: 76px; }
  .profile-copy h1 { font-size: 24px; }
}
</style>
