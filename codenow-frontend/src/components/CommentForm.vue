<template>
  <div class="comment-form">
    <h4 class="form-title">{{ parentId ? '回复评论' : '发表评论' }}</h4>
    <div v-if="!userStore.isLoggedIn" class="login-required">
      <span>登录后即可参与评论</span>
      <button type="button" @click="goLogin">去登录</button>
    </div>
    <textarea
      v-else
      v-model="form.content"
      class="form-textarea"
      placeholder="写下你的评论..."
      rows="4"
      maxlength="1000"
    ></textarea>
    <div v-if="userStore.isLoggedIn" class="form-actions">
      <button class="btn-submit" :disabled="submitting" @click="handleSubmit">
        {{ submitting ? '提交中...' : '提交评论' }}
      </button>
      <button v-if="parentId" class="btn-cancel" @click="$emit('cancel')">取消回复</button>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createComment } from '@/api/comment'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const props = defineProps({
  articleId: { type: Number, required: true },
  parentId: { type: Number, default: 0 },
})

const emit = defineEmits(['success', 'cancel'])
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const submitting = ref(false)
const form = reactive({
  content: '',
})

function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

async function handleSubmit() {
  if (!form.content.trim()) {
    ElMessage.warning('请输入评论内容')
    return
  }

  submitting.value = true
  try {
    await createComment({
      articleId: props.articleId,
      parentId: props.parentId || 0,
      content: form.content.trim(),
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
  background: var(--blog-color-surface);
  border-radius: var(--blog-radius-card);
  padding: 20px;
}
.form-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: var(--blog-color-text);
}
.login-required { padding: 16px; display: flex; align-items: center; justify-content: space-between; border-radius: var(--blog-radius-button); color: var(--blog-color-text-secondary); background: var(--blog-color-background); }
.login-required button { padding: 7px 14px; border: 0; border-radius: var(--blog-radius-button); color: #fff; background: var(--blog-color-primary); cursor: pointer; }
.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-button);
  font-size: 14px;
  resize: vertical;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
  transition: border-color 0.2s;
}
.form-textarea:focus {
  border-color: var(--blog-color-primary);
}
.form-actions {
  margin-top: 12px;
  display: flex;
  gap: 10px;
}
.btn-submit {
  padding: 8px 24px;
  background: var(--blog-color-primary);
  color: #fff;
  border: none;
  border-radius: var(--blog-radius-button);
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-submit:hover {
  background: var(--blog-color-primary-strong);
}
.btn-submit:disabled {
  background: var(--blog-color-primary-disabled);
  cursor: not-allowed;
}
.btn-cancel {
  padding: 8px 16px;
  background: var(--blog-color-background);
  color: var(--blog-color-text-secondary);
  border: none;
  border-radius: var(--blog-radius-button);
  font-size: 14px;
  cursor: pointer;
}
</style>
