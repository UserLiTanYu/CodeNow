<template>
  <div class="comment-form">
    <div v-if="!userStore.isLoggedIn" class="login-required">
      <span>登录后即可参与评论</span>
      <button type="button" @click="goLogin">去登录</button>
    </div>
    <div v-else class="input-bar">
      <textarea
        v-model="form.content"
        class="form-textarea"
        :placeholder="parentId ? '输入回复...' : '输入评论'"
        rows="1"
        maxlength="1000"
        @keydown.ctrl.enter.prevent="handleSubmit"
      ></textarea>
      <div class="form-actions">
        <button v-if="parentId" type="button" class="btn-cancel" @click="$emit('cancel')">取消</button>
        <button type="button" class="btn-submit" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '提交中' : '发布' }}
        </button>
      </div>
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
  width: 100%;
}
.login-required { min-height: 48px; padding: 0 14px; display: flex; align-items: center; justify-content: space-between; box-sizing: border-box; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-button); color: var(--blog-color-text-secondary); background: var(--blog-color-surface); }
.login-required button { padding: 7px 14px; border: 0; border-radius: var(--blog-radius-button); color: #fff; background: var(--blog-color-primary); cursor: pointer; }
.input-bar {
  min-height: 48px;
  display: flex;
  align-items: center;
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-button);
  background: var(--blog-color-surface);
  transition: border-color 0.2s, box-shadow 0.2s;
}
.input-bar:focus-within {
  border-color: var(--blog-color-primary);
  box-shadow: var(--blog-focus-ring);
}
.form-textarea {
  flex: 1;
  min-width: 0;
  min-height: 24px;
  max-height: 96px;
  padding: 12px 14px;
  border: 0;
  font-size: 14px;
  line-height: 24px;
  resize: none;
  overflow-y: auto;
  outline: none;
  font-family: inherit;
  box-sizing: border-box;
}
.form-actions {
  padding-right: 8px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.btn-submit {
  padding: 7px 14px;
  background: var(--blog-color-primary-soft);
  color: var(--blog-color-primary);
  border: none;
  border-radius: var(--blog-radius-button);
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.2s;
}
.btn-submit:hover {
  color: #fff;
  background: var(--blog-color-primary);
}
.btn-submit:disabled {
  color: var(--blog-color-text-muted);
  background: var(--blog-color-background);
  cursor: not-allowed;
}
.btn-cancel {
  padding: 7px 4px;
  background: transparent;
  color: var(--blog-color-text-secondary);
  border: none;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
}
</style>
