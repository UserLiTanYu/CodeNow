<template>
  <el-container class="author-layout">
    <el-aside width="224px" class="author-sidebar">
      <router-link to="/blog" class="brand">码上记</router-link>
      <div class="console-label">作者工作台</div>
      <el-menu :default-active="route.path" router class="author-menu">
        <el-menu-item index="/author-console/articles">
          <el-icon><Document /></el-icon>
          <span>我的文章</span>
        </el-menu-item>
        <el-menu-item index="/author-console/comments">
          <el-icon><ChatDotRound /></el-icon>
          <span>文章评论</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container class="author-shell">
      <el-header class="author-header">
        <div>
          <div class="eyebrow">AUTHOR CONSOLE</div>
          <h1>{{ route.meta.title || '我的文章' }}</h1>
        </div>
        <div class="header-actions">
          <el-button plain @click="router.push('/blog')">
            <el-icon><House /></el-icon>访问博客
          </el-button>
          <el-dropdown @command="handleCommand">
            <button type="button" class="user-menu">
              {{ userStore.userInfo?.nickname || userStore.userInfo?.username || '作者' }}
              <el-icon><ArrowDown /></el-icon>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="author-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import { ArrowDown, ChatDotRound, Document, House } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

if (!userStore.userInfo) userStore.fetchUserInfo().catch(() => {})

async function handleCommand(command) {
  if (command === 'profile') return router.push('/blog/profile')
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      await userStore.logout()
      router.push('/login')
    } catch {
      // 用户取消退出。
    }
  }
}
</script>

<style scoped>
.author-layout { height: 100vh; overflow: hidden; background: #f3f6fa; }
.author-sidebar { height: 100vh; overflow: hidden; color: #dce6f2; background: #172235; border-right: 1px solid #24344d; }
.brand { height: 66px; padding: 0 24px; display: flex; align-items: center; color: #fff; background: #111a2a; font-size: 22px; font-weight: 750; text-decoration: none; }
.console-label { padding: 24px 24px 10px; color: #8494aa; font-size: 11px; font-weight: 700; letter-spacing: 1.6px; }
.author-menu { border-right: 0; background: transparent; }
.author-menu :deep(.el-menu-item) { margin: 4px 12px; border-radius: 8px; color: #bfccdc; }
.author-menu :deep(.el-menu-item:hover), .author-menu :deep(.el-menu-item.is-active) { color: #fff; background: #2b66b1; }
.author-shell { min-width: 0; height: 100vh; overflow: hidden; }
.author-header { height: 78px; padding: 0 28px; flex: 0 0 78px; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #e5eaf0; background: #fff; }
.author-header h1 { margin: 2px 0 0; color: #1f2a3a; font-size: 20px; }
.eyebrow { color: #8794a5; font-size: 10px; font-weight: 700; letter-spacing: 1.4px; }
.header-actions { display: flex; align-items: center; gap: 16px; }
.user-menu { display: flex; align-items: center; gap: 5px; border: 0; color: #344054; background: transparent; cursor: pointer; font: inherit; }
.author-main { min-height: 0; overflow: auto; padding: 24px 28px; }
@media (max-width: 720px) {
  .author-sidebar { width: 72px !important; }
  .brand { padding: 0 12px; font-size: 16px; }
  .console-label, .author-menu span, .eyebrow, .header-actions > .el-button { display: none; }
  .author-header { padding: 0 12px; }
  .author-header h1 { font-size: 18px; }
  .header-actions { gap: 6px; min-width: 0; }
  .user-menu { max-width: 120px; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }
  .author-main { padding: 16px; }
}
</style>
