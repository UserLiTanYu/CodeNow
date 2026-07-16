<template>
  <div class="comment-tree">
    <div v-for="comment in comments" :key="comment.id" class="comment-item">
      <div class="comment-header">
        <span class="comment-nickname">{{ comment.nickname }}</span>
        <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
      </div>
      <div class="comment-content">{{ comment.content }}</div>
      <div class="comment-actions">
        <button class="action-button" :class="{ active: comment.liked }" @click="toggleLike(comment)">{{ comment.liked ? '已赞' : '点赞' }} {{ comment.likeCount || '' }}</button>
        <button class="action-button" @click="handleReply(comment)">回复</button>
        <button v-if="comment.ownedByCurrentUser" class="action-button danger" @click="removeComment(comment)">删除</button>
      </div>

      <!-- 回复表单（当前评论下方） -->
      <CommentForm
        v-if="replyTo === comment.id"
        :article-id="articleId"
        :parent-id="comment.id"
        class="reply-form"
        @success="handleReplySuccess"
        @cancel="replyTo = 0"
      />

      <!-- 递归渲染子评论 -->
      <div v-if="comment.children && comment.children.length > 0" class="comment-children">
        <CommentTree
          :comments="comment.children"
          :article-id="articleId"
          @reply="(id) => $emit('reply', id)"
          @refresh="$emit('refresh')"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommentForm from './CommentForm.vue'
import { likeComment, unlikeComment } from '@/api/comment'
import { deleteMyComment } from '@/api/member'
import { useUserStore } from '@/stores/user'

defineProps({
  comments: { type: Array, required: true },
  articleId: { type: Number, required: true },
})

const emit = defineEmits(['reply', 'refresh'])
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const replyTo = ref(0)

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

function handleReply(comment) {
  if (!userStore.isLoggedIn) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  replyTo.value = replyTo.value === comment.id ? 0 : comment.id
}

async function toggleLike(comment) {
  if (!userStore.isLoggedIn) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  if (comment.liked) { await unlikeComment(comment.id); comment.liked = false; comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1) }
  else { await likeComment(comment.id); comment.liked = true; comment.likeCount = (comment.likeCount || 0) + 1 }
}

async function removeComment(comment) {
  await ElMessageBox.confirm('删除后会保留回复关系，确定继续吗？', '删除评论', { type: 'warning' })
  await deleteMyComment(comment.id)
  ElMessage.success('评论已删除')
  emit('refresh')
}

function handleReplySuccess() {
  replyTo.value = 0
  emit('refresh')
}
</script>

<style scoped>
.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--blog-color-border);
}
.comment-item:last-child {
  border-bottom: none;
}
.comment-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.comment-nickname {
  font-size: 15px;
  font-weight: 600;
  color: var(--blog-color-text);
}
.comment-time {
  font-size: 12px;
  color: var(--blog-color-text-muted);
}
.comment-content {
  font-size: 14px;
  color: var(--blog-color-text-secondary);
  line-height: 1.7;
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}
.comment-actions {
  margin-bottom: 8px;
}
.action-button {
  background: none;
  border: none;
  color: var(--blog-color-primary);
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
.action-button:hover, .action-button.active {
  color: var(--blog-color-primary-strong);
}
.action-button + .action-button { margin-left: 14px; }
.action-button.danger { color: #f56c6c; }
.comment-children {
  margin-left: 32px;
  padding-left: 16px;
  border-left: 2px solid var(--blog-color-border);
}
.reply-form {
  margin: 12px 0;
}
</style>
