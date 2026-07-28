<template>
  <div>
    <!-- 工具栏：新增一级分类按钮和提示信息 -->
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增一级分类</el-button>
      <span class="toolbar-tip">分类支持多级结构；删除前需先清空子分类和文章。</span>
    </div>

    <!-- 分类树形表格 -->
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
      <!-- 操作列：新增子分类、编辑、删除 -->
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="primary" plain @click="openDialog(null, row.id)">新增子分类</el-button>
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑分类对话框 -->
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
/** 管理员分类管理页面 - 支持多级分类的增删改查操作 */
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCategories, createCategory, updateCategory, deleteCategory } from '@/api/category'
import { categoryCascaderOptions } from '@/utils/categoryTree'
import { formatDateCell } from '@/utils/format'

/** 分类树由后端统一返回；表格、父分类选择器和删除约束均以同一棵树为数据源 */
const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref()
/** 分类表单数据 */
const form = reactive({ name: '', description: '', parentId: null, sort: 0 })
const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }
/** 对话框标题，根据编辑/新增模式动态显示 */
const dialogTitle = computed(() => isEdit.value ? '编辑分类' : form.parentId ? '新增子分类' : '新增一级分类')
/** 父分类级联选择器选项，编辑时排除自身子树 */
const parentOptions = computed(() => categoryCascaderOptions(categories.value, editId.value))

/** 加载分类树数据 */
async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategories()
    categories.value = res.data
  } finally {
    loading.value = false
  }
}

/** 打开新增/编辑分类对话框 */
function openDialog(row, parentId = null) {
  isEdit.value = Boolean(row)
  editId.value = row?.id || null
  form.name = row?.name || ''
  form.description = row?.description || ''
  form.parentId = row ? (row.parentId || null) : parentId
  form.sort = row?.sort || 0
  dialogVisible.value = true
}

/** 保存分类（新增或更新） */
async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = { ...form, parentId: form.parentId || 0 }
    if (isEdit.value) await updateCategory(editId.value, data)
    else await createCategory(data)
    ElMessage.success(isEdit.value ? '修改成功' : '新增成功')
    dialogVisible.value = false
    await loadCategories()
  } finally {
    saving.value = false
  }
}

/** 删除分类（需二次确认） */
async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除分类"${row.name}"？`, '删除分类', { type: 'warning' })
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  await loadCategories()
}

/** 页面挂载时加载分类数据 */
onMounted(loadCategories)
</script>

<style scoped>
.toolbar { margin-bottom: 16px; display: flex; align-items: center; gap: 12px; }
.toolbar-tip { color: #909399; font-size: 13px; }
</style>
