<template>
  <el-container class="layout-container">
    <el-aside width="200px" class="aside">
      <div class="logo">码上记</div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/articles">
          <el-icon><Document /></el-icon>
          <span>文章管理</span>
        </el-menu-item>
        <el-menu-item index="/categories">
          <el-icon><Folder /></el-icon>
          <span>分类管理</span>
        </el-menu-item>
        <el-menu-item index="/tags">
          <el-icon><PriceTag /></el-icon>
          <span>标签管理</span>
        </el-menu-item>
        <el-menu-item index="/comments">
          <el-icon><ChatDotRound /></el-icon>
          <span>评论管理</span>
        </el-menu-item>
        <el-menu-item index="/logs">
          <el-icon><Notebook /></el-icon>
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <span>用户管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="title">{{ routeTitle }}</span>
        <el-dropdown @command="handleCommand">
          <span class="user-info">
            {{ userStore.userInfo?.nickname || '管理员' }}
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Document, Folder, PriceTag, ArrowDown, Notebook, ChatDotRound, User } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const routeTitle = computed(() => {
  const map = {
    '/articles': '文章管理',
    '/categories': '分类管理',
    '/tags': '标签管理',
    '/comments': '评论管理',
    '/logs': '操作日志',
    '/users': '用户管理',
  }
  return map[route.path] || '文章管理'
})

// 获取用户信息
userStore.fetchUserInfo().catch(() => {})

function handleCommand(cmd) {
  if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
      .then(() => {
        userStore.logout()
        router.push('/login')
      })
      .catch(() => {})
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}
.aside {
  background-color: #304156;
  overflow: hidden;
}
.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  font-size: 20px;
  font-weight: bold;
  color: #fff;
  background-color: #263445;
}
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eee;
  background: #fff;
}
.title {
  font-size: 18px;
  font-weight: 600;
}
.user-info {
  display: flex;
  align-items: center;
  cursor: pointer;
  gap: 4px;
}
.main {
  background: #f5f7fa;
}
</style>
