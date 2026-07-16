import { createRouter, createWebHistory } from 'vue-router'

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

let verifiedToken = ''

export function resetTokenVerification() {
  verifiedToken = ''
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
  return result.code === 200
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
  if (to.path.startsWith('/blog')) {
    return true
  }
  if (to.path === '/login') {
    return true
  }
  if (!token) {
    resetTokenVerification()
    return loginLocation(to)
  }
  if (token === verifiedToken) {
    return true
  }

  try {
    if (await verifyToken(token)) {
      verifiedToken = token
      return true
    }

    localStorage.removeItem('token')
    resetTokenVerification()
    return loginLocation(to)
  } catch {
    // 网络或服务临时不可用时保留 Token；后续 API 请求仍由后端执行权限校验。
    return true
  }
}

router.beforeEach(authGuard)

export default router
