<template>
  <!-- 管理后台主布局组件：左侧边栏导航 + 右侧内容区域 -->
  <el-container class="layout-container">
    <!-- 左侧边栏：Logo 和管理功能菜单 -->
    <el-aside width="200px" class="aside">
      <div class="logo">码上记</div>
      <!-- 管理功能导航菜单 -->
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
        <el-menu-item index="/author-applications">
          <el-icon><UserFilled /></el-icon>
          <span>作者申请</span>
        </el-menu-item>
        <el-menu-item index="/login-logs">
          <el-icon><Key /></el-icon>
          <span>登录日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <!-- 右侧内容区域 -->
    <el-container>
      <!-- 顶部头部栏：页面标题和操作按钮 -->
      <el-header class="header">
        <!-- 当前页面标题（根据路由路径动态显示） -->
        <span class="title">{{ routeTitle }}</span>
        <!-- 头部操作按钮 -->
        <div class="header-actions">
          <!-- 访问前台博客按钮 -->
          <el-button type="primary" plain @click="router.push('/blog')">
            <el-icon><House /></el-icon>
            访问前台
          </el-button>
          <!-- 管理员下拉菜单 -->
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
        </div>
      </el-header>
      <!-- 主内容区域：渲染子路由 -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
/**
 * 管理后台主布局组件
 * 提供管理后台的整体布局框架，包含左侧导航侧边栏和右侧内容区域。
 * 侧边栏包含文章、分类、标签、评论、日志、用户、作者申请等管理功能入口。
 * 顶部头部栏显示当前页面标题，支持访问前台和退出登录操作。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Document, Folder, PriceTag, ArrowDown, Notebook, ChatDotRound, User, UserFilled, Key, House } from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 根据当前路由路径动态计算页面标题 */
const routeTitle = computed(() => {
  const map = {
    '/articles': '文章管理',
    '/categories': '分类管理',
    '/tags': '标签管理',
    '/comments': '评论管理',
    '/logs': '操作日志',
    '/users': '用户管理',
    '/author-applications': '作者申请审批',
    '/login-logs': '登录日志',
  }
  return map[route.path] || '文章管理'
})

/** 页面加载时获取用户信息 */
userStore.fetchUserInfo().catch(() => {})

/** 处理管理员下拉菜单命令：退出登录时弹出确认对话框 */
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
.header-actions {
  display: flex;
  align-items: center;
  gap: 18px;
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
