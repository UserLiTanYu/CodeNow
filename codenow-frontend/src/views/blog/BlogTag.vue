<template>
  <div class="blog-tag">
    <div class="page-header">
      <h2>标签：{{ tagName }}</h2>
    </div>
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="5" animated />
    </div>
    <template v-else>
      <div v-if="articles.length === 0" class="empty-box">
        <el-empty description="该标签下暂无文章" />
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
            </div>
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
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Folder, Clock, View } from '@element-plus/icons-vue'
import { getBlogArticles, getBlogTags } from '@/api/blog'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const articles = ref([])
const tagName = ref('')
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
    const res = await getBlogArticles({ pageNum: pageNum.value, pageSize: pageSize.value, tagId: route.params.id })
    articles.value = res.data.records
    total.value = res.data.total
  } catch {
    // 静默失败
  } finally {
    loading.value = false
  }
}

async function fetchTagName() {
  try {
    const res = await getBlogTags()
    const tag = res.data.find((t) => t.id === Number(route.params.id))
    tagName.value = tag ? tag.name : '未知标签'
  } catch {
    tagName.value = '未知标签'
  }
}

onMounted(() => {
  fetchTagName()
  fetchArticles()
})

watch(
  () => route.params.id,
  () => {
    pageNum.value = 1
    fetchTagName()
    fetchArticles()
  },
)
</script>

<style scoped>
.page-header {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
}
.page-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}
.article-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.article-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
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
