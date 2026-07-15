<template>
  <div class="comment-tree">
    <div v-for="comment in comments" :key="comment.id" class="comment-item">
      <div class="comment-header">
        <span class="comment-nickname">{{ comment.nickname }}</span>
        <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
      </div>
      <div class="comment-content">{{ comment.content }}</div>
      <div class="comment-actions">
        <button class="btn-reply" @click="handleReply(comment)">回复</button>
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
import CommentForm from './CommentForm.vue'

defineProps({
  comments: { type: Array, required: true },
  articleId: { type: Number, required: true },
})

const emit = defineEmits(['reply', 'refresh'])

const replyTo = ref(0)

function formatTime(timeStr) {
  if (!timeStr) return ''
  return timeStr.replace('T', ' ').substring(0, 16)
}

function handleReply(comment) {
  replyTo.value = replyTo.value === comment.id ? 0 : comment.id
}

function handleReplySuccess() {
  replyTo.value = 0
  emit('refresh')
}
</script>

<style scoped>
.comment-item {
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
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
  color: #303133;
}
.comment-time {
  font-size: 12px;
  color: #909399;
}
.comment-content {
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  margin-bottom: 8px;
  white-space: pre-wrap;
  word-break: break-word;
}
.comment-actions {
  margin-bottom: 8px;
}
.btn-reply {
  background: none;
  border: none;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}
.btn-reply:hover {
  color: #66b1ff;
}
.comment-children {
  margin-left: 32px;
  padding-left: 16px;
  border-left: 2px solid #f0f2f5;
}
.reply-form {
  margin: 12px 0;
}
</style>
