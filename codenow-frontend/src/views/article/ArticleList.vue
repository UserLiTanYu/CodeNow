<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="router.push('/articles/edit')">新增文章</el-button>
      <div class="filters">
        <el-cascader
          v-model="filterCategoryId"
          :options="categoryOptions"
          :props="{ emitPath: false, checkStrictly: true, expandTrigger: 'hover' }"
          popper-class="article-category-filter-popper"
          placeholder="按分类筛选"
          clearable
          style="width: 210px"
          @change="handleFilterChange"
        />
        <el-select v-model="filterTagId" placeholder="按标签筛选" clearable style="width: 160px" @change="handleFilterChange">
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </div>
    </div>

    <el-table :data="articles" stripe>
      <el-table-column label="标题" min-width="200">
        <template #default="{ row }">
          <el-tag v-if="row.article.isTop === 1" type="danger" size="small" style="margin-right: 6px">置顶</el-tag>
          {{ row.article.title }}
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="120" />
      <el-table-column prop="article.sort" label="顺序" width="80" />
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
      <el-table-column prop="article.createTime" label="创建时间" width="180" :formatter="formatDateCell" />
      <el-table-column label="操作" width="310" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="router.push('/articles/edit/' + row.article.id)">编辑</el-button>
          <el-button size="small" :type="row.article.isTop === 1 ? 'warning' : ''" @click="toggleTop(row.article.id)">
            {{ row.article.isTop === 1 ? '取消置顶' : '置顶' }}
          </el-button>
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
import { computed, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArticles, deleteArticle, toggleArticleStatus, toggleArticleTop } from '@/api/article'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/tag'
import { categoryCascaderOptions } from '@/utils/categoryTree'
import { formatDateCell } from '@/utils/format'

const router = useRouter()
const LIST_STATE_KEY = 'codenow:article-list-state'
const savedState = readSavedState()
const articles = ref([])
const categories = ref([])
const tags = ref([])
const total = ref(0)
const pageNum = ref(savedState.pageNum || 1)
const pageSize = ref(7)
const filterCategoryId = ref(savedState.categoryId || null)
const filterTagId = ref(savedState.tagId || null)
const categoryOptions = computed(() => categoryCascaderOptions(categories.value))

function readSavedState() {
  try {
    return JSON.parse(sessionStorage.getItem(LIST_STATE_KEY)) || {}
  } catch {
    return {}
  }
}

function saveListState() {
  sessionStorage.setItem(LIST_STATE_KEY, JSON.stringify({
    pageNum: pageNum.value,
    categoryId: filterCategoryId.value,
    tagId: filterTagId.value,
  }))
}

async function loadArticles() {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value }
  if (filterCategoryId.value) params.categoryId = filterCategoryId.value
  if (filterTagId.value) params.tagId = filterTagId.value
  const res = await getArticles(params)
  if (res.data.records.length === 0 && pageNum.value > 1 && res.data.total > 0) {
    pageNum.value = Math.ceil(res.data.total / pageSize.value)
    return loadArticles()
  }
  articles.value = res.data.records
  total.value = res.data.total
  saveListState()
}

function handleFilterChange() {
  pageNum.value = 1
  loadArticles()
}

async function loadFilters() {
  const [catRes, tagRes] = await Promise.all([getCategories(), getTags()])
  categories.value = catRes.data
  tags.value = tagRes.data
}

async function toggleStatus(id) {
  await toggleArticleStatus(id)
  ElMessage.success('状态切换成功')
  loadArticles()
}

async function toggleTop(id) {
  await toggleArticleTop(id)
  ElMessage.success('置顶状态切换成功')
  loadArticles()
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该文章？', '提示', { type: 'warning' })
  await deleteArticle(id)
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
  justify-content: center;
}
</style>

<style>
.article-category-filter-popper .el-cascader-menu {
  width: 220px;
  min-width: 220px;
}

.article-category-filter-popper .el-cascader-node {
  padding-left: 16px;
}

.article-category-filter-popper .el-cascader-node > .el-radio {
  display: none;
}

.article-category-filter-popper .el-cascader-node__label {
  min-width: 0;
  padding-left: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
