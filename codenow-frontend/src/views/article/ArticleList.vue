<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="router.push('/articles/edit')">新增文章</el-button>
      <div class="filters">
        <el-select v-model="filterCategoryId" placeholder="按分类筛选" clearable style="width: 160px" @change="loadArticles">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-select v-model="filterTagId" placeholder="按标签筛选" clearable style="width: 160px" @change="loadArticles">
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </div>
    </div>

    <el-table :data="articles" v-loading="loading" stripe>
      <el-table-column prop="article.title" label="标题" min-width="200" />
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column label="标签" width="200">
        <template #default="{ row }">
          <el-tag v-for="tag in row.tags" :key="tag.id" size="small" style="margin: 2px">{{ tag.name }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.article.status === 1 ? 'success' : 'info'">
            {{ row.article.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="article.createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="250" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="router.push('/articles/edit/' + row.article.id)">编辑</el-button>
          <el-button size="small" :type="row.article.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row.article.id)">
            {{ row.article.status === 1 ? '下架' : '发布' }}
          </el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.article.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      background
      layout="total, prev, pager, next"
      :total="total"
      :page-size="pageSize"
      v-model:current-page="pageNum"
      @current-change="loadArticles"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const router = useRouter()
const articles = ref([])
const categories = ref([])
const tags = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const filterCategoryId = ref(null)
const filterTagId = ref(null)

async function loadArticles() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (filterCategoryId.value) params.categoryId = filterCategoryId.value
    if (filterTagId.value) params.tagId = filterTagId.value
    const res = await request.get('/articles', { params })
    articles.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadFilters() {
  const [catRes, tagRes] = await Promise.all([request.get('/categories'), request.get('/tags')])
  categories.value = catRes.data
  tags.value = tagRes.data
}

async function toggleStatus(id) {
  await request.put(`/articles/${id}/status`)
  ElMessage.success('状态切换成功')
  loadArticles()
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该文章？', '提示', { type: 'warning' })
  await request.delete(`/articles/${id}`)
  ElMessage.success('删除成功')
  loadArticles()
}

onMounted(() => {
  loadArticles()
  loadFilters()
})
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 16px;
}
.filters {
  display: flex;
  gap: 10px;
}
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
