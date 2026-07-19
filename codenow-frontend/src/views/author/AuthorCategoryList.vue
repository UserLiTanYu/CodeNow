<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增一级分类</el-button>
      <span class="toolbar-tip">分类支持多级结构；删除前需先清空子分类和文章。</span>
    </div>

    <el-table
      :data="categories"
      v-loading="loading"
      row-key="id"
      :tree-props="{ children: 'children' }"
      stripe
    >
      <el-table-column prop="name" label="分类名称" min-width="200" />
      <el-table-column prop="description" label="描述" min-width="260" />
      <el-table-column prop="sort" label="排序" width="90" />
      <el-table-column prop="createTime" label="创建时间" width="180" :formatter="formatDateCell" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openDialog(null, row.id)">新增子分类</el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="父分类">
          <el-cascader
            v-model="form.parentId"
            :options="parentOptions"
            :props="{ emitPath: false, checkStrictly: true }"
            placeholder="留空表示一级分类"
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="50" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
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
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAuthorCategories, createAuthorCategory, updateAuthorCategory, deleteAuthorCategory } from '@/api/authorConsole'

const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ parentId: null, name: '', description: '', sort: 0 })
const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }

const dialogTitle = computed(() => editingId.value ? '编辑分类' : '新增分类')
const parentOptions = computed(() => buildOptions(categories.value, editingId.value))

function buildOptions(nodes, excludeId, prefix = '') {
  const result = []
  for (const node of nodes) {
    if (node.id === excludeId) continue
    result.push({ value: node.id, label: prefix + node.name, children: buildOptions(node.children || [], excludeId, prefix + node.name + ' / ') })
  }
  return result
}

function formatDateCell(_row, _col, value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '-'
}

async function load() {
  loading.value = true
  try {
    const res = await getAuthorCategories()
    categories.value = res.data || []
  } finally {
    loading.value = false
  }
}

function openDialog(row, parentId) {
  editingId.value = row?.id || null
  form.parentId = row ? (row.parentId || null) : (parentId || null)
  form.name = row?.name || ''
  form.description = row?.description || ''
  form.sort = row?.sort ?? 0
  dialogVisible.value = true
}

async function handleSave() {
  await formRef.value.validate()
  saving.value = true
  try {
    const payload = { ...form }
    if (editingId.value) await updateAuthorCategory(editingId.value, payload)
    else await createAuthorCategory(payload)
    ElMessage.success(editingId.value ? '分类已更新' : '分类已创建')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除分类', { type: 'warning' })
  await deleteAuthorCategory(row.id)
  ElMessage.success('分类已删除')
  await load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.toolbar-tip { color: #909399; font-size: 13px; }
</style>
