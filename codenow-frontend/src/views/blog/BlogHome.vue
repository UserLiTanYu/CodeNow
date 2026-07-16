<template>
  <div class="blog-home">
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
        <el-empty :description="activeKeyword ? `没有找到与“${activeKeyword}”相关的文章` : '暂无文章'" />
      </div>
      <div v-else>
        <div
          v-for="item in articles"
          :key="item.article.id"
          class="article-card"
          @click="goDetail(item.article.id)"
        >
          <div class="card-content">
            <h2 class="card-title">
              <el-tag v-if="item.article.isTop" size="small" type="danger" effect="dark" class="top-tag">置顶</el-tag>
              {{ item.article.title }}
            </h2>
            <p class="card-summary">{{ item.article.summary || '暂无摘要' }}</p>
            <div class="card-meta">
              <span v-if="item.categoryName" class="meta-item">
                <el-icon><Folder /></el-icon> {{ item.categoryName }}
              </span>
              <span class="meta-item">
                <el-icon><Clock /></el-icon> {{ formatDate(item.article.createTime) }}
              </span>
              <span class="meta-item">
                <el-icon><View /></el-icon> {{ item.article.viewCount || 0 }} 阅读
              </span>
              <span v-for="tag in item.tags" :key="tag.id" class="meta-tag">{{ tag.name }}</span>
            </div>
          </div>
          <div v-if="item.article.coverImage" class="card-cover">
            <img :src="item.article.coverImage" :alt="item.article.title" />
          </div>
        </div>

        <div class="pagination-box">
          <el-pagination
            v-model:current-page="pageNum"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            @current-change="fetchArticles"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Folder, Clock, View } from '@element-plus/icons-vue'
import { getBlogArticles } from '@/api/blog'
import { formatDate } from '@/utils/format'

const router = useRouter()
const route = useRoute()
const articles = ref([])
const loading = ref(true)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const errorMessage = ref('')
const activeKeyword = ref('')

function goDetail(id) {
  router.push(`/blog/article/${id}`)
}

async function fetchArticles() {
  loading.value = true
  errorMessage.value = ''
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (activeKeyword.value) {
      params.keyword = activeKeyword.value
    }
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

function clearSearch() {
  router.push('/blog')
}

watch(
  () => route.query.keyword,
  (keyword) => {
    activeKeyword.value = typeof keyword === 'string' ? keyword.trim().slice(0, 100) : ''
    pageNum.value = 1
    fetchArticles()
  },
  { immediate: true },
)
</script>

<style scoped>
.article-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
  cursor: pointer;
  display: flex;
  gap: 20px;
  transition: box-shadow 0.2s;
}
.article-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.card-content {
  flex: 1;
  min-width: 0;
}
.card-title {
  font-size: 18px;
  font-weight: 600;
  margin: 0 0 10px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}
.top-tag {
  flex-shrink: 0;
}
.card-summary {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.card-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  font-size: 13px;
  color: #909399;
}
.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}
.meta-tag {
  background: #f0f2f5;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}
.card-cover {
  width: 180px;
  height: 120px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pagination-box {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}
.loading-box,
.empty-box {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
}
.error-alert {
  margin-bottom: 16px;
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
  gap: 8px;
  flex-wrap: wrap;
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

@media (max-width: 640px) {
  .article-card {
    padding: 18px;
  }
  .card-cover {
    display: none;
  }
  .card-title {
    font-size: 17px;
    line-height: 1.45;
  }
  .card-meta {
    gap: 9px 12px;
  }
  .search-result-heading {
    align-items: flex-start;
  }
}
</style>
