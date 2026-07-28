<template>
  <!-- 评论树组件：展示文章的评论列表，支持嵌套回复、点赞和删除 -->
  <div class="comment-tree">
    <!-- 遍历顶层评论列表 -->
    <article v-for="comment in comments" :key="comment.id" class="comment-item">
      <div class="comment-row">
        <img class="comment-avatar" :src="avatarUrl(comment.avatar)" alt="用户头像" @error="useDefaultAvatar" />

        <div class="comment-main">
          <!-- 评论头部：昵称、角色标签、时间 -->
          <div class="comment-header">
            <strong class="comment-nickname">{{ comment.nickname }}</strong>
            <span v-if="comment.userRole === 'ADMIN'" class="admin-badge">管理员</span>
            <span v-if="comment.userRole === 'AUTHOR'" class="author-badge">作者</span>
            <time class="comment-time" :datetime="comment.createTime">{{ formatDate(comment.createTime) }}</time>
          </div>
          <!-- 评论内容 -->
          <div class="comment-content">{{ comment.content }}</div>
          <!-- 评论操作栏：点赞、回复、删除 -->
          <div class="comment-actions">
            <button type="button" class="action-button" :class="{ active: comment.liked }" @click="toggleLike(comment)">
              <span class="action-icon">{{ comment.liked ? '♥' : '♡' }}</span>
              {{ comment.likeCount || '点赞' }}
            </button>
            <button type="button" class="action-button" @click="handleReply(comment)">回复</button>
            <button v-if="comment.ownedByCurrentUser" type="button" class="action-button danger" @click="removeComment(comment)">删除</button>
          </div>

          <!-- 回复输入表单：点击回复按钮后展开 -->
          <CommentForm
            v-if="replyTo === comment.id"
            :article-id="articleId"
            :parent-id="comment.id"
            class="reply-form"
            @success="handleReplySuccess"
            @cancel="replyTo = 0"
          />

          <!-- 子回复区域：折叠时显示预览摘要，展开时显示完整回复列表 -->
          <template v-if="repliesFor(comment.id).length">
            <!-- 折叠状态：显示前两条回复预览和总数 -->
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

            <!-- 展开状态：显示完整回复列表 -->
            <section v-else class="reply-panel">
              <div class="reply-panel-heading">
                <span>相关回复 {{ repliesFor(comment.id).length }} 条</span>
                <button type="button" @click="toggleReplies(comment.id)">收起</button>
              </div>

              <!-- 遍历子回复 -->
              <article v-for="item in repliesFor(comment.id)" :key="item.comment.id" class="reply-item">
                <img class="comment-avatar reply-avatar" :src="avatarUrl(item.comment.avatar)" alt="用户头像" @error="useDefaultAvatar" />
                <div class="comment-main">
                  <!-- 子回复头部：昵称、角色标签、回复对象、时间 -->
                  <div class="comment-header">
                    <strong class="comment-nickname">{{ item.comment.nickname }}</strong>
                    <span v-if="item.comment.userRole === 'ADMIN'" class="admin-badge">管理员</span>
                    <span v-if="item.comment.userRole === 'AUTHOR'" class="author-badge">作者</span>
                    <span v-if="item.replyToNickname" class="reply-target">回复 @{{ item.replyToNickname }}</span>
                    <time class="comment-time" :datetime="item.comment.createTime">{{ formatDate(item.comment.createTime) }}</time>
                  </div>
                  <!-- 子回复内容 -->
                  <div class="comment-content">{{ item.comment.content }}</div>
                  <!-- 子回复操作栏 -->
                  <div class="comment-actions">
                    <button type="button" class="action-button" :class="{ active: item.comment.liked }" @click="toggleLike(item.comment)">
                      <span class="action-icon">{{ item.comment.liked ? '♥' : '♡' }}</span>
                      {{ item.comment.likeCount || '点赞' }}
                    </button>
                    <button type="button" class="action-button" @click="handleReply(item.comment)">回复</button>
                    <button v-if="item.comment.ownedByCurrentUser" type="button" class="action-button danger" @click="removeComment(item.comment)">删除</button>
                  </div>
                  <!-- 子回复的回复表单 -->
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
/**
 * 评论树组件
 * 以树形结构展示文章评论，支持嵌套回复、点赞/取消点赞、删除自己的评论。
 * 后端返回的嵌套树会被展平为分组的回复列表，方便前端渲染折叠/展开交互。
 */
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import CommentForm from './CommentForm.vue'
import { likeComment, unlikeComment } from '@/api/comment'
import { deleteMyComment } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'

/** 组件属性：comments 评论列表（含嵌套子评论），articleId 文章ID */
const props = defineProps({
  comments: { type: Array, required: true },
  articleId: { type: Number, required: true },
})

/** 事件：refresh 刷新评论列表 */
const emit = defineEmits(['refresh'])
const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 当前正在回复的评论ID，0表示未在回复 */
const replyTo = ref(0)
/** 已展开回复的根评论ID集合 */
const expandedRoots = ref(new Set())

/**
 * 将后端返回的嵌套评论树按根评论分组展平
 * 后端返回无环嵌套树；界面按 DFS 将每个根节点的后代展平，并保留直接父评论昵称用于”回复 @谁”。
 */
const replyGroups = computed(() => {
  const groups = new Map()
  for (const root of props.comments) {
    const replies = []
    collectReplies(root.children || [], root.nickname, replies)
    groups.set(root.id, replies)
  }
  return groups
})

/** 递归收集子评论，保留父评论昵称用于显示”回复 @谁” */
function collectReplies(children, parentNickname, target) {
  for (const comment of children) {
    target.push({ comment, replyToNickname: parentNickname })
    collectReplies(comment.children || [], comment.nickname, target)
  }
}

/** 获取指定根评论下的所有展平后的回复列表 */
function repliesFor(rootId) {
  return replyGroups.value.get(rootId) || []
}

/** 切换指定根评论的回复展开/折叠状态 */
function toggleReplies(rootId) {
  const next = new Set(expandedRoots.value)
  if (next.has(rootId)) next.delete(rootId)
  else next.add(rootId)
  expandedRoots.value = next
}

/** 点击回复按钮：未登录则跳转登录页，已登录则切换回复输入框的显示 */
function handleReply(comment) {
  if (!userStore.isLoggedIn) return router.push({ path: '/login', query: { redirect: route.fullPath } })
  replyTo.value = replyTo.value === comment.id ? 0 : comment.id
}

/** 点赞/取消点赞：未登录则跳转登录页，已登录则切换点赞状态并更新计数 */
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

/** 删除自己的评论：二次确认后调用接口删除，成功后通知父组件刷新列表 */
async function removeComment(comment) {
  await ElMessageBox.confirm('删除后会保留回复关系，确定继续吗？', '删除评论', { type: 'warning' })
  await deleteMyComment(comment.id)
  ElMessage.success('评论已删除')
  emit('refresh')
}

/** 回复提交成功回调：关闭回复输入框并刷新评论列表 */
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
.comment-tree .author-badge {
  padding: 1px 6px;
  border-radius: 4px;
  color: #fff;
  background: #52c41a;
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
