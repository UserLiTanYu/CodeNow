<template>
  <div class="article-edit">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入文章标题" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-select v-model="form.categoryId" placeholder="请选择分类" style="width: 100%">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%">
          <el-option v-for="t in tags" :key="t.id" :label="t.name" :value="t.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="文章摘要（可选）" />
      </el-form-item>
      <el-form-item label="封面图">
        <ImageUpload v-model="form.coverImage" />
      </el-form-item>
      <el-form-item label="内容" prop="content">
        <div class="editor-toolbar">
          <el-button size="small" @click="showImageUpload = true">
            <el-icon><Picture /></el-icon> 插入图片
          </el-button>
        </div>
        <md-editor v-model="form.content" style="height: 500px" />
      </el-form-item>

      <!-- 插入图片弹窗 -->
      <el-dialog v-model="showImageUpload" title="插入图片" width="450px">
        <ImageUpload v-model="insertImageUrl" />
        <template #footer>
          <el-button @click="showImageUpload = false">取消</el-button>
          <el-button type="primary" :disabled="!insertImageUrl" @click="handleInsertImage">插入</el-button>
        </template>
      </el-dialog>
      <el-form-item>
        <el-button @click="router.back()">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave(0)">保存草稿</el-button>
        <el-button type="success" :loading="saving" @click="handleSave(1)">发布</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Picture } from '@element-plus/icons-vue'
import request from '@/utils/request'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import ImageUpload from '@/components/ImageUpload.vue'

const route = useRoute()
const router = useRouter()
const formRef = ref()
const saving = ref(false)
const categories = ref([])
const tags = ref([])
const isEdit = ref(false)
const showImageUpload = ref(false)
const insertImageUrl = ref('')

const form = reactive({
  title: '',
  content: '',
  summary: '',
  coverImage: '',
  categoryId: null,
  tagIds: [],
  status: 0,
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

async function loadOptions() {
  const [catRes, tagRes] = await Promise.all([request.get('/categories'), request.get('/tags')])
  categories.value = catRes.data
  tags.value = tagRes.data
}

async function loadArticle() {
  const id = route.params.id
  if (!id) return
  isEdit.value = true
  const res = await request.get(`/articles/${id}`)
  const a = res.data.article
  form.title = a.title
  form.content = a.content
  form.summary = a.summary
  form.coverImage = a.coverImage
  form.categoryId = a.categoryId
  form.status = a.status
  form.tagIds = res.data.tags.map((t) => t.id)
}

async function handleSave(status) {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = { ...form, status }
    if (isEdit.value) {
      await request.put(`/articles/${route.params.id}`, data)
      ElMessage.success('修改成功')
    } else {
      await request.post('/articles', data)
      ElMessage.success('创建成功')
    }
    router.push('/articles')
  } finally {
    saving.value = false
  }
}

function handleInsertImage() {
  if (!insertImageUrl.value) return
  const mdImage = `![图片](${insertImageUrl.value})`
  form.content = form.content + '\n' + mdImage + '\n'
  showImageUpload.value = false
  insertImageUrl.value = ''
}

onMounted(() => {
  loadOptions()
  loadArticle()
})
</script>

<style scoped>
.article-edit {
  max-width: 900px;
}
.editor-toolbar {
  margin-bottom: 8px;
}
</style>
