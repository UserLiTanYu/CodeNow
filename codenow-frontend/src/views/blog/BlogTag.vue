<template>
  <div class="blog-tag">
    <header class="page-header">
      <div>
        <span class="page-eyebrow">标签</span>
        <h1>{{ tagName }}</h1>
      </div>
      <label class="sort-control">
        <span>排序</span>
        <el-select v-model="selectedSort" aria-label="标签文章排序" @change="changeSort">
          <el-option label="最新发布" value="latest" />
          <el-option label="最多阅读" value="mostViewed" />
        </el-select>
      </label>
    </header>
    <div v-if="loading" class="loading-box">
      <el-skeleton :rows="5" animated />
    </div>
    <template v-else>
      <el-alert v-if="errorMessage" :title="errorMessage" type="error" show-icon :closable="false" class="error-alert" />
      <div v-if="articles.length === 0" class="empty-box">
        <el-empty description="该标签下暂无文章" />
      </div>
      <template v-else>
        <BlogArticleCard v-for="item in articles" :key="item.article.id" :item="item" />
        <nav v-if="total > pageSize" class="pagination-box" aria-label="标签文章分页">
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
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BlogArticleCard from '@/components/blog/BlogArticleCard.vue'
import { getBlogArticles, getBlogTags } from '@/api/blog'

const route = useRoute()
const router = useRouter()
const articles = ref([])
const tagName = ref('')
const loading = ref(true)
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const errorMessage = ref('')
const selectedSort = ref(route.query.sort === 'mostViewed' ? 'mostViewed' : 'latest')

async function fetchArticles() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getBlogArticles({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      tagId: route.params.id,
      sort: selectedSort.value,
    })
    articles.value = res.data.records
    total.value = res.data.total
  } catch {
    articles.value = []
    total.value = 0
    errorMessage.value = '标签文章加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

async function fetchTagName() {
  try {
    const res = await getBlogTags()
    const tag = res.data.find((item) => item.id === Number(route.params.id))
    tagName.value = tag ? tag.name : '未知标签'
  } catch {
    tagName.value = '未知标签'
  }
}

function changeSort() {
  const query = selectedSort.value === 'latest' ? {} : { sort: selectedSort.value }
  router.push({ path: route.path, query })
}

watch(
  () => [route.params.id, route.query.sort],
  ([, sort]) => {
    selectedSort.value = sort === 'mostViewed' ? 'mostViewed' : 'latest'
    pageNum.value = 1
    fetchTagName()
    fetchArticles()
  },
  { immediate: true },
)
</script>

<style scoped>
.page-header {
  margin-bottom: var(--blog-space-4);
  padding: var(--blog-space-4) var(--blog-space-5);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--blog-space-5);
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}
.page-eyebrow {
  color: var(--blog-color-primary);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}
.page-header h1 {
  margin: var(--blog-space-1) 0 0;
  color: var(--blog-color-text);
  font-size: 21px;
  font-weight: 600;
}
.sort-control {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--blog-color-text-secondary);
  font-size: 13px;
}
.sort-control :deep(.el-select) {
  width: 132px;
}
.pagination-box {
  padding: 24px 0;
  display: flex;
  justify-content: center;
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
@media (max-width: 520px) {
  .page-header {
    align-items: stretch;
    flex-direction: column;
  }
  .sort-control :deep(.el-select) {
    flex: 1;
    width: auto;
  }
}
</style>
