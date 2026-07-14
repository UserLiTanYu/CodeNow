<template>
  <div class="image-upload">
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
    <div v-else class="preview-area">
      <img :src="imageUrl" class="preview-image" />
      <div class="preview-actions">
        <el-button size="small" @click="triggerInput">更换</el-button>
        <el-button size="small" type="danger" @click="handleRemove">删除</el-button>
      </div>
    </div>
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
import { ref, watch } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadImage } from '@/api/upload'

const props = defineProps({
  modelValue: { type: String, default: '' },
})

const emit = defineEmits(['update:modelValue'])

const inputRef = ref(null)
const imageUrl = ref(props.modelValue)
const isDragover = ref(false)
const uploading = ref(false)

watch(
  () => props.modelValue,
  (val) => {
    imageUrl.value = val
  },
)

function triggerInput() {
  inputRef.value.click()
}

function handleDrop(e) {
  isDragover.value = false
  const file = e.dataTransfer.files[0]
  if (file) uploadFile(file)
}

function handleFileChange(e) {
  const file = e.target.files[0]
  if (file) uploadFile(file)
  e.target.value = ''
}

async function uploadFile(file) {
  if (uploading.value) return
  uploading.value = true

  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await uploadImage(formData)
    imageUrl.value = res.data.url
    emit('update:modelValue', res.data.url)
    ElMessage.success('上传成功')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    uploading.value = false
  }
}

function handleRemove() {
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
