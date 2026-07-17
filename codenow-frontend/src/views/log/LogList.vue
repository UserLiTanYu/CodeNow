<template>
  <div>
    <el-table :data="logs" v-loading="loading" stripe>
      <el-table-column prop="username" label="操作人" width="100" />
      <el-table-column prop="operation" label="操作类型" width="140" />
      <el-table-column prop="method" label="请求方法" width="200" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP 地址" width="140" />
      <el-table-column prop="duration" label="耗时(ms)" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="请求参数" min-width="200">
        <template #default="{ row }">
          <el-popover trigger="hover" width="400" v-if="row.params && row.params !== '{}'">
            <template #reference>
              <el-button size="small" link>查看参数</el-button>
            </template>
            <pre class="params-pre">{{ formatParams(row.params) }}</pre>
          </el-popover>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="操作时间" width="180" :formatter="formatDateCell" />
    </el-table>

    <el-pagination
      class="pagination"
      background
      layout="total, sizes, prev, pager, next"
      :total="total"
      :page-sizes="[13, 20, 50]"
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      @current-change="fetchLogs"
      @size-change="fetchLogs"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import { formatDateCell } from '@/utils/format'

const logs = ref([])
const loading = ref(false)
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(13)

function formatParams(params) {
  try {
    return JSON.stringify(JSON.parse(params), null, 2)
  } catch {
    return params
  }
}

async function fetchLogs() {
  loading.value = true
  try {
    const res = await request.get('/logs', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value },
    })
    logs.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped>
.pagination {
  margin-top: 16px;
  justify-content: center;
}
.params-pre {
  margin: 0;
  font-size: 12px;
  max-height: 300px;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
.text-muted {
  color: #c0c4cc;
}
</style>
