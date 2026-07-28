<template>
  <!-- 评论表单组件：未登录时显示登录提示，已登录时显示输入框和提交按钮 -->
  <div class="comment-form">
    <!-- 未登录状态：提示用户登录 -->
    <div v-if="!userStore.isLoggedIn" class="login-required">
      <span>登录后即可参与评论</span>
      <button type="button" @click="goLogin">去登录</button>
    </div>
    <!-- 已登录状态：评论输入区域 -->
    <div v-else class="input-bar">
      <textarea
        v-model="form.content"
        class="form-textarea"
        :placeholder="parentId ? '输入回复...' : '输入评论'"
        rows="1"
        maxlength="1000"
        @keydown.ctrl.enter.prevent="handleSubmit"
      ></textarea>
      <!-- 操作按钮区域：回复模式下显示取消按钮 -->
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
/**
 * 评论表单组件
 * 支持发表评论和回复评论两种模式，通过 parentId 区分。
 * 未登录用户会看到登录提示，已登录用户可直接输入评论内容。
 */
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createComment } from '@/api/comment'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

/** 组件属性：articleId 文章ID，parentId 父评论ID（0表示顶级评论） */
const props = defineProps({
  articleId: { type: Number, required: true },
  parentId: { type: Number, default: 0 },
})

/** 事件：success 评论提交成功，cancel 取消回复 */
const emit = defineEmits(['success', 'cancel'])
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 是否正在提交中 */
const submitting = ref(false)
/** 评论表单数据 */
const form = reactive({
  content: '',
})

/** 跳转到登录页面，登录成功后自动返回当前页面 */
function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}

/** 提交评论：校验内容非空后调用接口创建评论，成功后清空输入框并通知父组件刷新 */
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
