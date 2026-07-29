<template>
  <!-- 博客前台布局组件：顶部导航栏 + 主内容区 + 侧边栏 + 页脚 -->
  <div class="blog-layout">
    <!-- 顶部导航栏 -->
    <header class="blog-header">
      <div class="header-inner">
        <!-- Logo 品牌标识 -->
        <router-link to="/blog" class="logo" aria-label="码上记博客首页">码上记</router-link>

        <!-- 桌面端页面级导航；技术分类独立放在左侧栏 -->
        <nav class="nav-categories desktop-nav" aria-label="博客主导航">
          <router-link to="/blog" class="nav-item">文章</router-link>
          <router-link to="/blog/authors" class="nav-item">作者广场</router-link>
          <router-link to="/blog/about" class="nav-item">关于本站</router-link>
        </nav>

        <!-- 头部右侧操作区域 -->
        <div class="header-actions">

          <!-- 未登录时显示登录链接 -->
          <router-link v-if="!userStore.isLoggedIn" :to="loginTarget" class="login-link">
            <el-icon><User /></el-icon>
            <span>登录</span>
          </router-link>
          <!-- 管理员入口：前往后台 -->
          <router-link v-if="userStore.isAdmin" to="/" class="login-link admin-link">
            <el-icon><Setting /></el-icon>
            <span>前往后台</span>
          </router-link>
          <!-- 作者入口：作者工作台 -->
          <router-link v-if="userStore.canEnterAuthorConsole" to="/author-console/articles" class="login-link admin-link">
            <el-icon><EditPen /></el-icon>
            <span>作者工作台</span>
          </router-link>
          <!-- 已登录用户下拉菜单 -->
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
                <el-dropdown-item v-if="userStore.canEnterAuthorConsole" command="authorConsole">作者工作台</el-dropdown-item>
                <el-dropdown-item v-if="!userStore.isAdmin" command="authorApplication">{{ userStore.isAuthor ? '作者身份' : '申请成为作者' }}</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 移动端搜索按钮 -->
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

          <!-- 移动端菜单按钮 -->
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

      <!-- 移动端搜索面板（展开/收起动画） -->
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

      <!-- 移动端分类导航面板（展开/收起动画） -->
      <Transition name="header-panel">
        <nav
          v-if="mobileMenuOpen"
          id="mobile-category-nav"
          class="mobile-nav"
          aria-label="移动端博客分类导航"
        >
          <router-link to="/blog" class="mobile-nav-item">文章</router-link>
          <router-link to="/blog/authors" class="mobile-nav-item">作者广场</router-link>
          <router-link to="/blog/about" class="mobile-nav-item">关于本站</router-link>
          <div
            v-for="cat in categories"
            :key="cat.id"
            class="mobile-category-group"
          >
            <router-link
              :to="categoryTarget(cat.id)"
              :class="['mobile-nav-item', 'mobile-root-item', { 'author-filter-active': isAuthorCategorySelected(cat.id) }]"
              :exact-active-class="filterExactActiveClass"
            >{{ cat.name }}</router-link>
            <router-link
              v-for="child in cat.children || []"
              :key="child.id"
              :to="categoryTarget(child.id)"
              :class="['mobile-nav-item', 'mobile-child-item', { 'author-filter-active': isAuthorCategorySelected(child.id) }]"
              :exact-active-class="filterExactActiveClass"
            >{{ child.name }}</router-link>
          </div>
        </nav>
      </Transition>
    </header>

    <!-- 页面主体区域：作者广场使用全宽内容，其他页面保留左右侧栏 -->
    <div :class="['blog-body', { 'sidebarless-layout': isSidebarlessPage }]">
      <aside v-if="!isSidebarlessPage" class="blog-category-sidebar" aria-label="文章分类导航">
        <section class="sidebar-section category-navigation">
          <h3 class="category-navigation-title">文章分类</h3>
          <router-link
            :to="categoryHomeTarget"
            :class="['category-all-link', { active: isCategoryHomeSelected }]"
            :exact-active-class="filterExactActiveClass"
          >
            <span>全部文章</span>
            <span v-if="articleTotal" class="category-count">{{ articleTotal }}</span>
          </router-link>

          <div class="category-tree">
            <div v-for="cat in categories" :key="cat.id" class="category-tree-branch">
              <div :class="['category-tree-row', { active: isCategoryBranchSelected(cat) }]">
                <router-link
                  :to="categoryTarget(cat.id)"
                  :class="['category-tree-link', { active: isCategorySelected(cat.id) }]"
                  :exact-active-class="filterExactActiveClass"
                >{{ cat.name }}</router-link>
                <button
                  v-if="cat.children?.length"
                  type="button"
                  class="category-tree-toggle"
                  :aria-expanded="isCategoryExpanded(cat.id)"
                  :aria-controls="`category-children-${cat.id}`"
                  :aria-label="`${isCategoryExpanded(cat.id) ? '折叠' : '展开'}${cat.name}的子分类`"
                  @click="toggleCategory(cat.id)"
                >
                  <el-icon :class="{ expanded: isCategoryExpanded(cat.id) }"><ArrowRight /></el-icon>
                </button>
              </div>
              <Transition name="category-children">
                <div
                  v-if="cat.children?.length && isCategoryExpanded(cat.id)"
                  :id="`category-children-${cat.id}`"
                  class="category-tree-children"
                >
                  <router-link
                    v-for="child in cat.children"
                    :key="child.id"
                    :to="categoryTarget(child.id)"
                    :class="['category-tree-child', { active: isCategorySelected(child.id) }]"
                    :exact-active-class="filterExactActiveClass"
                  >{{ child.name }}</router-link>
                </div>
              </Transition>
            </div>
          </div>
        </section>
      </aside>

      <!-- 主内容区域：渲染子路由组件 -->
      <main class="blog-main">
        <router-view />
      </main>
      <!-- 右侧边栏 -->
      <aside v-if="!isSidebarlessPage" class="blog-sidebar">
        <!-- 热门文章列表 -->
        <div class="sidebar-section">
          <h3 class="sidebar-title">{{ isAuthorPage ? '作者热门文章' : '热门文章' }}</h3>
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
        <!-- 标签云 -->
        <div class="sidebar-section">
          <h3 class="sidebar-title">{{ isAuthorPage ? '作者标签' : '标签' }}</h3>
          <div class="tag-cloud">
            <router-link
              v-for="tag in orderedTags"
              :key="tag.id"
              :to="tagTarget(tag.id)"
              :class="['tag-item', tagTone(tag.name), { active: isAuthorTagSelected(tag.id) }]"
              :exact-active-class="filterExactActiveClass"
            >
              {{ tag.name }}
            </router-link>
          </div>
        </div>
        <!-- 当前作者个人简介 -->
        <div id="author-profile-summary" class="sidebar-section">
          <h3 class="sidebar-title">个人简介</h3>
          <p class="about-text">{{ authorBio || '该作者暂未填写个人简介。' }}</p>
        </div>
      </aside>
    </div>

    <!-- 页脚 -->
    <footer class="blog-footer">
      <p>&copy; {{ new Date().getFullYear() }} 码上记 CodeNow. All rights reserved.</p>
    </footer>
  </div>
</template>

<script setup>
/**
 * 博客前台布局组件
 * 提供博客前台页面的整体布局框架，包含顶部导航栏、主内容区、右侧边栏和页脚。
 * 支持桌面端和移动端响应式布局，移动端有折叠搜索面板和分类导航菜单。
 * 侧边栏根据当前页面（普通博客页/作者页）动态加载不同的数据。
 */
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Close, EditPen, Menu, Search, Setting, User, View } from '@element-plus/icons-vue'
import { getBlogArticles, getBlogCategories, getBlogTags, getHotArticles, getPublicAuthor, getPublicAuthorCategories, getPublicAuthorTags, getPublicAuthorArticles, getSiteProfile } from '@/api/blog'
import { SITE_OWNER_ID } from '@/config/site'
import { avatarUrl, useDefaultAvatar } from '@/utils/avatar'
import { getUnreadNotificationCount } from '@/api/member'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { normalizeRedirectTarget } from '@/utils/routeRedirect'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 博客分类列表（树形结构） */
const categories = ref([])
/** 标签列表 */
const tags = ref([])
/** 热门文章列表 */
const hotArticles = ref([])
/** 当前范围内的公开文章总数 */
const articleTotal = ref(0)
/** 当前内容作者的公开个人简介 */
const authorBio = ref('')

/** 左侧栏已展开的一级分类ID；初始为空，默认全部折叠。 */
const expandedCategoryIds = ref(new Set())

/** 右侧标签按知识体系展示；未列入的新增标签保持接口顺序并排在末尾。 */
const TAG_DISPLAY_ORDER = [
  'Java', 'Spring Boot', 'Mybatis-Plus', 'Vue 3', 'MySQL',
  'Redis', 'Docker', 'Git', 'JavaScript', 'Element Plus', 'Linux', '设计模式',
  'Java 入门与环境', 'Java 基础语法', '面向对象编程', 'Java 常用 API',
  '集合框架', '异常与泛型', 'IO 与 NIO', '多线程与并发', 'JVM 基础', '现代 Java',
  'Spring IoC 与 AOP', 'Spring MVC', 'MyBatis 与 MyBatis-Plus',
  '认证与权限', 'Spring Cloud', '消息与任务', 'Spring 测试与监控',
  'HTML 与 CSS', 'JavaScript 基础', 'TypeScript', 'React', '前端工程化',
  '数据库与 SQL 基础', 'MySQL 核心', 'MySQL 运维', 'MongoDB', '数据访问实践',
  'IntelliJ IDEA', 'VS Code', 'Maven 与 Gradle', 'CI/CD', '需求与架构设计',
  '后端项目实战', '前端项目实战', '全栈联调', '测试与质量保障', '部署与运维',
  '学习方法', 'Java 面试', '算法与数据结构', '计算机基础', '职业成长',
]
const tagDisplayRank = new Map(TAG_DISPLAY_ORDER.map((name, index) => [name, index]))
const orderedTags = computed(() => [...tags.value].sort((left, right) =>
  (tagDisplayRank.get(left.name) ?? Number.MAX_SAFE_INTEGER)
  - (tagDisplayRank.get(right.name) ?? Number.MAX_SAFE_INTEGER)))
/** 搜索关键词（从 URL query 参数初始化） */
const searchKeyword = ref(typeof route.query.keyword === 'string' ? route.query.keyword : '')
/** 移动端菜单是否展开 */
const mobileMenuOpen = ref(false)
/** 移动端搜索面板是否展开 */
const mobileSearchOpen = ref(false)
/** 移动端搜索输入框引用 */
const mobileSearchInput = ref()
/** 登录链接目标（携带当前页面作为重定向参数） */
const loginTarget = computed(() => ({
  path: '/login',
  query: { redirect: normalizeRedirectTarget(route.fullPath) },
}))
/** 未读消息数量 */
const unreadCount = ref(0)
/** 当前是否为作者页面 */
const isAuthorPage = computed(() => /^\/blog\/author\/\d+/.test(route.path))
/** 作者广场独立使用全宽布局，不展示文章分类与推荐侧栏。 */
const isAuthorsPage = computed(() => route.path === '/blog/authors')
/** 个人功能、政策和作者广场使用独立单栏布局。 */
const isSidebarlessPage = computed(() => isAuthorsPage.value
  || /^\/blog\/(about|profile|favorites|comments|notifications|author-application|terms|privacy)$/.test(route.path))
/** 公开博客首页使用站长作者范围。 */
const isSiteHome = computed(() => route.path === '/blog')
/** 作者ID（从路由参数提取） */
const authorId = computed(() => route.params.id)
/** 作者主页的数据作者范围；首页使用独立的管理员内容范围。 */
const scopedAuthorId = computed(() => isAuthorPage.value ? authorId.value : null)
const isAuthorScopedPage = computed(() => isAuthorPage.value || isSiteHome.value)
/** 作者页选中的分类ID，以URL为唯一状态源。 */
const selectedAuthorCategoryId = computed(() => route.query.categoryId ?? null)
const filterExactActiveClass = computed(() => isAuthorScopedPage.value ? 'author-route-exact-match' : 'router-link-exact-active')
/** 左侧“全部文章”的跳转目标。 */
const categoryHomeTarget = computed(() => isAuthorPage.value && authorId.value
  ? `/blog/author/${authorId.value}`
  : '/blog')
/** 左侧“全部文章”是否处于选中状态。 */
const isCategoryHomeSelected = computed(() => isAuthorScopedPage.value
  ? !route.query.categoryId && !route.query.tagId && (isAuthorPage.value || isSiteHome.value)
  : route.path === '/blog')

/** 标准化搜索关键词：去除首尾空格并限制最大长度为100字符 */
function normalizedKeyword() {
  return searchKeyword.value.trim().slice(0, 100)
}

/** 提交搜索：关闭移动端面板后跳转到搜索结果页 */
function submitSearch() {
  const keyword = normalizedKeyword()
  searchKeyword.value = keyword
  mobileSearchOpen.value = false
  mobileMenuOpen.value = false
  router.push({ path: '/blog', query: keyword ? { keyword } : {} })
}

/** 清空搜索关键词并返回博客首页 */
function clearSearch() {
  searchKeyword.value = ''
  if (route.query.keyword) {
    router.push({ path: '/blog' })
  }
}

/** 分类链接：首页和作者页保持作者上下文，其他页面进入全站分类页。 */
function categoryTarget(categoryId) {
  if (isAuthorPage.value && authorId.value) {
    return { path: `/blog/author/${authorId.value}`, query: { categoryId } }
  }
  if (isSiteHome.value) return { path: '/blog', query: { categoryId } }
  return `/blog/category/${categoryId}`
}

/** 标签链接：首页和作者页保持作者上下文，其他页面进入全站标签页。 */
function tagTarget(tagId) {
  if (isAuthorPage.value && authorId.value) {
    return { path: `/blog/author/${authorId.value}`, query: { tagId } }
  }
  if (isSiteHome.value) return { path: '/blog', query: { tagId } }
  return `/blog/tag/${tagId}`
}

/** 判断作者分类是否处于选中状态。 */
function isAuthorCategorySelected(catId) {
  return String(selectedAuthorCategoryId.value ?? '') === String(catId)
}

/** 判断指定分类是否是当前筛选条件。 */
function isCategorySelected(categoryId) {
  if (isAuthorScopedPage.value) return isAuthorCategorySelected(categoryId)
  return route.path.startsWith('/blog/category/')
    && String(route.params.id ?? '') === String(categoryId)
}

/** 一级分类或其任一子分类被选中时，标记该分支。 */
function isCategoryBranchSelected(category) {
  return isCategorySelected(category.id)
    || category.children?.some(child => isCategorySelected(child.id))
}

/** 判断一级分类是否展开。 */
function isCategoryExpanded(categoryId) {
  return expandedCategoryIds.value.has(String(categoryId))
}

/** 切换一级分类的展开状态。 */
function toggleCategory(categoryId) {
  const id = String(categoryId)
  const next = new Set(expandedCategoryIds.value)
  if (next.has(id)) next.delete(id)
  else next.add(id)
  expandedCategoryIds.value = next
}

/** 判断作者标签是否处于选中状态。 */
function isAuthorTagSelected(tagId) {
  return isAuthorScopedPage.value && String(route.query.tagId ?? '') === String(tagId)
}


/** 切换移动端搜索面板的展开/收起状态，并自动聚焦输入框 */
async function toggleMobileSearch() {
  mobileSearchOpen.value = !mobileSearchOpen.value
  mobileMenuOpen.value = false
  if (mobileSearchOpen.value) {
    await nextTick()
    mobileSearchInput.value?.focus()
  }
}

/** 切换移动端分类导航菜单的展开/收起状态 */
function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
  mobileSearchOpen.value = false
}

/** 处理用户下拉菜单命令：跳转到对应页面或执行退出登录 */
async function handleUserCommand(command) {
  if (command === 'profile') return router.push('/blog/profile')
  if (command === 'comments') return router.push('/blog/comments')
  if (command === 'favorites') return router.push('/blog/favorites')
  if (command === 'notifications') return router.push('/blog/notifications')
  if (command === 'authorConsole') return router.push('/author-console/articles')
  if (command === 'authorApplication') return router.push('/blog/author-application')
  if (command === 'logout') {
    await userStore.logout()
    ElMessage.success('已退出登录')
    router.push('/blog')
  }
}

/** 根据标签名称关键词返回对应的色调 CSS 类名 */
function tagTone(name = '') {
  const value = name.toLowerCase()
  if (value.includes('java')) return 'tag-java'
  if (value.includes('spring')) return 'tag-spring'
  if (value.includes('mysql') || value.includes('redis') || value.includes('mybatis') || value.includes('数据库')) return 'tag-database'
  if (value.includes('vue') || value.includes('javascript') || value.includes('前端')) return 'tag-frontend'
  if (value.includes('设计')) return 'tag-design'
  return 'tag-default'
}

/** 监听 URL 搜索关键词变化，同步到本地搜索框状态 */
watch(
  () => route.query.keyword,
  (keyword) => {
    searchKeyword.value = typeof keyword === 'string' ? keyword : ''
  },
)

let sidebarRequestId = 0

/** 根据当前作者范围加载分类、标签、热门文章和文章总数。 */
async function loadSidebarData() {
  const currentRequest = ++sidebarRequestId
  const scopeId = scopedAuthorId.value
  categories.value = []
  tags.value = []
  hotArticles.value = []
  articleTotal.value = 0
  authorBio.value = ''
  if (isSidebarlessPage.value) return
  try {
    if (scopeId) {
      // 先确认作者仍可公开访问，避免不存在或已下架作者触发多条附属接口错误提示。
      const profileRes = await getPublicAuthor(scopeId, { silentError: true })
      if (currentRequest !== sidebarRequestId) return
      const [catRes, tagRes, artRes] = await Promise.all([
        getPublicAuthorCategories(scopeId),
        getPublicAuthorTags(scopeId),
        getPublicAuthorArticles(scopeId, { pageNum: 1, pageSize: 3, sort: 'mostViewed' }),
      ])
      if (currentRequest !== sidebarRequestId) return
      categories.value = catRes.data || []
      tags.value = tagRes.data || []
      hotArticles.value = (artRes.data?.records || []).slice(0, 3)
      articleTotal.value = artRes.data?.total || 0
      authorBio.value = profileRes?.data?.bio?.trim() || ''
      return
    }
    if (isSiteHome.value) {
      const [profileRes, catRes, tagRes, artRes] = await Promise.all([
        getSiteProfile().catch(() => null),
        getBlogCategories({ ownerId: SITE_OWNER_ID }),
        getBlogTags({ ownerId: SITE_OWNER_ID }),
        getBlogArticles({ pageNum: 1, pageSize: 3, sort: 'mostViewed', authorId: SITE_OWNER_ID }),
      ])
      if (currentRequest !== sidebarRequestId) return
      categories.value = catRes.data || []
      tags.value = tagRes.data || []
      hotArticles.value = (artRes.data?.records || []).slice(0, 3)
      articleTotal.value = artRes.data?.total || 0
      authorBio.value = profileRes?.data?.bio?.trim() || ''
      return
    }
    const [profileRes, catRes, tagRes, hotRes, articleRes] = await Promise.all([
      getSiteProfile().catch(() => null),
      getBlogCategories(),
      getBlogTags(),
      getHotArticles(),
      getBlogArticles({ pageNum: 1, pageSize: 1 }),
    ])
    if (currentRequest !== sidebarRequestId) return
    categories.value = catRes.data || []
    tags.value = tagRes.data || []
    hotArticles.value = (hotRes.data || []).slice(0, 3)
    articleTotal.value = articleRes.data?.total || 0
    authorBio.value = profileRes?.data?.bio?.trim() || ''
  } catch {
    // 辅助内容加载失败不影响文章主列表。
  }
}

/** 仅在数据作用域变化时重新加载侧栏；搜索和筛选不重复请求相同导航数据。 */
watch(
  () => scopedAuthorId.value
    ? `author:${scopedAuthorId.value}`
    : (isSiteHome.value ? `owner:${SITE_OWNER_ID}` : (isSidebarlessPage.value ? `standalone:${route.path}` : 'global')),
  loadSidebarData,
  { immediate: true },
)

watch(() => route.fullPath, () => { mobileMenuOpen.value = false })

/** 组件挂载：获取用户信息和未读消息数。 */
onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.fetchUserInfo().catch(() => {})
  }
  if (userStore.token) {
    getUnreadNotificationCount().then(res => { unreadCount.value = res.data.count || 0 }).catch(() => {})
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
  max-width: 1720px;
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
.category-all-link:focus-visible,
.category-tree-link:focus-visible,
.category-tree-toggle:focus-visible,
.category-tree-child:focus-visible,
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
.nav-item:hover {
  color: var(--blog-color-primary);
  background: var(--blog-color-primary-soft);
}
.nav-item:active {
  transform: translateY(1px);
}
.nav-item.router-link-exact-active,
.nav-item.author-filter-active {
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
.mobile-search-panel :deep(.el-input__wrapper) {
  border-radius: var(--blog-radius-button);
  box-shadow: 0 0 0 1px var(--blog-color-border) inset;
  transition: box-shadow 0.18s ease, background-color 0.18s ease;
}
.mobile-search-panel :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--blog-color-border-hover) inset;
}
.mobile-search-panel :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(51, 126, 204, 0.28) inset;
}
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
  max-width: 1720px;
  margin: var(--blog-space-5) auto;
  padding: 0 var(--blog-space-6);
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 300px;
  flex: 1;
  gap: var(--blog-layout-gap);
  min-height: 0;
  overflow: hidden;
  box-sizing: border-box;
}
.blog-body.sidebarless-layout {
  max-width: 1248px;
  grid-template-columns: minmax(0, 1fr);
}
.blog-category-sidebar,
.blog-main,
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
.blog-category-sidebar:hover,
.blog-main:hover,
.blog-sidebar:hover {
  scrollbar-color: rgba(144, 152, 163, 0.32) transparent;
}
.blog-category-sidebar::-webkit-scrollbar,
.blog-main::-webkit-scrollbar,
.blog-sidebar::-webkit-scrollbar {
  width: 4px;
}
.blog-category-sidebar::-webkit-scrollbar-track,
.blog-main::-webkit-scrollbar-track,
.blog-sidebar::-webkit-scrollbar-track {
  background: transparent;
}
.blog-category-sidebar::-webkit-scrollbar-thumb,
.blog-main::-webkit-scrollbar-thumb,
.blog-sidebar::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: transparent;
  transition: background-color 0.18s ease;
}
.blog-category-sidebar:hover::-webkit-scrollbar-thumb,
.blog-main:hover::-webkit-scrollbar-thumb,
.blog-sidebar:hover::-webkit-scrollbar-thumb {
  background: rgba(144, 152, 163, 0.28);
}
.blog-category-sidebar::-webkit-scrollbar-thumb:hover,
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
.category-navigation {
  min-height: 100%;
  margin-bottom: 0;
  padding: 18px 14px;
}
.category-navigation-title {
  margin: 0 10px 14px;
  padding-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid var(--blog-color-border);
  color: var(--blog-color-text);
  font-size: 16px;
  font-weight: 650;
}
.category-navigation-title::before {
  width: 4px;
  height: 18px;
  border-radius: 2px;
  background: var(--blog-color-primary);
  content: '';
}
.category-all-link {
  min-height: 38px;
  padding: 0 12px;
  display: flex;
  align-items: center;
  border: 0;
  border-radius: var(--blog-radius-button);
  color: var(--blog-color-text-secondary);
  background: transparent;
  font-family: inherit;
  font-size: 13px;
  line-height: 1.4;
  text-align: left;
  text-decoration: none;
  cursor: pointer;
  transition: color 0.16s ease, background-color 0.16s ease;
}
.category-all-link {
  justify-content: space-between;
  font-weight: 650;
}
.category-all-link:hover,
.category-all-link.active,
.category-all-link.router-link-exact-active {
  color: var(--blog-color-primary);
  background: var(--blog-color-primary-soft);
}
.category-count {
  color: currentColor;
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}
.category-tree {
  margin-top: 10px;
}
.category-tree-branch + .category-tree-branch {
  margin-top: 3px;
}
.category-tree-row {
  min-height: 40px;
  display: flex;
  align-items: center;
  border-radius: var(--blog-radius-button);
  transition: background-color 0.16s ease;
}
.category-tree-row:hover,
.category-tree-row.active {
  background: var(--blog-color-primary-soft);
}
.category-tree-link {
  min-width: 0;
  min-height: 40px;
  padding: 0 4px 0 12px;
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
  color: var(--blog-color-text-secondary);
  font-size: 13px;
  line-height: 1.4;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.category-tree-link:hover,
.category-tree-link.active {
  color: var(--blog-color-primary);
}
.category-tree-link.active {
  font-weight: 600;
}
.category-tree-toggle {
  width: 32px;
  height: 32px;
  margin-right: 4px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 32px;
  border: 0;
  border-radius: 7px;
  color: var(--blog-color-text-muted);
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  transition: color 0.16s ease, background-color 0.16s ease;
}
.category-tree-toggle:hover {
  color: var(--blog-color-primary);
  background: rgba(51, 126, 204, 0.1);
}
.category-tree-toggle .el-icon {
  transition: transform 0.18s ease;
}
.category-tree-toggle .el-icon.expanded {
  transform: rotate(90deg);
}
.category-tree-children {
  margin: 3px 0 7px 14px;
  padding: 2px 0 2px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.category-tree-child {
  position: relative;
  padding: 7px 10px 7px 18px;
  border-radius: 7px;
  overflow: hidden;
  color: var(--blog-color-text-muted);
  font-size: 12px;
  line-height: 1.35;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
  transition: color 0.16s ease, background-color 0.16s ease;
}
.category-tree-child::before {
  position: absolute;
  left: 7px;
  top: 50%;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #c3cfdb;
  content: '';
  transform: translateY(-50%);
  transition: background-color 0.16s ease, transform 0.16s ease;
}
.category-tree-child:hover,
.category-tree-child.active {
  color: var(--blog-color-primary);
  background: var(--blog-color-primary-soft);
}
.category-tree-child:hover::before,
.category-tree-child.active::before {
  background: var(--blog-color-primary);
  transform: translateY(-50%) scale(1.25);
}
.category-tree-child.active {
  font-weight: 600;
}
.category-children-enter-active,
.category-children-leave-active {
  transition: opacity 0.16s ease, transform 0.16s ease;
}
.category-children-enter-from,
.category-children-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
.sidebar-title {
  margin: 0 0 var(--blog-space-4);
  padding-bottom: var(--blog-space-3);
  border-bottom: 1px solid var(--blog-color-border);
  color: var(--blog-color-text);
  font-size: 15px;
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
.tag-item.router-link-exact-active,
.tag-item.active {
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

@media (max-width: 1360px) {
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
  .blog-body {
    grid-template-columns: minmax(0, 1fr) 300px;
  }
  .blog-category-sidebar {
    display: none;
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
  .mobile-nav-item.router-link-exact-active,
  .mobile-nav-item.author-filter-active {
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
