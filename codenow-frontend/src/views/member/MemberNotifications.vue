<template>
  <section class="member-card">
    <div class="heading"><div><h1>消息中心</h1><p>评论回复等站内通知</p></div><el-button :disabled="unreadCount === 0" @click="readAll">全部已读</el-button></div>
    <div v-loading="loading" class="notification-list">
      <button v-for="item in notifications" :key="item.id" :class="['notification-item', { unread: !item.isRead }]" @click="open(item)">
        <span class="dot"></span><span class="content"><strong>{{ item.title }}</strong><span>{{ item.content }}</span><small>{{ formatTime(item.createTime) }}</small></span>
      </button>
      <el-empty v-if="!loading && notifications.length === 0" description="暂无消息" />
    </div>
    <el-pagination v-if="total > pageSize" v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" class="pagination" @current-change="load" />
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getNotifications, markAllNotificationsRead, markNotificationRead } from '@/api/member'
const router = useRouter(); const notifications = ref([]); const loading = ref(false); const pageNum = ref(1); const pageSize = 10; const total = ref(0); const unreadCount = ref(0)
async function load() { loading.value = true; try { const res = await getNotifications({ pageNum: pageNum.value, pageSize }); notifications.value = res.data.records; total.value = res.data.total; unreadCount.value = notifications.value.filter(item => !item.isRead).length } finally { loading.value = false } }
async function open(item) { if (!item.isRead) { await markNotificationRead(item.id); item.isRead = 1; unreadCount.value = Math.max(0, unreadCount.value - 1) } if (item.articleId) router.push(`/blog/article/${item.articleId}`) }
async function readAll() { await markAllNotificationsRead(); notifications.value.forEach(item => { item.isRead = 1 }); unreadCount.value = 0 }
function formatTime(value) { return value ? value.replace('T', ' ').slice(0, 16) : '' }
onMounted(load)
</script>

<style scoped>
.member-card { padding: 28px; border: 1px solid var(--blog-color-border); border-radius: var(--blog-radius-card); background: #fff; } .heading { margin-bottom: 20px; display: flex; justify-content: space-between; align-items: center; } h1 { margin: 0 0 6px; font-size: 24px; } p { margin: 0; color: var(--blog-color-text-muted); }
.notification-list { min-height: 180px; } .notification-item { width: 100%; padding: 16px 12px; display: flex; gap: 12px; border: 0; border-bottom: 1px solid var(--blog-color-border); background: #fff; text-align: left; cursor: pointer; } .notification-item:hover { background: var(--blog-color-primary-soft); } .dot { width: 8px; height: 8px; margin-top: 7px; border-radius: 50%; background: transparent; } .unread .dot { background: var(--blog-color-primary); } .content { display: flex; flex-direction: column; gap: 5px; color: var(--blog-color-text-secondary); } .content strong { color: var(--blog-color-text); } .content small { color: var(--blog-color-text-muted); } .pagination { margin-top: 20px; justify-content: center; }
</style>
