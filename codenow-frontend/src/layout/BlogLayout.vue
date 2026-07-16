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
  async () => {
    mobileMenuOpen.value = false
    try {
      const hotRes = await getHotArticles()
      hotArticles.value = (hotRes.data || []).slice(0, 3)
    } catch {
      // 热门榜刷新失败时保留上一次成功结果。
    }
  },
)

onMounted(async () => {
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
  min-height: 100vh;
  display: flex;
  flex-direction: column;
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
.admin-link {
  min-height: 36px;
  padding: 0 14px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--blog-space-2);
  border: 1px solid var(--blog-color-border-hover);
  border-radius: var(--blog-radius-button);
  color: var(--blog-color-primary);
  background: var(--blog-color-surface);
  font-size: 13px;
  font-weight: 550;
  text-decoration: none;
  white-space: nowrap;
  transition: color 0.18s ease, border-color 0.18s ease, background-color 0.18s ease, transform 0.18s ease;
}
.admin-link:hover {
  color: #fff;
  border-color: var(--blog-color-primary);
  background: var(--blog-color-primary);
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
  box-sizing: border-box;
}
.blog-main {
  min-width: 0;
  flex: 1;
}
.blog-sidebar {
  min-width: 0;
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
  color: #fff;
  background: var(--blog-color-primary);
}
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
  display: flex;
  flex-direction: column;
  border-bottom: 1px solid var(--blog-color-border);
  text-decoration: none;
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
    border-top: 1px solid var(--blog-color-border);
    background: var(--blog-color-surface);
  }
  .blog-body {
    margin: 16px auto;
    padding: 0 var(--blog-space-4);
    grid-template-columns: minmax(0, 1fr);
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
