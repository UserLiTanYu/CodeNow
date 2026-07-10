<template>
  <div>
    <div class="toolbar">
      <el-button type="primary" @click="openDialog()">新增分类</el-button>
    </div>

    <el-table :data="categories" v-loading="loading" stripe>
      <el-table-column prop="name" label="分类名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑分类' : '新增分类'" width="400px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const categories = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const isEdit = ref(false)
const editId = ref(null)
const formRef = ref()

const form = reactive({ name: '', description: '', sort: 0 })
const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

async function loadCategories() {
  loading.value = true
  try {
    const res = await request.get('/categories')
    categories.value = res.data
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.name = row.name
    form.description = row.description
    form.sort = row.sort
  } else {
    isEdit.value = false
    editId.value = null
    form.name = ''
    form.description = ''
    form.sort = 0
  }
  dialogVisible.value = true
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (isEdit.value) {
      await request.put(`/categories/${editId.value}`, { ...form })
      ElMessage.success('修改成功')
    } else {
      await request.post('/categories', { ...form })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadCategories()
  } finally {
    saving.value = false
  }
}

async function handleDelete(id) {
  await ElMessageBox.confirm('确定删除该分类？', '提示', { type: 'warning' })
  await request.delete(`/categories/${id}`)
  ElMessage.success('删除成功')
  loadCategories()
}

onMounted(loadCategories)
</script>

<style scoped>
.toolbar {
  margin-bottom: 16px;
}
</style>
