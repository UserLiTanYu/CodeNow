<template>
  <div class="comment-tree">
    <article v-for="comment in comments" :key="comment.id" class="comment-item">
      <div class="comment-row">
        <img class="comment-avatar" :src="avatarUrl(comment.avatar)" alt="用户头像" @error="useDefaultAvatar" />

        <div class="comment-main">
          <div class="comment-header">
            <strong class="comment-nickname">{{ comment.nickname }}</strong>
            <span v-if="comment.userRole === 'ADMIN'" class="admin-badge">管理员</span>
            <time class="comment-time" :datetime="comment.createTime">{{ formatDate(comment.createTime) }}</time>
          </div>
          <div class="comment-content">{{ comment.content }}</div>
          <div class="comment-actions">
            <button type="button" class="action-button" :class="{ active: comment.liked }" @click="toggleLike(comment)">
              <span class="action-icon">{{ comment.liked ? '♥' : '♡' }}</span>
              {{ comment.likeCount || '点赞' }}
            </button>
            <button type="button" class="action-button" @click="handleReply(comment)">回复</button>
            <button v-if="comment.ownedByCurrentUser" type="button" class="action-button danger" @click="removeComment(comment)">删除</button>
          </div>

          <CommentForm
            v-if="replyTo === comment.id"
            :article-id="articleId"
            :parent-id="comment.id"
            class="reply-form"
            @success="handleReplySuccess"
            @cancel="replyTo = 0"
          />

          <template v-if="repliesFor(comment.id).length">
            <button
              v-if="!expandedRoots.has(comment.id)"
              type="button"
              class="reply-summary"
              @click="toggleReplies(comment.id)"
            >
              <span v-for="item in repliesFor(comment.id).slice(0, 2)" :key="item.comment.id" class="reply-preview">
                <strong>{{ item.comment.nickname }}</strong>
                <span>{{ item.comment.content }}</span>
              </span>
              <span class="reply-count">共 {{ repliesFor(comment.id).length }} 条回复，点击展开</span>
            </button>

            <section v-else class="reply-panel">
              <div class="reply-panel-heading">
                <span>相关回复 {{ repliesFor(comment.id).length }} 条</span>
                <button type="button" @click="toggleReplies(comment.id)">收起</button>
              </div>

              <article v-for="item in repliesFor(comment.id)" :key="item.comment.id" class="reply-item">
                <img class="comment-avatar reply-avatar" :src="avatarUrl(item.comment.avatar)" alt="用户头像" @error="useDefaultAvatar" />
                <div class="comment-main">
                  <div class="comment-header">
                    <strong class="comment-nickname">{{ item.comment.nickname }}</strong>
                    <span v-if="item.comment.userRole === 'ADMIN'" class="admin-badge">管理员</span>
                    <span v-if="item.replyToNickname" class="reply-target">回复 @{{ item.replyToNickname }}</span>
                    <time class="comment-time" :datetime="item.comment.createTime">{{ formatDate(item.comment.createTime) }}</time>
                  </div>
                  <div class="comment-content">{{ item.comment.content }}</div>
                  <div class="comment-actions">
                    <button type="button" class="action-button" :class="{ active: item.comment.liked }" @click="toggleLike(item.comment)">
                      <span class="action-icon">{{ item.comment.liked ? '♥' : '♡' }}</span>
                      {{ item.comment.likeCount || '点赞' }}
                    </button>
                    <button type="button" class="action-button" @click="handleReply(item.comment)">回复</button>
                    <button v-if="item.comment.ownedByCurrentUser" type="button" class="action-button danger" @click="removeComment(item.comment)">删除</button>
                  </div>
                  <CommentForm
                    v-if="replyTo === item.comment.id"
                    :article-id="articleId"
                    :parent-id="item.comment.id"
                    class="reply-form"
                    @success="handleReplySuccess"
                    @cancel="replyTo = 0"
                  />
                </div>
              </article>
            </section>
          </template>
        </div>
      </div>
    </article>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommentForm from './CommentForm.vue'
import { likeComment, unlikeComment } from '@/api/comment'
import { deleteMyComment } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'

const props = defineProps({
  comments: { type: Array, required: true },
  articleId: { type: Number, required: true },
})

const emit = defineEmits(['refresh'])
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const replyTo = ref(0)
const expandedRoots = ref(new Set())

const replyGroups = computed(() => {
  const groups = new Map()
  for (const root of props.comments) {
    const replies = []
    collectReplies(root.children || [], root.nickname, replies)
    groups.set(root.id, replies)
  }
  return groups
})

function collectReplies(children, parentNickname, target) {
  for (const comment of children) {
    target.push({ comment, replyToNickname: parentNickname })
    collectReplies(comment.children || [], comment.nickname, target)
  }
}

function repliesFor(rootId) {
  return replyGroups.value.get(rootId) || []
}

function toggleReplies(rootId) {
  const next = new Set(expandedRoots.value)
  if (next.has(rootId)) next.delete(rootId)
  else next.add(rootId)
  expandedRoots.value = next
}

function handleReply(comment) {
  if (!userStore.isLoggedIn) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  replyTo.value = replyTo.value === comment.id ? 0 : comment.id
}

async function toggleLike(comment) {
  if (!userStore.isLoggedIn) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  if (comment.liked) {
    await unlikeComment(comment.id)
    comment.liked = false
    comment.likeCount = Math.max(0, (comment.likeCount || 0) - 1)
  } else {
    await likeComment(comment.id)
    comment.liked = true
    comment.likeCount = (comment.likeCount || 0) + 1
  }
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

<style>
.comment-tree .comment-item {
  padding: 24px 0;
  border-bottom: 1px solid var(--blog-color-border);
}
.comment-tree .comment-item:first-child { padding-top: 2px; }
.comment-tree .comment-item:last-child { border-bottom: 0; }
.comment-tree .comment-row,
.comment-tree .reply-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.comment-tree .comment-avatar {
  width: 40px;
  height: 40px;
  flex: 0 0 40px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 0 0 1px var(--blog-color-border);
}
.comment-tree .comment-main { min-width: 0; flex: 1; }
.comment-tree .comment-header {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-height: 22px;
}
.comment-tree .comment-nickname { color: var(--blog-color-text); font-size: 15px; font-weight: 600; }
.comment-tree .comment-time { color: var(--blog-color-text-muted); font-size: 12px; }
.comment-tree .admin-badge {
  padding: 1px 6px;
  border-radius: 4px;
  color: #fff;
  background: var(--blog-color-primary);
  font-size: 10px;
  line-height: 16px;
}
.comment-tree .reply-target { color: var(--blog-color-primary); font-size: 12px; }
.comment-tree .comment-content {
  margin: 8px 0 10px;
  color: var(--blog-color-text);
  font-size: 15px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
.comment-tree .comment-actions { display: flex; align-items: center; gap: 18px; min-height: 24px; }
.comment-tree .action-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: 0;
  color: var(--blog-color-text-muted);
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  line-height: 24px;
  cursor: pointer;
  transition: color 0.16s ease;
}
.comment-tree .action-icon { font-size: 17px; line-height: 1; }
.comment-tree .action-button:hover,
.comment-tree .action-button.active { color: var(--blog-color-primary); }
.comment-tree .action-button.danger:hover { color: #f56c6c; }
.comment-tree .reply-form { margin-top: 14px; }
.comment-tree .reply-summary {
  width: 100%;
  margin-top: 16px;
  padding: 13px 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  box-sizing: border-box;
  border: 1px solid #edf0f4;
  border-radius: 10px;
  color: var(--blog-color-text-secondary);
  background: #f7f8fa;
  text-align: left;
  font-family: inherit;
  cursor: pointer;
  transition: border-color 0.16s ease, background-color 0.16s ease;
}
.comment-tree .reply-summary:hover { border-color: var(--blog-color-border-hover); background: #f4f8fc; }
.comment-tree .reply-preview {
  display: flex;
  gap: 7px;
  min-width: 0;
  font-size: 13px;
  line-height: 1.55;
}
.comment-tree .reply-preview strong { flex: 0 0 auto; color: var(--blog-color-text); font-weight: 600; }
.comment-tree .reply-preview span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.comment-tree .reply-count { color: var(--blog-color-primary); font-size: 13px; }
.comment-tree .reply-panel {
  margin-top: 16px;
  padding: 0 16px;
  border: 1px solid #edf0f4;
  border-radius: 10px;
  background: #f7f8fa;
}
.comment-tree .reply-panel-heading {
  padding: 13px 0;
  display: flex;
  justify-content: space-between;
  border-bottom: 1px solid var(--blog-color-border);
  color: var(--blog-color-text-secondary);
  font-size: 13px;
}
.comment-tree .reply-panel-heading button {
  padding: 0;
  border: 0;
  color: var(--blog-color-primary);
  background: transparent;
  font-family: inherit;
  cursor: pointer;
}
.comment-tree .reply-item { padding: 16px 0; border-bottom: 1px solid var(--blog-color-border); }
.comment-tree .reply-item:last-child { border-bottom: 0; }
.comment-tree .reply-avatar { width: 32px; height: 32px; flex-basis: 32px; }
@media (max-width: 640px) {
  .comment-tree .comment-avatar { width: 34px; height: 34px; flex-basis: 34px; }
  .comment-tree .reply-avatar { width: 28px; height: 28px; flex-basis: 28px; }
  .comment-tree .comment-row, .comment-tree .reply-item { gap: 9px; }
  .comment-tree .reply-panel { padding: 0 12px; }
}
</style>
