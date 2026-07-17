<template>
  <section class="favorites-card">
    <div class="page-heading"><div><h1>我的收藏</h1><p>保存下来，稍后继续阅读</p></div><router-link to="/blog/profile">个人中心</router-link></div>
    <el-skeleton v-if="loading" :rows="6" animated />
    <el-empty v-else-if="favorites.length === 0" description="还没有收藏文章" />
    <div v-else class="favorite-list">
      <article v-for="item in favorites" :key="item.articleId" class="favorite-item">
        <div><router-link :to="`/blog/article/${item.articleId}`">{{ item.title }}</router-link><p>{{ item.summary || '暂无摘要' }}</p><small>收藏于 {{ formatDate(item.favoriteTime) }}</small></div>
        <el-button text type="danger" @click="cancelFavorite(item.articleId)">取消收藏</el-button>
      </article>
      <el-pagination v-if="total > pageSize" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="loadFavorites" />
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getFavorites, removeFavorite } from '@/api/member'
import { formatDate } from '@/utils/format'

const favorites = ref([])
const loading = ref(true)
const pageNum = ref(1)
const pageSize = 10
const total = ref(0)

async function loadFavorites() {
  loading.value = true
  try {
    const res = await getFavorites({ pageNum: pageNum.value, pageSize })
    favorites.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
async function cancelFavorite(id) {
  await removeFavorite(id)
  ElMessage.success('已取消收藏')
  loadFavorites()
}
onMounted(loadFavorites)
</script>

<style scoped>
.favorites-card { padding: 28px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: #fff; }
.page-heading { margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; } h1 { margin: 0 0 6px; font-size: 24px; } .page-heading p { margin: 0; color: var(--blog-color-text-muted); } .page-heading a { color: var(--blog-color-primary); text-decoration: none; }
.favorite-item { padding: 18px 0; display: flex; justify-content: space-between; gap: 20px; border-bottom: 1px solid var(--blog-color-border); }
.favorite-item a { color: var(--blog-color-text); font-size: 17px; font-weight: 600; text-decoration: none; } .favorite-item a:hover { color: var(--blog-color-primary); }
.favorite-item p { margin: 7px 0; color: var(--blog-color-text-secondary); } .favorite-item small { color: var(--blog-color-text-muted); }
</style>
