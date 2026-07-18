import { createRouter, createWebHistory } from 'vue-router'
import { invalidateAuthSession } from '@/utils/authSession'

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

export function resetTokenVerification() {
  // Kept for callers/tests; restricted routes are intentionally revalidated.
}

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

function loginLocation(to) {
  return {
    name: 'login',
    query: to.fullPath && to.fullPath !== '/' ? { redirect: to.fullPath } : {},
  }
}

// 路由守卫：/blog 路由公开访问，其他路由需要登录
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
    // Keep the token for a temporary outage, but never open a role-restricted shell.
    return allowedRoles.length ? { path: '/blog' } : true
  }
}

router.beforeEach(authGuard)

export default router
