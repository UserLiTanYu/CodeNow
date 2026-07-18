<template>
  <div class="article-editor">
    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" show-icon :closable="false" />
      <el-button type="primary" @click="loadArticle">重新加载</el-button>
    </div>
    <el-form
      v-else
      ref="formRef"
      v-loading="initializing"
      class="article-form"
      :disabled="initializing"
      :model="form"
      :rules="rules"
      label-width="80px"
    >
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入文章标题" />
      </el-form-item>
      <el-form-item label="分类" prop="categoryId">
        <el-cascader
          v-model="form.categoryId"
          :options="categoryOptions"
          :props="{ emitPath: false, checkStrictly: true }"
          placeholder="请选择分类或子分类"
          clearable
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%">
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="文章摘要（可选）" />
      </el-form-item>
      <el-form-item label="学习顺序">
        <el-input-number v-model="form.sort" :min="0" :max="9999" />
        <span class="sort-tip">数字越小越靠前</span>
      </el-form-item>
      <el-form-item v-if="adminTools || imageTools" label="封面图">
        <ImageUpload v-model="form.coverImage" :upload-request="uploadImageRequest" />
      </el-form-item>
      <el-form-item v-else label="封面图">
        <span class="stage-tip">作者图片上传将在下一阶段开放</span>
      </el-form-item>
      <el-form-item class="content-form-item" label="内容" prop="content">
        <div class="editor-toolbar">
          <input
            ref="documentInputRef"
            class="document-input"
            type="file"
            accept=".md,.txt,text/markdown,text/plain"
            @change="handleDocumentSelected"
          />
          <input
            v-if="adminTools"
            ref="packageInputRef"
            class="document-input"
            type="file"
            accept=".zip,application/zip,application/x-zip-compressed"
            @change="handlePackageSelected"
          />
          <el-button size="small" :loading="importing" @click="documentInputRef?.click()">
            <el-icon><Upload /></el-icon> 导入文档
          </el-button>
          <el-button v-if="adminTools" size="small" :loading="packageImporting" @click="packageInputRef?.click()">
            <el-icon><FolderOpened /></el-icon> 导入 ZIP 文章包
          </el-button>
          <el-button v-if="adminTools || imageTools" size="small" @click="showImageUpload = true">
            <el-icon><Picture /></el-icon> 插入图片
          </el-button>
          <span class="import-tip">.md/.txt 最大 2MB{{ adminTools ? '；含本地图片请使用 ZIP 包，最大 25MB' : '' }}</span>
        </div>
        <MdEditor v-model="form.content" style="height: 500px" />
      </el-form-item>

      <el-dialog v-if="adminTools || imageTools" v-model="showImageUpload" title="插入图片" width="450px">
        <ImageUpload v-model="insertImageUrl" :upload-request="uploadImageRequest" />
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
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { FolderOpened, Picture, Upload } from '@element-plus/icons-vue'
import { MdEditor } from 'md-editor-v3'
import 'md-editor-v3/lib/style.css'
import ImageUpload from '@/components/ImageUpload.vue'
import { importArticlePackage } from '@/api/upload'
import { DOCUMENT_IMPORT_EXTENSIONS, DOCUMENT_IMPORT_MAX_SIZE, documentExtension, parseTextDocument } from '@/utils/documentImport'
import { categoryCascaderOptions } from '@/utils/categoryTree'

const props = defineProps({
  articleApi: { type: Object, required: true },
  loadCategories: { type: Function, required: true },
  loadTags: { type: Function, required: true },
  redirectPath: { type: String, required: true },
  adminTools: { type: Boolean, default: false },
  imageTools: { type: Boolean, default: false },
  uploadImageRequest: { type: Function, default: undefined },
})

const route = useRoute()
const router = useRouter()
const formRef = ref()
const saving = ref(false)
const categories = ref([])
const tags = ref([])
const isEdit = ref(false)
const articleLoading = ref(false)
const optionsLoading = ref(true)
const loadError = ref('')
const showImageUpload = ref(false)
const insertImageUrl = ref('')
const initialSnapshot = ref('')
const allowLeave = ref(false)
const documentInputRef = ref()
const importing = ref(false)
const packageInputRef = ref()
const packageImporting = ref(false)
const categoryOptions = computed(() => categoryCascaderOptions(categories.value))
const initializing = computed(() => articleLoading.value || optionsLoading.value)

const form = reactive({
  title: '',
  content: '',
  summary: '',
  coverImage: '',
  categoryId: null,
  tagIds: [],
  status: 0,
  sort: 0,
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

async function loadOptions() {
  optionsLoading.value = true
  try {
    const [categoryResult, tagResult] = await Promise.allSettled([props.loadCategories(), props.loadTags()])
    if (categoryResult.status === 'fulfilled') categories.value = categoryResult.value.data
    if (tagResult.status === 'fulfilled') tags.value = tagResult.value.data
  } finally {
    optionsLoading.value = false
  }
}

async function loadArticle() {
  if (!route.params.id) return
  isEdit.value = true
  articleLoading.value = true
  loadError.value = ''
  try {
    const response = await props.articleApi.get(route.params.id)
    const article = response.data.article
    Object.assign(form, {
      title: article.title,
      content: article.content,
      summary: article.summary,
      coverImage: article.coverImage,
      categoryId: article.categoryId,
      status: article.status,
      sort: article.sort || 0,
      tagIds: response.data.tags.map((tag) => tag.id),
    })
    initialSnapshot.value = snapshotForm()
  } catch {
    loadError.value = '文章加载失败，请检查网络后重试'
  } finally {
    articleLoading.value = false
  }
}

async function handleSave(status) {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = { ...form, status }
    if (isEdit.value) {
      await props.articleApi.update(route.params.id, data)
      ElMessage.success('修改成功')
    } else {
      await props.articleApi.create(data)
      ElMessage.success('创建成功')
    }
    allowLeave.value = true
    router.push(props.redirectPath)
  } finally {
    saving.value = false
  }
}

function snapshotForm() {
  return JSON.stringify({ ...form, tagIds: [...form.tagIds].sort((a, b) => a - b) })
}

function hasUnsavedChanges() {
  return !allowLeave.value && initialSnapshot.value !== '' && snapshotForm() !== initialSnapshot.value
}

function handleBeforeUnload(event) {
  if (!hasUnsavedChanges()) return
  event.preventDefault()
  event.returnValue = ''
}

onBeforeRouteLeave(async () => {
  if (!hasUnsavedChanges()) return true
  try {
    await ElMessageBox.confirm('当前文章有未保存的修改，确定离开吗？', '未保存的修改', {
      type: 'warning',
      confirmButtonText: '离开',
      cancelButtonText: '继续编辑',
    })
    return true
  } catch {
    return false
  }
})

function handleInsertImage() {
  if (!insertImageUrl.value) return
  form.content = `${form.content}\n![图片](${insertImageUrl.value})\n`
  showImageUpload.value = false
  insertImageUrl.value = ''
}

async function handleDocumentSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  const extension = documentExtension(file.name)
  if (!DOCUMENT_IMPORT_EXTENSIONS.includes(extension)) return ElMessage.error('仅支持导入 .md 和 .txt 文档')
  if (file.size > DOCUMENT_IMPORT_MAX_SIZE) return ElMessage.error('文档大小不能超过 2MB')
  if (form.content.trim()) {
    try {
      await ElMessageBox.confirm('导入文档会覆盖当前正文，确定继续吗？', '导入文档', {
        type: 'warning',
        confirmButtonText: '继续导入',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
  }
  importing.value = true
  try {
    const imported = parseTextDocument(file.name, await file.text())
    if (!imported.content.trim()) return ElMessage.warning('文档内容为空')
    form.content = imported.content
    if (!form.title.trim() && imported.title) form.title = imported.title.slice(0, 200)
    ElMessage.success(`已导入 ${file.name}，请检查内容后再保存`)
  } catch {
    ElMessage.error('文档读取失败，请确认文件为 UTF-8 编码')
  } finally {
    importing.value = false
  }
}

async function handlePackageSelected(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  if (!file.name.toLowerCase().endsWith('.zip')) return ElMessage.error('仅支持导入 .zip 文章包')
  if (file.size > 25 * 1024 * 1024) return ElMessage.error('ZIP 文章包不能超过 25MB')
  if (form.content.trim()) {
    try {
      await ElMessageBox.confirm('导入 ZIP 文章包会覆盖当前正文，确定继续吗？', '导入 ZIP 文章包', {
        type: 'warning',
        confirmButtonText: '继续导入',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
  }
  packageImporting.value = true
  try {
    const data = new FormData()
    data.append('file', file)
    const response = await importArticlePackage(data)
    form.content = response.data.content
    if (!form.title.trim() && response.data.title) form.title = response.data.title.slice(0, 200)
    ElMessage.success(`文章包导入成功，已处理 ${response.data.imageCount} 张图片，请检查后再保存`)
  } finally {
    packageImporting.value = false
  }
}

onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  if (!route.params.id) initialSnapshot.value = snapshotForm()
  await Promise.allSettled([loadOptions(), loadArticle()])
})

onBeforeUnmount(() => window.removeEventListener('beforeunload', handleBeforeUnload))
</script>

<style scoped>
.article-editor { width: 100%; box-sizing: border-box; }
.load-error { display: flex; align-items: center; gap: 12px; }
.load-error .el-alert { flex: 1; }
.article-form > .el-form-item { max-width: 350px; }
.article-form > .content-form-item { max-width: none; margin-right: 80px; }
.article-editor :deep(.el-form-item__content) { min-width: 0; }
.article-editor :deep(.md-editor) { width: 100%; }
.editor-toolbar { margin-bottom: 8px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.document-input { display: none; }
.import-tip, .sort-tip, .stage-tip { color: #909399; font-size: 12px; }
.sort-tip { margin-left: 10px; }
@media (max-width: 768px) { .article-form > .content-form-item { margin-right: 0; } }
</style>
