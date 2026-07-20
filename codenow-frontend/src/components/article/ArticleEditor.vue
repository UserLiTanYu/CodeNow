<template>
  <!-- 文章编辑器组件：支持新建和编辑文章，包含表单校验、Markdown编辑器、文档导入等功能 -->
  <div class="article-editor">
    <!-- 加载错误提示 -->
    <div v-if="loadError" class="load-error">
      <el-alert :title="loadError" type="error" show-icon :closable="false" />
      <el-button type="primary" @click="loadArticle">重新加载</el-button>
    </div>
    <!-- 文章编辑表单 -->
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
      <!-- 文章标题输入 -->
      <el-form-item label="标题" prop="title">
        <el-input v-model="form.title" placeholder="请输入文章标题" />
      </el-form-item>
      <!-- 文章分类选择（级联选择器，支持父子分类） -->
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
      <!-- 文章标签多选 -->
      <el-form-item label="标签">
        <el-select v-model="form.tagIds" multiple placeholder="请选择标签" style="width: 100%">
          <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
        </el-select>
      </el-form-item>
      <!-- 文章摘要 -->
      <el-form-item label="摘要">
        <el-input v-model="form.summary" type="textarea" :rows="2" placeholder="文章摘要（可选）" />
      </el-form-item>
      <!-- 学习顺序：数字越小越靠前 -->
      <el-form-item label="学习顺序">
        <el-input-number v-model="form.sort" :min="0" :max="9999" />
        <span class="sort-tip">数字越小越靠前</span>
      </el-form-item>
      <!-- 封面图上传（权限控制：管理员和有图片权限的作者可上传） -->
      <el-form-item v-if="adminTools || imageTools" label="封面图">
        <ImageUpload v-model="form.coverImage" :upload-request="uploadImageRequest" />
      </el-form-item>
      <el-form-item v-else label="封面图">
        <span class="stage-tip">作者图片上传将在下一阶段开放</span>
      </el-form-item>
      <!-- 文章内容编辑区：工具栏 + Markdown 编辑器 -->
      <el-form-item class="content-form-item" label="内容" prop="content">
        <!-- 编辑器工具栏：文档导入、ZIP包导入、插入图片 -->
        <div class="editor-toolbar">
          <!-- 隐藏的文档文件输入框 -->
          <input
            ref="documentInputRef"
            class="document-input"
            type="file"
            accept=".md,.txt,text/markdown,text/plain"
            @change="handleDocumentSelected"
          />
          <!-- 隐藏的ZIP包输入框（仅管理员可见） -->
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
        <!-- Markdown 编辑器 -->
        <MdEditor v-model="form.content" style="height: 500px" />
      </el-form-item>

      <!-- 图片插入对话框：上传图片后插入到 Markdown 内容中 -->
      <el-dialog v-if="adminTools || imageTools" v-model="showImageUpload" title="插入图片" width="450px">
        <ImageUpload v-model="insertImageUrl" :upload-request="uploadImageRequest" />
        <template #footer>
          <el-button @click="showImageUpload = false">取消</el-button>
          <el-button type="primary" :disabled="!insertImageUrl" @click="handleInsertImage">插入</el-button>
        </template>
      </el-dialog>
      <!-- 底部操作按钮：取消、保存草稿、发布 -->
      <el-form-item>
        <el-button @click="router.back()">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave(0)">保存草稿</el-button>
        <el-button type="success" :loading="saving" @click="handleSave(1)">发布</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup>
/**
 * 文章编辑器组件
 * 支持新建和编辑文章，包含标题、分类、标签、摘要、封面图、Markdown 内容编辑等功能。
 * 编辑器通过 props 注入管理端/作者端 API，共享表单和离开保护，同时保持各自的权限与上传能力。
 * 支持导入 .md/.txt 文档和 ZIP 文章包（含图片），并提供未保存修改的离开保护。
 */
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

/**
 * 组件属性
 * articleApi: 文章 CRUD 接口对象（管理端和作者端使用不同实现）
 * loadCategories/loadTags: 加载分类和标签的函数
 * redirectPath: 保存成功后的跳转路径
 * adminTools: 是否启用管理员专属工具（ZIP导入等）
 * imageTools: 是否启用图片上传功能
 * uploadImageRequest: 自定义图片上传接口
 */
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

/** 表单引用，用于触发表单校验 */
const formRef = ref()
/** 是否正在保存中 */
const saving = ref(false)
/** 分类列表 */
const categories = ref([])
/** 标签列表 */
const tags = ref([])
/** 是否为编辑模式（URL 中有文章ID） */
const isEdit = ref(false)
/** 文章数据是否正在加载 */
const articleLoading = ref(false)
/** 分类和标签选项是否正在加载 */
const optionsLoading = ref(true)
/** 加载错误信息 */
const loadError = ref('')
/** 是否显示图片插入对话框 */
const showImageUpload = ref(false)
/** 待插入的图片URL */
const insertImageUrl = ref('')
/** 表单初始快照，用于检测未保存修改 */
const initialSnapshot = ref('')
/** 是否允许离开当前页面（保存成功后置为 true） */
const allowLeave = ref(false)
/** 文档文件输入框引用 */
const documentInputRef = ref()
/** 是否正在导入文档 */
const importing = ref(false)
/** ZIP 包输入框引用 */
const packageInputRef = ref()
/** 是否正在导入 ZIP 文章包 */
const packageImporting = ref(false)

/** 将分类列表转换为级联选择器所需的树形结构 */
const categoryOptions = computed(() => categoryCascaderOptions(categories.value))
/** 是否处于初始化加载状态（文章或选项加载中） */
const initializing = computed(() => articleLoading.value || optionsLoading.value)

/** 文章表单数据 */
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

/** 表单校验规则 */
const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

/** 加载分类和标签选项数据，并行请求互不影响 */
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

/** 加载文章详情（编辑模式）：从接口获取文章数据并填充到表单中 */
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

/** 保存文章：校验表单后根据模式调用创建或更新接口，成功后跳转到指定页面 */
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

/** 生成表单当前状态的 JSON 快照（标签ID排序后序列化） */
function snapshotForm() {
  return JSON.stringify({ ...form, tagIds: [...form.tagIds].sort((a, b) => a - b) })
}

/** 检测表单是否有未保存的修改：比较当前快照与初始快照 */
function hasUnsavedChanges() {
  return !allowLeave.value && initialSnapshot.value !== '' && snapshotForm() !== initialSnapshot.value
}

/** 浏览器关闭/刷新前的保护：有未保存修改时提示用户 */
function handleBeforeUnload(event) {
  if (!hasUnsavedChanges()) return
  event.preventDefault()
  event.returnValue = ''
}

/** 路由离开前的保护：有未保存修改时弹出确认对话框 */
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

/** 将上传的图片以 Markdown 图片语法插入到文章内容末尾 */
function handleInsertImage() {
  if (!insertImageUrl.value) return
  form.content = `${form.content}\n![图片](${insertImageUrl.value})\n`
  showImageUpload.value = false
  insertImageUrl.value = ''
}

/** 处理文档导入：读取 .md/.txt 文件内容，覆盖当前正文，可选提取标题 */
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

/** 处理 ZIP 文章包导入：上传 ZIP 文件到服务端，服务端解析后返回文章内容和图片数量 */
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

/** 组件挂载：注册浏览器关闭保护，初始化快照，并行加载选项和文章数据 */
onMounted(async () => {
  window.addEventListener('beforeunload', handleBeforeUnload)
  if (!route.params.id) initialSnapshot.value = snapshotForm()
  await Promise.allSettled([loadOptions(), loadArticle()])
})

/** 组件卸载前：移除浏览器关闭保护事件监听 */
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
