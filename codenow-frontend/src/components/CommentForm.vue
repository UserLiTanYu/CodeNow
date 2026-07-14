<template>
  <div class="comment-form">
    <h4 class="form-title">{{ parentId ? '回复评论' : '发表评论' }}</h4>
    <div class="form-row">
      <input
        v-model="form.nickname"
        class="form-input"
        placeholder="昵称（必填）"
        maxlength="50"
      />
      <input
        v-model="form.email"
        class="form-input"
        placeholder="邮箱（选填，不会公开）"
        maxlength="100"
      />
    </div>
    <textarea
      v-model="form.content"
      class="form-textarea"
      placeholder="写下你的评论..."
      rows="4"
      maxlength="1000"
    ></textarea>
    <div class="form-actions">
      <button class="btn-submit" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? '提交中...' : '提交评论' }}
      </button>
      <button v-if="parentId" class="btn-cancel" @click="$emit('cancel')">取消回复</button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import request from '@/utils/request'
import { ElMessage } from 'element-plus'

const props = defineProps({
  articleId: { type: Number, required: true },
  parentId: { type: Number, default: 0 },
})

const emit = defineEmits(['success', 'cancel'])

const submitting = ref(false)
const form = reactive({
  nickname: '',
  email: '',
  content: '',
})

async function handleSubmit() {
  if (!form.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return
  }
  if (!form.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submitting.value = true
  try {
    await request.post('/comments', {
      articleId: props.articleId,
      parentId: props.parentId || 0,
      content: form.content.trim(),
      nickname: form.nickname.trim(),
      email: form.email.trim() || null,
    })
    ElMessage.success('评论成功')
    form.content = ''
    emit('success')
  } catch {
    // 错误已在拦截器中处理
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.comment-form {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
}
.form-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #303133;
}
.form-row {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}
.form-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
}
.form-input:focus {
  border-color: #409eff;
}
.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  font-size: 14px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.form-textarea:focus {
  border-color: #409eff;
}
.form-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}
.btn-submit {
  padding: 8px 24px;
  background: #409eff;
  color: #fff;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-submit:hover {
  background: #66b1ff;
}
.btn-submit:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}
.btn-cancel {
  padding: 8px 16px;
  background: #f0f2f5;
  color: #606266;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
}
</style>
