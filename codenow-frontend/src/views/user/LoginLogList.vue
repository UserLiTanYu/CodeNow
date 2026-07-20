<template>
  <div>
    <!-- 筛选工具栏：按账号和登录结果筛选 -->
    <div class="toolbar">
      <el-input v-model="account" placeholder="搜索登录账号" clearable class="search-input" @keyup.enter="search" />
      <el-select v-model="success" placeholder="全部结果" clearable style="width: 140px">
        <el-option label="登录成功" :value="1" /><el-option label="登录失败" :value="0" />
      </el-select>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
    <!-- 登录日志列表表格 -->
    <el-table v-loading="loading" :data="logs" stripe>
      <el-table-column prop="account" label="登录账号" min-width="150" />
      <el-table-column prop="userId" label="用户ID" width="90" />
      <el-table-column label="结果" width="100"><template #default="{ row }"><el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag></template></el-table-column>
      <el-table-column prop="failureReason" label="失败原因" min-width="180" />
      <el-table-column prop="ip" label="IP地址" min-width="140" />
      <el-table-column prop="userAgent" label="客户端" min-width="260" show-overflow-tooltip />
      <el-table-column prop="createTime" label="登录时间" width="180" :formatter="formatDateCell" />
    </el-table>
    <!-- 分页器 -->
    <el-pagination class="pagination" background layout="total, sizes, prev, pager, next" :total="total" :page-sizes="[10, 20, 50]" v-model:current-page="pageNum" v-model:page-size="pageSize" @current-change="load" @size-change="load" />
  </div>
</template>

<script setup>
/** 管理员登录日志页面 - 查看用户登录记录，支持按账号和结果筛选 */
import { onMounted, ref } from 'vue'
import { getLoginLogs } from '@/api/user'
import { formatDateCell } from '@/utils/format'
const logs = ref([]); const loading = ref(false); const account = ref(''); const success = ref(); const pageNum = ref(1); const pageSize = ref(10); const total = ref(0)
/** 加载登录日志列表，支持按账号和登录结果筛选 */
async function load() { loading.value = true; try { const res = await getLoginLogs({ pageNum: pageNum.value, pageSize: pageSize.value, account: account.value.trim() || undefined, success: success.value }); logs.value = res.data.records; total.value = res.data.total } finally { loading.value = false } }
/** 重置到第一页并重新加载 */
function search() { pageNum.value = 1; load() }
/** 页面挂载时加载登录日志 */
onMounted(load)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; display: flex; gap: 10px; } .search-input { width: 300px; } .pagination { margin-top: 16px; justify-content: flex-end; }
</style>
