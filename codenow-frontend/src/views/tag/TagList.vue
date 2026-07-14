<template>
  <div>
    <div class="toolbar">
      <el-input v-model="newTagName" placeholder="输入标签名称" style="width: 200px; margin-right: 10px" @keyup.enter="handleAdd" />
      <el-button type="primary" :loading="adding" @click="handleAdd">新增标签</el-button>
    </div>

    <el-table :data="tags" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="100" />
      <el-table-column prop="name" label="标签名称" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getTags, createTag, deleteTag } from '@/api/tag'

const tags = ref([])
const loading = ref(false)
const adding = ref(false)
const newTagName = ref('')

async function loadTags() {
  loading.value = true
  try {
    const res = await getTags()
    tags.value = res.data
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  if (!newTagName.value.trim()) return
  adding.value = true
  try {
    await createTag({ name: newTagName.value.trim() })
    ElMessage.success('新增成功')
    newTagName.value = ''
    loadTags()
  } finally {
    adding.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该标签？', '提示', { type: 'warning' })
  await deleteTag(id)
  ElMessage.success('删除成功')
  loadTags()
}

onMounted(loadTags)
</script>

<style scoped>
.toolbar {
  display: flex;
  margin-bottom: 16px;
}
</style>
