<template>
  <div class="blog-article">
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="10" animated />
    </div>
    <template v-else-if="article">
      <div class="article-header">
        <h1 class="article-title">{{ article.title }}</h1>
        <div class="article-meta">
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
      </div>
      <div class="article-body markdown-body" v-html="renderedContent"></div>

      <!-- 评论区 -->
      <div class="comment-section">
        <h3 class="section-title">评论 ({{ comments.length }})</h3>
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
        </div>
        <div v-else class="no-comments">
          <p>暂无评论，快来发表第一条评论吧</p>
        </div>
      </div>
    </template>
    <div v-else class="empty-box">
      <el-empty description="文章不存在" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Folder, Clock, View } from '@element-plus/icons-vue'
import { marked } from 'marked'
import { markedHighlight } from 'marked-highlight'
import CommentForm from '@/components/CommentForm.vue'
import CommentTree from '@/components/CommentTree.vue'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.css'
import { getBlogArticle } from '@/api/blog'
import { getCommentTree } from '@/api/comment'
import { formatDate } from '@/utils/format'

// 配置 marked 使用 highlight.js（v18 使用 marked-highlight 扩展）
marked.use(markedHighlight({
  langPrefix: 'hljs language-',
  highlight(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      return hljs.highlight(code, { language: lang }).value
    }
    return hljs.highlightAuto(code).value
  },
}))

const route = useRoute()
const article = ref(null)
const categoryName = ref('')
const tags = ref([])
const comments = ref([])
const loading = ref(true)

const renderedContent = computed(() => {
  if (!article.value?.content) return ''
  return marked(article.value.content)
})

async function fetchArticle(id) {
  loading.value = true
  try {
    const res = await getBlogArticle(id)
    article.value = res.data.article
    categoryName.value = res.data.categoryName || ''
    tags.value = res.data.tags || []
    fetchComments()
  } catch {
    article.value = null
  } finally {
    loading.value = false
  }
}

async function fetchComments() {
  try {
    const res = await getCommentTree(route.params.id)
    comments.value = res.data
  } catch {
    comments.value = []
  }
}

onMounted(() => {
  fetchArticle(route.params.id)
})

watch(
  () => route.params.id,
  (newId) => {
    if (newId) fetchArticle(newId)
  },
)
</script>

<style scoped>
.article-header {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  margin-bottom: 16px;
}
.article-title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 16px;
  color: #303133;
  line-height: 1.4;
}
.article-meta {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.article-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.tag-link {
  padding: 3px 10px;
  background: #ecf5ff;
  color: #409eff;
  border-radius: 4px;
  font-size: 13px;
  text-decoration: none;
  transition: all 0.2s;
}
.tag-link:hover {
  background: #409eff;
  color: #fff;
}

/* 文章内容 */
.article-body {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
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
  color: #303133;
}
.markdown-body :deep(code) {
  background: #f0f2f5;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 13px;
  font-family: 'Courier New', Courier, monospace;
}
.markdown-body :deep(pre) {
  background: #f6f8fa;
  border-radius: 6px;
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
  border-left: 4px solid #409eff;
  margin: 0 0 16px;
  padding: 12px 16px;
  background: #f0f7ff;
  color: #606266;
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
  border: 1px solid #e4e7ed;
  padding: 8px 12px;
  text-align: left;
}
.markdown-body :deep(th) {
  background: #f5f7fa;
  font-weight: 600;
}
.markdown-body :deep(img) {
  max-width: 100%;
  border-radius: 4px;
}

.loading-box,
.empty-box {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
}

/* 评论区 */
.comment-section {
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  margin-top: 16px;
}
.section-title {
  margin: 0 0 20px;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}
.comment-list {
  margin-top: 24px;
}
.no-comments {
  margin-top: 24px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
