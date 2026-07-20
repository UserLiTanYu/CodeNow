<template>
  <!-- 图片上传组件：支持点击选择和拖拽上传，上传后显示预览图 -->
  <div class="image-upload">
    <!-- 未上传状态：显示上传区域（支持点击和拖拽） -->
    <div
      v-if="!imageUrl"
      class="upload-area"
      :class="{ 'is-dragover': isDragover }"
      @dragover.prevent="isDragover = true"
      @dragleave="isDragover = false"
      @drop.prevent="handleDrop"
      @click="triggerInput"
    >
      <el-icon class="upload-icon"><Plus /></el-icon>
      <p class="upload-text">点击或拖拽图片到此处上传</p>
      <p class="upload-hint">支持 JPG、PNG、GIF、WebP，最大 5MB</p>
    </div>
    <!-- 已上传状态：显示预览图和操作按钮 -->
    <div v-else class="preview-area">
      <img :src="imageUrl" class="preview-image" />
      <div class="preview-actions">
        <el-button size="small" @click="triggerInput">更换</el-button>
        <el-button size="small" type="danger" @click="handleRemove">删除</el-button>
      </div>
    </div>
    <!-- 隐藏的文件输入框，通过点击上传区域触发 -->
    <input
      ref="inputRef"
      type="file"
      accept="image/jpeg,image/png,image/gif,image/webp"
      style="display: none"
      @change="handleFileChange"
    />
  </div>
</template>

<script setup>
/**
 * 图片上传组件
 * 支持点击选择和拖拽两种上传方式，上传前进行客户端 MIME 类型和文件大小校验。
 * 通过 v-model 双向绑定图片 URL，支持通过 uploadRequest 属性注入不同的上传接口。
 */
import { ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/upload'

/**
 * 组件属性
 * uploadRequest 是权限域注入点：管理端使用默认接口，作者编辑器传入 owner-scoped 上传接口。
 * 客户端 MIME/大小检查只提供快速反馈，服务端仍必须校验真实文件内容。
 */
const props = defineProps({
  modelValue: { type: String, default: '' },
  uploadRequest: { type: Function, default: uploadImage },
})

/** 事件：update:modelValue 更新绑定的图片URL */
const emit = defineEmits(['update:modelValue'])

/** 文件输入框引用 */
const inputRef = ref(null)
/** 当前图片URL（本地状态，与父组件 v-model 同步） */
const imageUrl = ref(props.modelValue)
/** 是否处于拖拽悬停状态 */
const isDragover = ref(false)
/** 是否正在上传中 */
const uploading = ref(false)
/** 允许上传的图片 MIME 类型集合 */
const ALLOWED_TYPES = new Set(['image/jpeg', 'image/png', 'image/gif', 'image/webp'])
/** 最大文件大小：5MB */
const MAX_FILE_SIZE = 5 * 1024 * 1024

/** 监听父组件传入的 modelValue 变化，同步到本地状态 */
watch(
  () => props.modelValue,
  (val) => {
    imageUrl.value = val
  },
)

/** 触发隐藏的文件输入框点击事件 */
function triggerInput() {
  inputRef.value.click()
}

/** 处理文件拖放事件：提取第一个文件并上传 */
function handleDrop(e) {
  isDragover.value = false
  const file = e.dataTransfer.files[0]
  if (file) uploadFile(file)
}

/** 处理文件选择事件：提取选中的文件并上传，然后清空输入框 */
function handleFileChange(e) {
  const file = e.target.files[0]
  if (file) uploadFile(file)
  e.target.value = ''
}

/** 上传文件：校验类型和大小后，构造 FormData 调用上传接口，成功后更新图片URL */
async function uploadFile(file) {
  if (uploading.value) return
  if (!ALLOWED_TYPES.has(file.type)) {
    ElMessage.error('仅支持 JPG、PNG、GIF、WebP 图片')
    return
  }
  if (file.size > MAX_FILE_SIZE) {
    ElMessage.error('图片大小不能超过 5MB')
    return
  }
  uploading.value = true

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await props.uploadRequest(formData)
    imageUrl.value = res.data.url
    emit('update:modelValue', res.data.url)
    ElMessage.success('上传成功')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    uploading.value = false
  }
}

/** 删除已上传的图片：只清空表单中的 URL，不删除服务器上的文件 */
function handleRemove() {
  // 这里只解除表单中的 URL，不删除已经上传的服务器对象。
  imageUrl.value = ''
  emit('update:modelValue', '')
}
</script>

<style scoped>
.upload-area {
  border: 2px dashed #dcdfe6;
  border-radius: 8px;
  padding: 40px 20px;
  text-align: center;
  cursor: pointer;
  transition: border-color 0.2s;
}
.upload-area:hover,
.upload-area.is-dragover {
  border-color: #409eff;
}
.upload-icon {
  font-size: 40px;
  color: #c0c4cc;
  margin-bottom: 8px;
}
.upload-text {
  font-size: 14px;
  color: #606266;
  margin: 0 0 4px;
}
.upload-hint {
  font-size: 12px;
  color: #909399;
  margin: 0;
}
.preview-area {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.preview-image {
  max-width: 300px;
  max-height: 200px;
  border-radius: 6px;
  object-fit: contain;
  border: 1px solid #eee;
}
.preview-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
</style>
