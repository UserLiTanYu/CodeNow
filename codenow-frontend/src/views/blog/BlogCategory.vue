<template>
  <div class="blog-category">
    <header class="page-header">
      <div>
        <span class="page-eyebrow">分类</span>
        <h1>{{ categoryName }}</h1>
      </div>
      <label class="sort-control">
        <span>排序</span>
        <el-select v-model="selectedSort" aria-label="分类文章排序" @change="changeSort">
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
        <el-empty description="该分类下暂无文章" />
      </div>
      <template v-else>
        <BlogArticleCard v-for="item in articles" :key="item.article.id" :item="item" :show-category="false" />
        <nav v-if="total > pageSize" class="pagination-box" aria-label="分类文章分页">
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
import { getBlogArticles, getBlogCategories } from '@/api/blog'

const route = useRoute()
const router = useRouter()
const articles = ref([])
const categoryName = ref('')
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
      categoryId: route.params.id,
      sort: selectedSort.value,
    })
    articles.value = res.data.records
    total.value = res.data.total
  } catch {
    articles.value = []
    total.value = 0
    errorMessage.value = '分类文章加载失败，请检查网络后重试'
  } finally {
    loading.value = false
  }
}

async function fetchCategoryName() {
  try {
    const res = await getBlogCategories()
    const category = res.data.find((item) => item.id === Number(route.params.id))
    categoryName.value = category ? category.name : '未知分类'
  } catch {
    categoryName.value = '未知分类'
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
    fetchCategoryName()
    fetchArticles()
  },
  { immediate: true },
)
</script>

<style scoped>
.page-header {
  margin-bottom: 16px;
  padding: 18px 22px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}
.page-eyebrow {
  color: #337ecc;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.08em;
}
.page-header h1 {
  margin: 3px 0 0;
  color: #303133;
  font-size: 21px;
  font-weight: 600;
}
.sort-control {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #7d8592;
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
  border-radius: 10px;
  background: #fff;
}
.error-alert {
  margin-bottom: 16px;
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
