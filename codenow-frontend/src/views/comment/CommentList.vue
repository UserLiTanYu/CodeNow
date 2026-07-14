<template>
  <div>
    <el-table :data="comments" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="nickname" label="昵称" width="120" />
      <el-table-column prop="content" label="评论内容" min-width="250" show-overflow-tooltip />
      <el-table-column prop="articleId" label="文章 ID" width="80" />
      <el-table-column prop="parentId" label="父评论 ID" width="90">
        <template #default="{ row }">
          {{ row.parentId === 0 ? '-' : row.parentId }}
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP 地址" width="130" />
      <el-table-column prop="createTime" label="评论时间" width="170" />
      <el-table-column label="操作" width="80" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      background
      layout="total, sizes, prev, pager, next"
      :total="total"
      :page-sizes="[10, 20, 50]"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      @current-change="fetchComments"
      @size-change="fetchComments"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const comments = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

async function fetchComments() {
  loading.value = true
  try {
    const res = await request.get('/comments', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value },
    })
    comments.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('删除评论将同时删除其所有回复，确定继续？', '提示', { type: 'warning' })
  await request.delete(`/comments/${id}`)
  ElMessage.success('删除成功')
  fetchComments()
}

onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
