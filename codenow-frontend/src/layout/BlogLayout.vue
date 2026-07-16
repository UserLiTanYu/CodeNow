<template>
  <div class="blog-layout">
    <header class="blog-header">
      <div class="header-inner">
        <router-link to="/blog" class="logo" aria-label="码上记博客首页">码上记</router-link>

        <nav class="nav-categories desktop-nav" aria-label="博客分类导航">
          <router-link to="/blog" class="nav-item">首页</router-link>
          <router-link
            v-for="cat in categories"
            :key="cat.id"
            :to="`/blog/category/${cat.id}`"
            class="nav-item"
          >
            {{ cat.name }}
          </router-link>
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

          <router-link to="/" class="admin-link">
            <el-icon><UserFilled /></el-icon>
            <span>管理后台</span>
          </router-link>

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
          <router-link
            v-for="cat in categories"
            :key="cat.id"
            :to="`/blog/category/${cat.id}`"
            class="mobile-nav-item"
          >
            {{ cat.name }}
          </router-link>
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
              v-for="item in hotArticles"
              :key="item.article.id"
              :to="`/blog/article/${item.article.id}`"
              class="hot-item"
            >
              <span class="hot-title">{{ item.article.title }}</span>
              <span class="hot-views">{{ item.article.viewCount }} 阅读</span>
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
              class="tag-item"
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
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close, Menu, Search, UserFilled } from '@element-plus/icons-vue'
import { getBlogCategories, getBlogTags, getHotArticles } from '@/api/blog'

const route = useRoute()
const router = useRouter()
const categories = ref([])
const tags = ref([])
const hotArticles = ref([])
const searchKeyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
const mobileMenuOpen = ref(false)
const mobileSearchOpen = ref(false)
const mobileSearchInput = ref()

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

watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
)

watch(
  () => route.fullPath,
  () => {
    mobileMenuOpen.value = false
  },
)

onMounted(async () => {
  try {
    const [catRes, tagRes, hotRes] = await Promise.all([
      getBlogCategories(),
      getBlogTags(),
      getHotArticles({ topN: 10 }),
    ])
    categories.value = catRes.data
    tags.value = tagRes.data
    hotArticles.value = hotRes.data
  } catch {
    // 辅助内容加载失败不影响文章主列表。
  }
})
</script>

<style scoped>
.blog-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.blog-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.96);
  border-bottom: 1px solid #e4e7ed;
  box-shadow: 0 4px 18px rgba(31, 45, 61, 0.06);
  backdrop-filter: blur(12px);
}
.header-inner {
  max-width: 1440px;
  height: 68px;
  margin: 0 auto;
  padding: 0 28px;
  display: flex;
  align-items: center;
  gap: 28px;
  box-sizing: border-box;
}
.logo {
  flex-shrink: 0;
  color: #337ecc;
  font-size: 24px;
  font-weight: 750;
  letter-spacing: -0.5px;
  line-height: 1;
  text-decoration: none;
}
.logo:focus-visible,
.nav-item:focus-visible,
.mobile-nav-item:focus-visible,
.admin-link:focus-visible,
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
  gap: 6px;
  flex: 1;
}
.nav-item {
  padding: 9px 13px;
  border-radius: 7px;
  color: #606266;
  font-size: 14px;
  font-weight: 450;
  line-height: 20px;
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.18s ease, background-color 0.18s ease, transform 0.18s ease;
}
.nav-item:hover {
  color: #337ecc;
  background: #f0f7ff;
}
.nav-item:active {
  transform: translateY(1px);
}
.nav-item.router-link-exact-active {
  color: #337ecc;
  background: #eaf4ff;
  font-weight: 600;
}
.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.desktop-search {
  width: clamp(210px, 18vw, 280px);
}
.desktop-search :deep(.el-input__wrapper),
.mobile-search-panel :deep(.el-input__wrapper) {
  border-radius: 9px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
  transition: box-shadow 0.18s ease, background-color 0.18s ease;
}
.desktop-search :deep(.el-input__wrapper:hover),
.mobile-search-panel :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #a8abb2 inset;
}
.desktop-search :deep(.el-input__wrapper.is-focus),
.mobile-search-panel :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.35) inset;
}
.admin-link {
  min-height: 36px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  border: 1px solid #b3d8ff;
  border-radius: 9px;
  color: #337ecc;
  background: #fff;
  font-size: 13px;
  font-weight: 550;
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.18s ease, border-color 0.18s ease, background-color 0.18s ease, transform 0.18s ease;
}
.admin-link:hover {
  color: #fff;
  border-color: #409eff;
  background: #409eff;
}
.admin-link:active,
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
  border: 1px solid #dcdfe6;
  border-radius: 9px;
  color: #606266;
  background: #fff;
  cursor: pointer;
  font-size: 19px;
  transition: color 0.18s ease, border-color 0.18s ease, background-color 0.18s ease;
}
.header-icon-button:hover,
.header-icon-button[aria-expanded='true'] {
  color: #337ecc;
  border-color: #b3d8ff;
  background: #ecf5ff;
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
  max-width: 1240px;
  margin: 28px auto;
  padding: 0 24px;
  display: flex;
  flex: 1;
  gap: 24px;
  box-sizing: border-box;
}
.blog-main {
  min-width: 0;
  flex: 1;
}
.blog-sidebar {
  width: 280px;
  flex-shrink: 0;
}
.sidebar-section {
  margin-bottom: 16px;
  padding: 20px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}
.sidebar-title {
  margin: 0 0 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid #ebeef5;
  font-size: 16px;
  font-weight: 650;
}
.tag-cloud {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-item {
  padding: 5px 11px;
  border-radius: 6px;
  color: #606266;
  background: #f2f3f5;
  font-size: 13px;
  text-decoration: none;
  transition: color 0.18s ease, background-color 0.18s ease;
}
.tag-item:hover,
.tag-item.router-link-exact-active {
  color: #fff;
  background: #409eff;
}
.about-text {
  margin: 0;
  color: #7d8592;
  font-size: 14px;
  line-height: 1.7;
}
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.hot-item {
  padding: 9px 0;
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid #f0f2f5;
  text-decoration: none;
}
.hot-item:last-child {
  border-bottom: none;
}
.hot-item:hover .hot-title {
  color: #337ecc;
}
.hot-title {
  overflow: hidden;
  display: -webkit-box;
  color: #303133;
  font-size: 14px;
  line-height: 1.55;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}
.hot-views {
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}
.empty-text {
  margin: 0;
  color: #b1b3b8;
  font-size: 13px;
}
.blog-footer {
  padding: 22px;
  border-top: 1px solid #ebeef5;
  color: #909399;
  background: #fff;
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
    gap: 8px;
    overflow-y: auto;
    border-top: 1px solid #ebeef5;
    background: #fff;
  }
  .mobile-nav-item {
    padding: 11px 13px;
    border-radius: 8px;
    color: #606266;
    font-size: 14px;
    font-weight: 450;
    text-decoration: none;
  }
  .mobile-nav-item:hover,
  .mobile-nav-item.router-link-exact-active {
    color: #337ecc;
    background: #eaf4ff;
    font-weight: 600;
  }
}

@media (max-width: 768px) {
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
    border-top: 1px solid #ebeef5;
    background: #fff;
  }
  .blog-body {
    margin: 16px auto;
    padding: 0 14px;
  }
  .blog-sidebar {
    display: none;
  }
}

@media (max-width: 480px) {
  .admin-link {
    width: 38px;
    height: 38px;
    min-height: 38px;
    padding: 0;
  }
  .admin-link span {
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
