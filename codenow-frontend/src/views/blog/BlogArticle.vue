<template>
  <div class="blog-article">
    <!-- 加载中骨架屏 -->
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="10" animated />
    </div>
    <template v-else-if="article">
      <!-- 文章头部信息：标题、作者、分类、时间、阅读量、标签、收藏按钮 -->
      <div class="article-header">
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-meta">
          <router-link v-if="author" :to="`/blog/author/${author.userId}`" class="meta-item author-meta">
            <img :src="avatarUrl(author.avatar)" alt="" @error="useDefaultAvatar" />
            {{ author.displayName }}
          </router-link>
          <span v-if="categoryName" class="meta-item">
            <el-icon><Folder /></el-icon> {{ categoryName }}
          </span>
          <span class="meta-item">
            <el-icon><Clock /></el-icon> {{ formatDate(article.createTime) }}
          </span>
          <span class="meta-item">
            <el-icon><View /></el-icon> {{ article.viewCount || 0 }} 阅读
          </span>
        </div>
        <div v-if="tags.length > 0" class="article-tags">
          <router-link
            v-for="tag in tags"
            :key="tag.id"
            :to="`/blog/tag/${tag.id}`"
            class="tag-link"
          >
            {{ tag.name }}
          </router-link>
        </div>
        <button type="button" :class="['favorite-button', { active: favorited }]" :disabled="favoriteLoading" @click="toggleFavorite">
          <el-icon><StarFilled v-if="favorited" /><Star v-else /></el-icon>
          {{ favorited ? '已收藏' : '收藏文章' }}
        </button>
      </div>
      <!-- 文章正文内容（Markdown 渲染后的 HTML） -->
      <div class="article-body markdown-body" v-html="renderedContent"></div>

      <!-- 学习顺序导航：下一篇 -->
      <nav v-if="nextArticle" class="next-article-nav" aria-label="学习顺序导航">
        <span class="next-article-label">下一篇</span>
        <router-link :to="`/blog/article/${nextArticle.id}`" class="next-article-link">
          <span class="next-article-title">{{ nextArticle.title }}</span>
          <el-icon><ArrowRight /></el-icon>
        </router-link>
      </nav>

      <!-- 评论区 -->
      <div class="comment-section">
        <h3 class="section-title">评论 ({{ commentTotalCount }})</h3>
        <CommentForm
          :article-id="Number(route.params.id)"
          :parent-id="0"
          @success="fetchComments"
        />
        <div v-if="comments.length > 0" class="comment-list">
          <CommentTree
            :comments="comments"
            :article-id="Number(route.params.id)"
            @refresh="fetchComments"
          />
          <el-pagination
            v-if="commentRootTotal > commentPageSize"
            v-model:current-page="commentPageNum"
            :page-size="commentPageSize"
            :total="commentRootTotal"
            layout="prev, pager, next"
            class="comment-pagination"
            @current-change="fetchComments"
          />
        </div>
        <el-alert v-if="commentError" :title="commentError" type="error" show-icon :closable="false" />
        <div v-else-if="comments.length === 0" class="no-comments">
          <p>暂无评论，快来发表第一条评论吧</p>
        </div>
      </div>
    </template>
    <!-- 文章不存在或加载失败时的空状态 -->
    <div v-else class="empty-box">
      <el-alert v-if="articleError" :title="articleError" type="error" show-icon :closable="false" />
      <el-empty :description="articleError ? '暂时无法显示文章' : '文章不存在'" />
    </div>
  </div>
</template>

<script setup>
/** 博客文章详情页 - 展示文章内容、作者信息、标签、评论区和收藏功能 */
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Folder, Clock, Star, StarFilled, View, ArrowRight } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import DOMPurify from 'dompurify'
import CommentForm from '@/components/CommentForm.vue'
import CommentTree from '@/components/CommentTree.vue'
// highlight.js 按需导入（仅常用语言，避免全量打包 ~1MB）
import hljs from 'highlight.js/lib/core'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'
import sql from 'highlight.js/lib/languages/sql'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import bash from 'highlight.js/lib/languages/bash'
import json from 'highlight.js/lib/languages/json'
import yaml from 'highlight.js/lib/languages/yaml'
import go from 'highlight.js/lib/languages/go'
import rust from 'highlight.js/lib/languages/rust'
import cpp from 'highlight.js/lib/languages/cpp'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('python', python)
hljs.registerLanguage('java', java)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('json', json)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('go', go)
hljs.registerLanguage('rust', rust)
hljs.registerLanguage('c', cpp)
hljs.registerLanguage('cpp', cpp)
import { getBlogArticle, getBlogArticles } from '@/api/blog'
import { getCommentTree } from '@/api/comment'
import { formatDate } from '@/utils/format'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'
import { currentArticleAuthorId } from '@/utils/blogArticleAuthor'
import { addFavorite, getFavoriteStatus, removeFavorite } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

// 配置 marked 使用 highlight.js 进行代码高亮
// 注意：每次模块加载时调用 marked.use() 会合并配置而非覆盖，
// 所以用 _hljsConfigured 标记避免 HMR 重复注册
if (!globalThis._hljsConfigured) {
  marked.use(markedHighlight({
    langPrefix: 'hljs language-',
    highlight(code, lang) {
      if (lang && hljs.getLanguage(lang)) {
        return hljs.highlight(code, { language: lang }).value
      }
      return hljs.highlightAuto(code).value
    },
  }))
  globalThis._hljsConfigured = true
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const article = ref(null)
const author = ref(null)
const categoryName = ref('')
const tags = ref([])
const comments = ref([])
const nextArticle = ref(null)
const loading = ref(true)
const articleError = ref('')
const commentError = ref('')
const commentPageNum = ref(1)
const commentPageSize = 20
const commentRootTotal = ref(0)
const commentTotalCount = ref(0)
const favorited = ref(false)
const favoriteLoading = ref(false)
/** 路由快速切换时递增请求序号，旧文章响应不得覆盖新文章页面 */
let requestId = 0

/** 将 Markdown 内容渲染为安全的 HTML（经过 DOMPurify 消毒） */
const renderedContent = computed(() => {
  if (!article.value?.content) return ''
  const result = marked(article.value.content)
  // marked-highlight 使用同步 highlight 函数，应返回字符串；
  // 若因模块初始化时序返回 Promise，则回退为空（下次依赖变化时 computed 会重新求值）
  const html = typeof result === 'string' ? result : ''
  // 安全顺序固定为 Markdown/高亮生成 HTML 后再清洗；class 仅供代码高亮样式使用，勿随意扩大白名单。
  return DOMPurify.sanitize(html, { ADD_ATTR: ['class'] })
})

/** 获取文章详情数据 */
async function fetchArticle(articleId) {
  const currentRequest = ++requestId
  loading.value = true
  articleError.value = ''
  // 路由切换时先清空作者，避免旧文章的作者侧边栏残留
  currentArticleAuthorId.value = null
  try {
    const res = await getBlogArticle(articleId)
    if (currentRequest !== requestId) return
    article.value = res.data.article
    author.value = res.data.author || null
    categoryName.value = res.data.categoryName || ''
    tags.value = res.data.tags || []
    // 向布局共享当前文章的作者ID，使侧边栏随文章作者作用域化
    currentArticleAuthorId.value = author.value?.userId ?? article.value?.authorId ?? null
    document.title = `${article.value.title} - 码上记`
    commentPageNum.value = 1
    fetchComments()
    fetchFavoriteStatus()
    fetchNextArticle()
  } catch {
    if (currentRequest !== requestId) return
    article.value = null
    author.value = null
    articleError.value = '文章加载失败，请稍后重试'
  } finally {
    if (currentRequest === requestId) loading.value = false
  }
}

/** 离开文章详情页时清空共享的作者ID，避免影响其他页面。 */
onUnmounted(() => {
  currentArticleAuthorId.value = null
})

/** 获取文章收藏状态 */
async function fetchFavoriteStatus() {
  const currentArticleId = route.params.id
  favorited.value = false
  if (!userStore.isLoggedIn) return
  try {
    const res = await getFavoriteStatus(route.params.id)
    if (route.params.id !== currentArticleId) return
    favorited.value = Boolean(res.data.favorited)
  } catch {
    if (route.params.id !== currentArticleId) return
    favorited.value = false
  }
}

/** 切换文章收藏状态 */
async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return
  }
  favoriteLoading.value = true
  try {
    if (favorited.value) {
      await removeFavorite(route.params.id)
      favorited.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addFavorite(route.params.id)
      favorited.value = true
      ElMessage.success('收藏成功')
    }
  } finally {
    favoriteLoading.value = false
  }
}

/** 获取同分类下一篇学习顺序的文章 */
async function fetchNextArticle() {
  nextArticle.value = null
  if (!article.value?.categoryId) return
  try {
    const res = await getBlogArticles({
      pageNum: 1,
      pageSize: 100,
      categoryId: article.value.categoryId,
      sort: 'learning',
    })
    const records = res.data?.records || []
    const idx = records.findIndex(r => r.article.id === article.value.id)
    if (idx >= 0 && idx < records.length - 1) {
      nextArticle.value = records[idx + 1].article
    }
  } catch {
    nextArticle.value = null
  }
}

/** 获取评论树数据 */
async function fetchComments() {
  const currentArticleId = route.params.id
  commentError.value = ''
  try {
    const res = await getCommentTree(route.params.id, {
      pageNum: commentPageNum.value,
      pageSize: commentPageSize,
    })
    if (route.params.id !== currentArticleId) return
    comments.value = res.data.page.records
    commentRootTotal.value = res.data.page.total
    commentTotalCount.value = res.data.totalCount
  } catch {
    if (route.params.id !== currentArticleId) return
    comments.value = []
    commentRootTotal.value = 0
    commentTotalCount.value = 0
    commentError.value = '评论加载失败，请稍后重试'
  }
}

/** 页面挂载时加载文章详情 */
onMounted(() => {
  fetchArticle(route.params.id)
})

/** 监听路由参数变化，切换文章时重新加载 */
watch(
  () => route.params.id,
  (newId) => {
    if (newId) fetchArticle(newId)
  },
)
</script>

<style scoped>
.article-header {
  margin-bottom: var(--blog-space-4);
  padding: var(--blog-space-6);
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}
.article-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 var(--blog-space-4);
  color: var(--blog-color-text);
  line-height: 1.4;
}
.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--blog-space-5);
  font-size: 14px;
  color: var(--blog-color-text-muted);
  margin-bottom: var(--blog-space-3);
}
.meta-item {
  display: flex;
  align-items: center;
  gap: var(--blog-space-1);
}
.author-meta { color: var(--blog-color-text-secondary); font-weight: 600; text-decoration: none; }
.author-meta:hover { color: var(--blog-color-primary); }
.author-meta img { width: 22px; height: 22px; border-radius: 50%; object-fit: cover; background: var(--blog-color-background); }
.article-tags {
  display: flex;
  gap: var(--blog-space-2);
  flex-wrap: wrap;
}
.favorite-button { margin-top: 16px; padding: 8px 14px; display: inline-flex; align-items: center; gap: 6px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-button); color: var(--blog-color-text-secondary); background: #fff; cursor: pointer; }
.favorite-button:hover, .favorite-button.active { color: var(--blog-color-primary); border-color: var(--blog-color-border-hover); background: var(--blog-color-primary-soft); }
.favorite-button:disabled { cursor: wait; opacity: 0.65; }
.tag-link {
  padding: var(--blog-space-1) var(--blog-space-2);
  background: var(--blog-color-primary-soft);
  color: var(--blog-color-primary);
  border-radius: var(--blog-radius-tag);
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}
.tag-link:hover {
  background: var(--blog-color-primary);
  color: #fff;
}

/* 文章内容 */
.article-body {
  padding: var(--blog-space-6);
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}

/* Markdown 样式 */
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin-top: 24px;
  margin-bottom: 12px;
  font-weight: 600;
}
.markdown-body :deep(h1) { font-size: 24px; }
.markdown-body :deep(h2) { font-size: 20px; }
.markdown-body :deep(h3) { font-size: 17px; }
.markdown-body :deep(p) {
  line-height: 1.8;
  margin: 0 0 12px;
  color: var(--blog-color-text);
}
.markdown-body :deep(code) {
  background: var(--blog-color-background);
  padding: 2px 6px;
  border-radius: var(--blog-radius-tag);
  font-size: 13px;
  font-family: 'Courier New', Courier, monospace;
}
.markdown-body :deep(pre) {
  background: var(--blog-color-background);
  border-radius: var(--blog-radius-button);
  padding: 16px;
  overflow-x: auto;
  margin: 0 0 16px;
}
.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  font-size: 14px;
  line-height: 1.6;
}
.markdown-body :deep(blockquote) {
  border-left: 4px solid var(--blog-color-primary);
  margin: 0 0 16px;
  padding: 12px 16px;
  background: var(--blog-color-primary-soft);
  color: var(--blog-color-text-secondary);
}
.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 0 0 12px;
}
.markdown-body :deep(li) {
  line-height: 1.8;
}
.markdown-body :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 0 0 16px;
}
.markdown-body :deep(th),
.markdown-body :deep(td) {
  border: 1px solid var(--blog-color-border);
  padding: 8px 12px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: var(--blog-color-background);
  font-weight: 600;
}
.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: var(--blog-radius-tag);
}

.loading-box,
.empty-box {
  padding: 40px;
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}

/* 学习顺序导航：下一篇 */
.next-article-nav {
  margin-top: var(--blog-space-4);
  padding: var(--blog-space-4) var(--blog-space-5);
  display: flex;
  align-items: center;
  gap: var(--blog-space-3);
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}
.next-article-label {
  flex-shrink: 0;
  color: var(--blog-color-text-muted);
  font-size: 13px;
  font-weight: 500;
}
.next-article-link {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 6px;
  flex: 1;
  justify-content: flex-end;
  color: var(--blog-color-primary);
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  transition: opacity 0.16s ease;
}
.next-article-link:hover {
  opacity: 0.75;
}
.next-article-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 评论区 */
.comment-section {
  margin-top: var(--blog-space-4);
  padding: var(--blog-space-6);
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}
.section-title {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
  color: var(--blog-color-text);
}
.comment-list {
  margin-top: 24px;
}
.no-comments {
  margin-top: 24px;
  text-align: center;
  color: var(--blog-color-text-muted);
  font-size: 14px;
}
.comment-pagination {
  justify-content: center;
  margin-top: 24px;
}
</style>
