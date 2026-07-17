<template>
  <div>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="搜索用户名、昵称或邮箱" clearable class="search-input" @keyup.enter="search" />
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <el-table :data="users" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="nickname" label="昵称" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="200" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }"><el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role === 'ADMIN' ? '管理员' : '用户' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }"><el-tag :type="row.status === 'BANNED' ? 'danger' : 'success'">{{ row.status === 'BANNED' ? '已禁用' : '正常' }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" :formatter="formatDateCell" />
      <el-table-column prop="lastLoginTime" label="最后登录" width="180" :formatter="formatDateCell" />
      <el-table-column prop="banReason" label="封禁原因" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.role !== 'ADMIN'" size="small" :type="row.status === 'BANNED' ? 'success' : 'danger'" @click="toggleStatus(row)">
            {{ row.status === 'BANNED' ? '恢复' : '禁用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="pagination" background layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[10, 20, 50]" v-model:current-page="pageNum" v-model:page-size="pageSize" @current-change="loadUsers" @size-change="loadUsers" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserStatus } from '@/api/user'
import { formatDateCell } from '@/utils/format'

const users = ref([])
const loading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

async function loadUsers() {
  loading.value = true
  try {
    const res = await getUsers({ pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value.trim() || undefined })
    users.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}
function search() { pageNum.value = 1; loadUsers() }
async function toggleStatus(user) {
  const nextStatus = user.status === 'BANNED' ? 'ACTIVE' : 'BANNED'
  let reason
  if (nextStatus === 'BANNED') {
    const result = await ElMessageBox.prompt(`请输入禁用用户“${user.username}”的原因`, '禁用用户', {
      type: 'warning', inputPattern: /\S+/, inputErrorMessage: '封禁原因不能为空', inputPlaceholder: '例如：发布违规内容',
    })
    reason = result.value.trim()
  } else {
    await ElMessageBox.confirm(`确定恢复用户“${user.username}”吗？`, '恢复用户', { type: 'warning' })
  }
  await updateUserStatus(user.id, nextStatus, reason)
  ElMessage.success(nextStatus === 'BANNED' ? '用户已禁用' : '用户已恢复')
  loadUsers()
}
onMounted(loadUsers)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; } .search-input { width: 320px; } .pagination { margin-top: 16px; justify-content: flex-end; }
</style>
