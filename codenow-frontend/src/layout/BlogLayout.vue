<template>
  <div class="blog-layout">
    <header class="blog-header">
      <div class="header-inner">
        <router-link to="/blog" class="logo" aria-label="码上记博客首页">码上记</router-link>

        <nav class="nav-categories desktop-nav" aria-label="博客分类导航">
          <router-link to="/blog" class="nav-item">首页</router-link>
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="nav-category-group"
          >
            <router-link :to="`/blog/category/${cat.id}`" class="nav-item">{{ cat.name }}</router-link>
            <div v-if="cat.children?.length" class="nav-submenu">
              <router-link v-for="child in cat.children" :key="child.id" :to="`/blog/category/${child.id}`">
                {{ child.name }}
              </router-link>
            </div>
          </div>
        </nav>

        <div class="header-actions">
          <form class="desktop-search" role="search" @submit.prevent="submitSearch">
            <el-input
              v-model="searchKeyword"
              :prefix-icon="Search"
              placeholder="搜索文章、分类或标签"
              aria-label="搜索文章、分类或标签"
              maxlength="100"
              clearable
              @clear="clearSearch"
            />
          </form>

          <router-link v-if="!userStore.isLoggedIn" :to="loginTarget" class="login-link">
            <el-icon><User /></el-icon>
            <span>登录</span>
          </router-link>
          <router-link v-if="userStore.isAdmin" to="/" class="login-link admin-link">
            <el-icon><Setting /></el-icon>
            <span>前往后台</span>
          </router-link>
          <el-dropdown v-if="userStore.isLoggedIn" trigger="click" @command="handleUserCommand">
            <button type="button" class="login-link user-trigger">
              <img class="header-user-avatar" :src="avatarUrl(userStore.userInfo?.avatar)" alt="用户头像" @error="useDefaultAvatar" />
              <span>{{ userStore.userInfo?.nickname || userStore.userInfo?.username || '个人中心' }}</span>
            </button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                <el-dropdown-item command="comments">我的评论</el-dropdown-item>
                <el-dropdown-item command="favorites">我的收藏</el-dropdown-item>
                <el-dropdown-item command="notifications">消息中心<span v-if="unreadCount" class="unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span></el-dropdown-item>
                <el-dropdown-item v-if="!userStore.isAdmin" command="authorApplication">{{ userStore.isAuthor ? '作者身份' : '申请成为作者' }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <button
            type="button"
            class="header-icon-button mobile-search-trigger"
            :aria-expanded="mobileSearchOpen"
            aria-controls="mobile-search-panel"
            :aria-label="mobileSearchOpen ? '关闭搜索' : '打开搜索'"
            @click="toggleMobileSearch"
          >
            <el-icon><Close v-if="mobileSearchOpen" /><Search v-else /></el-icon>
          </button>

          <button
            type="button"
            class="header-icon-button menu-trigger"
            :aria-expanded="mobileMenuOpen"
            aria-controls="mobile-category-nav"
            :aria-label="mobileMenuOpen ? '关闭分类菜单' : '打开分类菜单'"
            @click="toggleMobileMenu"
          >
            <el-icon><Close v-if="mobileMenuOpen" /><Menu v-else /></el-icon>
          </button>
        </div>
      </div>

      <Transition name="header-panel">
        <div v-if="mobileSearchOpen" id="mobile-search-panel" class="mobile-search-panel">
          <form role="search" @submit.prevent="submitSearch">
            <el-input
              ref="mobileSearchInput"
              v-model="searchKeyword"
              :prefix-icon="Search"
              placeholder="搜索标题、摘要、分类或标签"
              aria-label="搜索标题、摘要、分类或标签"
              maxlength="100"
              clearable
              @clear="clearSearch"
              @keyup.esc="mobileSearchOpen = false"
            />
          </form>
        </div>
      </Transition>

      <Transition name="header-panel">
        <nav
          v-if="mobileMenuOpen"
          id="mobile-category-nav"
          class="mobile-nav"
          aria-label="移动端博客分类导航"
        >
          <router-link to="/blog" class="mobile-nav-item">首页</router-link>
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="mobile-category-group"
          >
            <router-link :to="`/blog/category/${cat.id}`" class="mobile-nav-item mobile-root-item">{{ cat.name }}</router-link>
            <router-link
              v-for="child in cat.children || []"
              :key="child.id"
              :to="`/blog/category/${child.id}`"
              class="mobile-nav-item mobile-child-item"
            >{{ child.name }}</router-link>
          </div>
        </nav>
      </Transition>
    </header>

    <div class="blog-body">
      <main class="blog-main">
        <router-view />
      </main>
      <aside class="blog-sidebar">
        <div class="sidebar-section">
          <h3 class="sidebar-title">热门文章</h3>
          <div v-if="hotArticles.length > 0" class="hot-list">
            <router-link
              v-for="(item, index) in hotArticles"
              :key="item.article.id"
              :to="`/blog/article/${item.article.id}`"
              class="hot-item"
            >
              <span class="hot-rank">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="hot-content">
                <span class="hot-title">{{ item.article.title }}</span>
                <span class="hot-views"><el-icon><View /></el-icon>{{ item.article.viewCount }} 阅读</span>
              </span>
            </router-link>
          </div>
          <p v-else class="empty-text">暂无热门文章</p>
        </div>
        <div class="sidebar-section">
          <h3 class="sidebar-title">标签</h3>
          <div class="tag-cloud">
            <router-link
              v-for="tag in tags"
              :key="tag.id"
              :to="`/blog/tag/${tag.id}`"
              :class="['tag-item', tagTone(tag.name)]"
            >
              {{ tag.name }}
            </router-link>
          </div>
        </div>
        <div class="sidebar-section">
          <h3 class="sidebar-title">关于</h3>
          <p class="about-text">一个支持 Markdown 写作的个人技术博客，帮助开发者记录和分享学习笔记。</p>
        </div>
      </aside>
    </div>

    <footer class="blog-footer">
      <p>&copy; {{ new Date().getFullYear() }} 码上记 CodeNow. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, Menu, Search, Setting, User, View } from '@element-plus/icons-vue'
import { getBlogCategories, getBlogTags, getHotArticles } from '@/api/blog'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'
import { getUnreadNotificationCount } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const categories = ref([])
const tags = ref([])
const hotArticles = ref([])
const searchKeyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const mobileMenuOpen = ref(false)
const mobileSearchOpen = ref(false)
const mobileSearchInput = ref()
const loginTarget = computed(() => ({ path: '/login', query: { redirect: route.fullPath } }))
const unreadCount = ref(0)

function normalizedKeyword() {
  return searchKeyword.value.trim().slice(0, 100)
}

function submitSearch() {
  const keyword = normalizedKeyword()
  searchKeyword.value = keyword
  mobileSearchOpen.value = false
  mobileMenuOpen.value = false
  router.push({ path: '/blog', query: keyword ? { keyword } : {} })
}

function clearSearch() {
  searchKeyword.value = ''
  if (route.query.keyword) {
    router.push({ path: '/blog' })
  }
}

async function toggleMobileSearch() {
  mobileSearchOpen.value = !mobileSearchOpen.value
  mobileMenuOpen.value = false
  if (mobileSearchOpen.value) {
    await nextTick()
    mobileSearchInput.value?.focus()
  }
}

function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
  mobileSearchOpen.value = false
}

async function handleUserCommand(command) {
  if (command === 'profile') return router.push('/blog/profile')
  if (command === 'comments') return router.push('/blog/comments')
  if (command === 'favorites') return router.push('/blog/favorites')
  if (command === 'notifications') return router.push('/blog/notifications')
  if (command === 'authorApplication') return router.push('/blog/author-application')
  if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/blog')
  }
}

function tagTone(name = '') {
  const value = name.toLowerCase()
  if (value.includes('java')) return 'tag-java'
  if (value.includes('spring')) return 'tag-spring'
  if (value.includes('mysql') || value.includes('redis') || value.includes('mybatis') || value.includes('数据库')) return 'tag-database'
  if (value.includes('vue') || value.includes('javascript') || value.includes('前端')) return 'tag-frontend'
  if (value.includes('设计')) return 'tag-design'
  return 'tag-default'
}

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
)

watch(
  () => route.fullPath,
  async () => {
    mobileMenuOpen.value = false
    try {
      const hotRes = await getHotArticles()
      hotArticles.value = (hotRes.data || []).slice(0, 3)
    } catch {
      // 热门榜刷新失败时保留上一次成功结果。
    }
    if (userStore.token) {
      getUnreadNotificationCount().then(res => { unreadCount.value = res.data.count || 0 }).catch(() => {})
    }
  },
)

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    userStore.fetchUserInfo().catch(() => {})
  }
  if (userStore.token) {
    getUnreadNotificationCount().then(res => { unreadCount.value = res.data.count || 0 }).catch(() => {})
  }
  try {
    const [catRes, tagRes, hotRes] = await Promise.all([
      getBlogCategories(),
      getBlogTags(),
      getHotArticles(),
    ])
    categories.value = catRes.data
    tags.value = tagRes.data
    hotArticles.value = (hotRes.data || []).slice(0, 3)
  } catch {
    // 辅助内容加载失败不影响文章主列表。
  }
})
</script>

<style scoped>
.blog-layout {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--blog-color-background);
}
.blog-layout :deep(.el-button),
.blog-layout :deep(.el-input__wrapper),
.blog-layout :deep(.el-select__wrapper),
.blog-layout :deep(.el-pagination button),
.blog-layout :deep(.el-pager li) {
  border-radius: var(--blog-radius-button);
}
.blog-layout :deep(.el-tag) {
  border-radius: var(--blog-radius-tag);
}
.blog-layout :deep(.el-avatar) {
  border-radius: var(--blog-radius-avatar);
}

.blog-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid var(--blog-color-border);
  box-shadow: 0 2px 10px rgba(31, 45, 61, 0.035);
  backdrop-filter: blur(12px);
}
.header-inner {
  max-width: var(--blog-content-max-width);
  height: 68px;
  margin: 0 auto;
  padding: 0 var(--blog-space-6);
  display: flex;
  align-items: center;
  gap: var(--blog-space-5);
  box-sizing: border-box;
}
.logo {
  flex-shrink: 0;
  color: var(--blog-color-primary);
  font-size: 24px;
  font-weight: 750;
  letter-spacing: -0.5px;
  line-height: 1;
  text-decoration: none;
}
.logo:focus-visible,
.nav-item:focus-visible,
.mobile-nav-item:focus-visible,
.login-link:focus-visible,
.header-icon-button:focus-visible,
.tag-item:focus-visible,
.hot-item:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.3);
  outline-offset: 2px;
}
.nav-categories {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--blog-space-1);
  flex: 1;
}
.nav-item {
  padding: 8px 12px;
  border-radius: var(--blog-radius-button);
  color: var(--blog-color-text-secondary);
  font-size: 14px;
  font-weight: 450;
  line-height: 20px;
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.18s ease, background-color 0.18s ease, transform 0.18s ease;
}
.nav-category-group { position: relative; }
.nav-submenu {
  min-width: 160px;
  padding: 8px;
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  display: none;
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
  box-shadow: 0 10px 28px rgba(31, 45, 61, 0.12);
}
.nav-category-group:hover .nav-submenu,
.nav-category-group:focus-within .nav-submenu { display: flex; flex-direction: column; }
.nav-submenu a { padding: 8px 10px; border-radius: var(--blog-radius-button); color: var(--blog-color-text-secondary); font-size: 13px; text-decoration: none; white-space: nowrap; }
.nav-submenu a:hover,
.nav-submenu a.router-link-exact-active { color: var(--blog-color-primary); background: var(--blog-color-primary-soft); }
.nav-item:hover {
  color: var(--blog-color-primary);
  background: var(--blog-color-primary-soft);
}
.nav-item:active {
  transform: translateY(1px);
}
.nav-item.router-link-exact-active {
  color: var(--blog-color-primary);
  background: var(--blog-color-primary-soft);
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: var(--blog-space-2);
  flex-shrink: 0;
}
.desktop-search {
  width: clamp(210px, 18vw, 280px);
}
.desktop-search :deep(.el-input__wrapper),
.mobile-search-panel :deep(.el-input__wrapper) {
  border-radius: var(--blog-radius-button);
  box-shadow: 0 0 0 1px var(--blog-color-border) inset;
  transition: box-shadow 0.18s ease, background-color 0.18s ease;
}
.desktop-search :deep(.el-input__wrapper:hover),
.mobile-search-panel :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--blog-color-border-hover) inset;
}
.desktop-search :deep(.el-input__wrapper.is-focus),
.mobile-search-panel :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(51, 126, 204, 0.28) inset;
}
.desktop-search :deep(.el-input__inner::placeholder),
.mobile-search-panel :deep(.el-input__inner::placeholder) {
  color: #8b95a3;
}
.login-link {
  min-height: 36px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--blog-space-2);
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-button);
  color: var(--blog-color-text-secondary);
  background: var(--blog-color-surface);
  font-size: 13px;
  font-weight: 550;
  text-decoration: none;
  white-space: nowrap;
  font: inherit;
  transition: color 0.18s ease, border-color 0.18s ease, background-color 0.18s ease, transform 0.18s ease;
}
.user-trigger { cursor: pointer; }
.header-user-avatar {
  width: 24px;
  height: 24px;
  flex: 0 0 24px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 0 0 1px var(--blog-color-border);
}
.unread-badge { min-width: 18px; height: 18px; margin-left: 8px; padding: 0 5px; display: inline-flex; align-items: center; justify-content: center; border-radius: 9px; color: #fff; background: #f56c6c; font-size: 11px; }
.login-link:hover {
  color: var(--blog-color-primary);
  border-color: var(--blog-color-border-hover);
  background: var(--blog-color-primary-soft);
}
.login-link:active,
.header-icon-button:active {
  transform: translateY(1px);
}
.header-icon-button {
  width: 38px;
  height: 38px;
  padding: 0;
  display: none;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-button);
  color: var(--blog-color-text-secondary);
  background: var(--blog-color-surface);
  cursor: pointer;
  font-size: 19px;
  transition: color 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;
}
.header-icon-button:hover,
.header-icon-button[aria-expanded='true'] {
  color: var(--blog-color-primary);
  border-color: var(--blog-color-border-hover);
  background: var(--blog-color-primary-soft);
}
.mobile-search-panel,
.mobile-nav {
  display: none;
}
.header-panel-enter-active,
.header-panel-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}
.header-panel-enter-from,
.header-panel-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

.blog-body {
  width: 100%;
  max-width: var(--blog-content-max-width);
  margin: var(--blog-space-5) auto;
  padding: 0 var(--blog-space-6);
  display: grid;
  grid-template-columns: minmax(0, 1fr) var(--blog-sidebar-width);
  flex: 1;
  gap: var(--blog-layout-gap);
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}
.blog-main {
  min-width: 0;
  min-height: 0;
  padding-right: 6px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-color: transparent transparent;
  scrollbar-width: thin;
}
.blog-sidebar {
  min-width: 0;
  min-height: 0;
  padding-right: 6px;
  overflow-y: auto;
  overscroll-behavior: contain;
  scrollbar-gutter: stable;
  scrollbar-color: transparent transparent;
  scrollbar-width: thin;
}
.blog-main:hover,
.blog-sidebar:hover {
  scrollbar-color: rgba(144, 152, 163, 0.32) transparent;
}
.blog-main::-webkit-scrollbar,
.blog-sidebar::-webkit-scrollbar {
  width: 4px;
}
.blog-main::-webkit-scrollbar-track,
.blog-sidebar::-webkit-scrollbar-track {
  background: transparent;
}
.blog-main::-webkit-scrollbar-thumb,
.blog-sidebar::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: transparent;
  transition: background-color 0.18s ease;
}
.blog-main:hover::-webkit-scrollbar-thumb,
.blog-sidebar:hover::-webkit-scrollbar-thumb {
  background: rgba(144, 152, 163, 0.28);
}
.blog-main::-webkit-scrollbar-thumb:hover,
.blog-sidebar::-webkit-scrollbar-thumb:hover {
  background: rgba(112, 121, 134, 0.48);
}
.sidebar-section {
  margin-bottom: var(--blog-space-4);
  padding: var(--blog-space-5);
  border: 1px solid var(--blog-color-border);
  border-radius: var(--blog-radius-card);
  background: var(--blog-color-surface);
}
.sidebar-title {
  margin: 0 0 var(--blog-space-4);
  padding-bottom: var(--blog-space-3);
  border-bottom: 1px solid var(--blog-color-border);
  font-size: 16px;
  font-weight: 650;
}
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: var(--blog-space-2);
}
.tag-item {
  padding: 5px 11px;
  border-radius: var(--blog-radius-tag);
  color: var(--blog-color-text-secondary);
  background: var(--blog-color-background);
  font-size: 13px;
  text-decoration: none;
  transition: color 0.18s ease, background-color 0.18s ease;
}
.tag-item:hover,
.tag-item.router-link-exact-active {
  color: var(--blog-color-primary);
  box-shadow: 0 0 0 1px currentColor inset;
}
.tag-java { color: #9a5b13; background: #fff4e5; }
.tag-spring { color: #3e7b43; background: #edf8ee; }
.tag-database { color: #7155a4; background: #f3effb; }
.tag-frontend { color: #28719c; background: #eaf6fb; }
.tag-design { color: #8a6a16; background: #fff8dc; }
.tag-default { color: var(--blog-color-text-secondary); background: var(--blog-color-background); }
.about-text {
  margin: 0;
  color: var(--blog-color-text-secondary);
  font-size: 14px;
  line-height: 1.7;
}
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.hot-item {
  padding: var(--blog-space-2) 0;
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 8px;
  border-bottom: 1px solid var(--blog-color-border);
  text-decoration: none;
}
.hot-rank {
  padding-top: 1px;
  color: var(--blog-color-text-muted);
  font-size: 15px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.hot-item:nth-child(-n + 3) .hot-rank {
  color: var(--blog-color-primary);
}
.hot-content {
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.hot-item:last-child {
  border-bottom: none;
}
.hot-item:hover .hot-title {
  color: var(--blog-color-primary);
}
.hot-title {
  overflow: hidden;
  display: -webkit-box;
  color: var(--blog-color-text);
  font-size: 14px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.hot-views {
  margin-top: 4px;
  color: var(--blog-color-text-muted);
  font-size: 12px;
}
.hot-views .el-icon {
  margin-right: 4px;
  vertical-align: -2px;
}
.empty-text {
  margin: 0;
  color: var(--blog-color-text-muted);
  font-size: 13px;
}
.blog-footer {
  padding: 22px;
  border-top: 1px solid var(--blog-color-border);
  color: var(--blog-color-text-muted);
  background: var(--blog-color-surface);
  font-size: 13px;
  text-align: center;
}
.blog-footer p {
  margin: 0;
}

@media (max-width: 1100px) {
  .desktop-nav {
    display: none;
  }
  .header-inner {
    max-width: 100%;
  }
  .header-actions {
    margin-left: auto;
  }
  .menu-trigger {
    display: inline-flex;
  }
  .mobile-nav {
    max-height: min(70vh, 520px);
    padding: 12px 20px 18px;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--blog-space-2);
    overflow-y: auto;
    border-top: 1px solid var(--blog-color-border);
    background: var(--blog-color-surface);
  }
  .mobile-nav-item {
    padding: 11px 13px;
    border-radius: var(--blog-radius-button);
    color: var(--blog-color-text-secondary);
    font-size: 14px;
    font-weight: 450;
    text-decoration: none;
  }
  .mobile-nav-item:hover,
  .mobile-nav-item.router-link-exact-active {
    color: var(--blog-color-primary);
    background: var(--blog-color-primary-soft);
    font-weight: 600;
  }
  .mobile-category-group { display: flex; flex-direction: column; }
  .mobile-root-item { font-weight: 650; }
  .mobile-child-item { padding-left: 26px; font-size: 13px; }
}

@media (max-width: 768px) {
  .blog-layout {
    height: auto;
    min-height: 100vh;
    overflow: visible;
  }
  .header-inner {
    height: 60px;
    padding: 0 16px;
    gap: 12px;
  }
  .logo {
    font-size: 21px;
  }
  .desktop-search {
    display: none;
  }
  .mobile-search-trigger {
    display: inline-flex;
  }
  .mobile-search-panel {
    padding: 12px 16px 16px;
    display: block;
    border-top: 1px solid var(--blog-color-border);
    background: var(--blog-color-surface);
  }
  .blog-body {
    margin: 16px auto;
    padding: 0 var(--blog-space-4);
    grid-template-columns: minmax(0, 1fr);
    overflow: visible;
  }
  .blog-main {
    padding-right: 0;
    overflow: visible;
    scrollbar-gutter: auto;
  }
  .blog-sidebar {
    display: none;
  }
}

@media (max-width: 480px) {
  .login-link {
    width: 38px;
    height: 38px;
    min-height: 38px;
    padding: 0;
  }
  .login-link span {
    position: absolute;
    width: 1px;
    height: 1px;
    padding: 0;
    margin: -1px;
    overflow: hidden;
    clip: rect(0, 0, 0, 0);
    white-space: nowrap;
    border: 0;
  }
  .mobile-nav {
    grid-template-columns: 1fr;
  }
}
</style>
