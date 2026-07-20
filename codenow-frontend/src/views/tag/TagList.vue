<template>
  <div>
    <!-- 工具栏：新增标签输入框和按钮 -->
    <div class="toolbar">
      <el-input v-model="newTagName" placeholder="输入标签名称" style="width: 200px; margin-right: 10px" @keyup.enter="handleAdd" />
      <el-button type="primary" :loading="adding" @click="handleAdd">新增标签</el-button>
    </div>

    <!-- 标签列表表格 -->
    <el-table :data="pagedTags" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="100" />
      <el-table-column prop="name" label="标签名称" />
      <el-table-column prop="createTime" label="创建时间" width="180" :formatter="formatDateCell" />
      <!-- 操作列：编辑和删除 -->
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页器 -->
    <el-pagination
      v-model:current-page="pageNum"
      class="pagination"
      background
      layout="total, prev, pager, next"
      :page-size="pageSize"
      :total="tags.length"
    />

    <!-- 编辑标签对话框 -->
    <el-dialog v-model="editVisible" title="编辑标签" width="420px">
      <el-input v-model="editName" maxlength="50" show-word-limit @keyup.enter="handleUpdate" />
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="updating" @click="handleUpdate">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/** 管理员标签管理页面 - 支持标签的增删改查操作 */
import { computed, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getTags, createTag, updateTag, deleteTag } from '@/api/tag'
import { formatDateCell } from '@/utils/format'

/** 保存和删除后始终重新读取服务端列表，避免本地状态与唯一性校验结果不一致 */
const tags = ref([])
const loading = ref(false)
const adding = ref(false)
const newTagName = ref('')
const editVisible = ref(false)
const editId = ref(null)
const editName = ref('')
const updating = ref(false)
const pageNum = ref(1)
const pageSize = 11
/** 前端分页：从全部标签中截取当前页数据 */
const pagedTags = computed(() => {
  const start = (pageNum.value - 1) * pageSize
  return tags.value.slice(start, start + pageSize)
})

/** 加载全部标签数据 */
async function loadTags() {
  loading.value = true
  try {
    const res = await getTags()
    tags.value = res.data
    // 确保当前页码不超过实际总页数
    const lastPage = Math.max(1, Math.ceil(tags.value.length / pageSize))
    if (pageNum.value > lastPage) pageNum.value = lastPage
  } finally {
    loading.value = false
  }
}

/** 新增标签 */
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

/** 删除标签（需二次确认） */
async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该标签？', '提示', { type: 'warning' })
  await deleteTag(id)
  ElMessage.success('删除成功')
  loadTags()
}

/** 打开编辑标签对话框 */
function handleEdit(row) {
  editId.value = row.id
  editName.value = row.name
  editVisible.value = true
}

/** 保存标签编辑 */
async function handleUpdate() {
  const name = editName.value.trim()
  if (!name) {
    ElMessage.warning('标签名称不能为空')
    return
  }
  updating.value = true
  try {
    await updateTag(editId.value, { name })
    ElMessage.success('修改成功')
    editVisible.value = false
    await loadTags()
  } finally {
    updating.value = false
  }
}

/** 页面挂载时加载标签数据 */
onMounted(loadTags)
</script>

<style scoped>
.toolbar {
  display: flex;
  margin-bottom: 16px;
}
.pagination {
  margin-top: 16px;
}
</style>
