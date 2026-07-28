<template>
  <div>
    <!-- 工具栏：新增标签按钮和提示信息 -->
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增标签</el-button>
      <span class="toolbar-tip">标签用于文章分类标记，删除前请确保没有文章使用。</span>
    </div>

    <!-- 标签列表表格 -->
    <el-table :data="tags" v-loading="loading" stripe>
      <el-table-column prop="name" label="标签名称" min-width="200" />
      <el-table-column prop="createTime" label="创建时间" width="180" :formatter="formatDateCell" />
      <!-- 操作列：编辑和删除 -->
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑标签对话框 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑标签' : '新增标签'" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/** 作者标签管理页面 - 支持标签的增删改查操作 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAuthorTags, createAuthorTag, updateAuthorTag, deleteAuthorTag } from '@/api/authorConsole'

const tags = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
/** 标签表单数据 */
const form = reactive({ name: '' })
const rules = { name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }] }

/** 格式化时间为中文本地格式 */
function formatDateCell(_row, _col, value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

/** 加载标签列表数据 */
async function load() {
  loading.value = true
  try {
    const res = await getAuthorTags()
    tags.value = res.data || []
  } finally {
    loading.value = false
  }
}

/** 打开新增/编辑标签对话框 */
function openDialog(row) {
  editingId.value = row?.id || null
  form.name = row?.name || ''
  dialogVisible.value = true
}

/** 保存标签（新增或更新） */
async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editingId.value) await updateAuthorTag(editingId.value, { name: form.name.trim() })
    else await createAuthorTag({ name: form.name.trim() })
    ElMessage.success(editingId.value ? '标签已更新' : '标签已创建')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

/** 删除标签（需二次确认） */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除标签「${row.name}」吗？`, '删除标签', { type: 'warning' })
  await deleteAuthorTag(row.id)
  ElMessage.success('标签已删除')
  await load()
}

/** 页面挂载时加载标签数据 */
onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.toolbar-tip { color: #909399; font-size: 13px; }
</style>
