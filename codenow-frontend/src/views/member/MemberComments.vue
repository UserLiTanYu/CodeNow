<template>
  <section class="member-card">
    <div class="heading"><div><h1>我的评论</h1><p>查看和管理你发表过的评论</p></div><router-link to="/blog/profile">返回个人中心</router-link></div>
    <el-table v-loading="loading" :data="comments" empty-text="还没有发表过评论">
      <el-table-column label="文章" min-width="180"><template #default="{ row }"><router-link :to="`/blog/article/${row.articleId}`">{{ row.articleTitle }}</router-link></template></el-table-column>
      <el-table-column prop="content" label="评论内容" min-width="280" show-overflow-tooltip />
      <el-table-column prop="createTime" label="发表时间" width="180" :formatter="formatDateCell" />
      <el-table-column label="操作" width="90"><template #default="{ row }"><el-button type="danger" link @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
    <el-pagination v-if="total > pageSize" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" class="pagination" @current-change="load" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteMyComment, getMyComments } from '@/api/member'
import { formatDateCell } from '@/utils/format'
const comments = ref([]); const loading = ref(false); const pageNum = ref(1); const pageSize = 10; const total = ref(0)
async function load() { loading.value = true; try { const res = await getMyComments({ pageNum: pageNum.value, pageSize }); comments.value = res.data.records; total.value = res.data.total } finally { loading.value = false } }
async function remove(row) { await ElMessageBox.confirm('删除后将显示为“该评论已由用户删除”，回复关系会保留。', '删除评论', { type: 'warning' }); await deleteMyComment(row.id); ElMessage.success('评论已删除'); load() }
onMounted(load)
</script>

<style scoped>
.member-card { padding: 28px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: #fff; }
.heading { margin-bottom: 24px; display: flex; justify-content: space-between; align-items: center; } h1 { margin: 0 0 6px; font-size: 24px; } p { margin: 0; color: var(--blog-color-text-muted); } a { color: var(--blog-color-primary); text-decoration: none; } .pagination { margin-top: 20px; justify-content: center; }
</style>
