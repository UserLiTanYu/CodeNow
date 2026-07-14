<template>
  <div class="blog-home">
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="5" animated />
    </div>
    <template v-else>
      <div v-if="articles.length === 0" class="empty-box">
        <el-empty description="暂无文章" />
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Folder, Clock, View } from '@element-plus/icons-vue'
import { getBlogArticles } from '@/api/blog'
import { formatDate } from '@/utils/format'

const router = useRouter()
const articles = ref([])
const loading = ref(true)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

function goDetail(id) {
  router.push(`/blog/article/${id}`)
}

async function fetchArticles() {
  loading.value = true
  try {
    const res = await getBlogArticles({ pageNum: pageNum.value, pageSize: pageSize.value })
    articles.value = res.data.records
    total.value = res.data.total
  } catch {
    // 静默失败
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchArticles()
})
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
</style>
