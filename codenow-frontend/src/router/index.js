/**
 * 路由配置模块
 * 定义应用的完整路由表，包括管理后台、作者控制台和博客前台三大模块
 * 路由 meta 只负责前端导航体验；真正的身份与角色授权仍由后端 Sa-Token 拦截器执行
 */
import { createRouter, createWebHistory } from 'vue-router'
import { invalidateAuthSession } from '@/utils/authSession'

/**
 * Vue Router 实例
 * 使用 HTML5 History 模式，配置各模块路由和权限守卫
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      meta: { title: '登录' },
      component: () => import('@/views/login/LoginView.vue'),
    },
    {
      path: '/',
      component: () => import('@/layout/MainLayout.vue'),
      redirect: '/articles',
      meta: { requiresAdmin: true },
      children: [
        {
          path: 'articles',
          name: 'articles',
          meta: { title: '文章管理' },
          component: () => import('@/views/article/ArticleList.vue'),
        },
        {
          path: 'articles/edit/:id?',
          name: 'article-edit',
          meta: { title: '编辑文章' },
          component: () => import('@/views/article/ArticleEdit.vue'),
        },
        {
          path: 'categories',
          name: 'categories',
          meta: { title: '分类管理' },
          component: () => import('@/views/category/CategoryList.vue'),
        },
        {
          path: 'tags',
          name: 'tags',
          meta: { title: '标签管理' },
          component: () => import('@/views/tag/TagList.vue'),
        },
        {
          path: 'comments',
          name: 'comments',
          meta: { title: '评论管理' },
          component: () => import('@/views/comment/CommentList.vue'),
        },
        {
          path: 'logs',
          name: 'logs',
          meta: { title: '操作日志' },
          component: () => import('@/views/log/LogList.vue'),
        },
        {
          path: 'users',
          name: 'users',
          meta: { title: '用户管理' },
          component: () => import('@/views/user/UserList.vue'),
        },
        {
          path: 'author-applications',
          name: 'admin-author-applications',
          meta: { title: '作者申请' },
          component: () => import('@/views/author/AdminAuthorApplications.vue'),
        },
        {
          path: 'login-logs',
          name: 'login-logs',
          meta: { title: '登录日志' },
          component: () => import('@/views/user/LoginLogList.vue'),
        },
      ],
    },
    {
      path: '/author-console',
      component: () => import('@/layout/AuthorLayout.vue'),
      redirect: '/author-console/articles',
      meta: { allowedRoles: ['AUTHOR', 'ADMIN'] },
      children: [
        {
          path: 'articles',
          name: 'author-articles',
          meta: { title: '我的文章' },
          component: () => import('@/views/author/AuthorArticleList.vue'),
        },
        {
          path: 'articles/edit/:id?',
          name: 'author-article-edit',
          meta: { title: '编辑文章' },
          component: () => import('@/views/author/AuthorArticleEdit.vue'),
        },
        {
          path: 'comments',
          name: 'author-comments',
          meta: { title: '文章评论' },
          component: () => import('@/views/author/AuthorCommentList.vue'),
        },
        {
          path: 'categories',
          name: 'author-categories',
          meta: { title: '分类管理' },
          component: () => import('@/views/author/AuthorCategoryList.vue'),
        },
        {
          path: 'tags',
          name: 'author-tags',
          meta: { title: '标签管理' },
          component: () => import('@/views/author/AuthorTagList.vue'),
        },
      ],
    },
    {
      path: '/blog',
      component: () => import('@/layout/BlogLayout.vue'),
      children: [
        {
          path: '',
          name: 'blog-home',
          meta: { title: '博客首页' },
          component: () => import('@/views/blog/BlogHome.vue'),
        },
        {
          path: 'article/:id',
          name: 'blog-article',
          meta: { title: '文章详情' },
          component: () => import('@/views/blog/BlogArticle.vue'),
        },
        {
          path: 'category/:id',
          name: 'blog-category',
          meta: { title: '分类文章' },
          component: () => import('@/views/blog/BlogCategory.vue'),
        },
        {
          path: 'tag/:id',
          name: 'blog-tag',
          meta: { title: '标签文章' },
          component: () => import('@/views/blog/BlogTag.vue'),
        },
        {
          path: 'authors',
          name: 'blog-authors',
          meta: { title: '作者发现' },
          component: () => import('@/views/blog/BlogAuthors.vue'),
        },
        {
          path: 'author/:id',
          name: 'blog-author',
          meta: { title: '作者主页' },
          component: () => import('@/views/blog/BlogAuthor.vue'),
        },
        {
          path: 'profile',
          name: 'member-profile',
          meta: { title: '个人中心', requiresAuth: true },
          component: () => import('@/views/member/MemberProfile.vue'),
        },
        {
          path: 'favorites',
          name: 'member-favorites',
          meta: { title: '我的收藏', requiresAuth: true },
          component: () => import('@/views/member/MemberFavorites.vue'),
        },
        {
          path: 'comments',
          name: 'member-comments',
          meta: { title: '我的评论', requiresAuth: true },
          component: () => import('@/views/member/MemberComments.vue'),
        },
        {
          path: 'author-application',
          name: 'author-application',
          meta: { title: '作者身份', requiresAuth: true },
          component: () => import('@/views/author/AuthorApplication.vue'),
        },
        {
          path: 'notifications',
          name: 'member-notifications',
          meta: { title: '消息中心', requiresAuth: true },
          component: () => import('@/views/member/MemberNotifications.vue'),
        },
        {
          path: 'terms',
          name: 'terms',
          meta: { title: '用户协议' },
          component: () => import('@/views/blog/PolicyView.vue'),
        },
        {
          path: 'privacy',
          name: 'privacy',
          meta: { title: '隐私政策' },
          component: () => import('@/views/blog/PolicyView.vue'),
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      redirect: '/blog',
    },
  ],
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '页面'} - 码上记`
})

/**
 * 重置 Token 验证状态
 * 保留给调用者和测试使用；受限路由会被故意重新验证
 * @returns {void}
 */
export function resetTokenVerification() {
  // Kept for callers/tests; restricted routes are intentionally revalidated.
}

/**
 * 向后端验证 Token 有效性
 * @param {string} token - 用户认证 Token
 * @returns {Promise<Object|false>} 有效时返回用户数据，无效返回 false
 */
export async function verifyToken(token) {
  const response = await fetch('/api/auth/me', {
    headers: { Authorization: token },
  })

  if (response.status === 401 || response.status === 403) {
    return false
  }
  if (!response.ok) {
    throw new Error(`Token verification failed with HTTP ${response.status}`)
  }

  const result = await response.json()
  return result.code === 200 ? result.data : false
}

/**
 * 构建登录页跳转位置，携带当前路径作为 redirect 参数
 * @param {Object} to - 目标路由对象
 * @returns {Object} 登录路由位置对象，包含 name 和 query
 */
function loginLocation(to) {
  return {
    name: 'login',
    query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : {},
  }
}

/**
 * 路由全局前置守卫
 * 受限路由每次访问都向 /auth/me 读取最新角色：显式 allowedRoles 优先，否则后台默认 ADMIN
 * 401/403 清除会话；网络或 5xx 保留 Token，但角色受限页面失败关闭
 * 这里只控制导航，后端才是授权边界
 * @param {Object} to - 目标路由对象
 * @returns {Promise<boolean|Object>} true 放行，对象则跳转到指定路由
 */
export async function authGuard(to) {
  const token = localStorage.getItem('token')
  const requiresAuth = Boolean(to.meta?.requiresAuth)
  const allowedRoles = Array.isArray(to.meta?.allowedRoles)
    ? to.meta.allowedRoles.map((role) => role.toUpperCase())
    : Boolean(to.meta?.requiresAdmin) || (!to.path.startsWith('/blog') && to.path !== '/login')
      ? ['ADMIN']
      : []
  if (to.path.startsWith('/blog') && !requiresAuth) {
    return true
  }
  if (to.path === '/login') {
    return true
  }
  if (!token) {
    resetTokenVerification()
    return loginLocation(to)
  }
  try {
    const user = await verifyToken(token)
    if (user) {
      const role = user.role?.toUpperCase() || ''
      if (allowedRoles.length && !allowedRoles.includes(role)) return { path: '/blog' }
      return true
    }

    invalidateAuthSession()
    resetTokenVerification()
    return loginLocation(to)
  } catch {
    // 临时故障不等于 Token 失效；仅登录页面可暂时放行，角色受限外壳绝不失败开放。
    return allowedRoles.length ? { path: '/blog' } : true
  }
}

router.beforeEach(authGuard)

export default router
