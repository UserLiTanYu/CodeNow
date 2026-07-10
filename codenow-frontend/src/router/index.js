import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
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
          component: () => import('@/views/article/ArticleList.vue'),
        },
        {
          path: 'articles/edit/:id?',
          name: 'article-edit',
          component: () => import('@/views/article/ArticleEdit.vue'),
        },
        {
          path: 'categories',
          name: 'categories',
          component: () => import('@/views/category/CategoryList.vue'),
        },
        {
          path: 'tags',
          name: 'tags',
          component: () => import('@/views/tag/TagList.vue'),
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
          component: () => import('@/views/blog/BlogHome.vue'),
        },
        {
          path: 'article/:id',
          name: 'blog-article',
          component: () => import('@/views/blog/BlogArticle.vue'),
        },
        {
          path: 'category/:id',
          name: 'blog-category',
          component: () => import('@/views/blog/BlogCategory.vue'),
        },
        {
          path: 'tag/:id',
          name: 'blog-tag',
          component: () => import('@/views/blog/BlogTag.vue'),
        },
      ],
    },
  ],
})

// 路由守卫：/blog 路由公开访问，其他路由需要登录
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path.startsWith('/blog')) {
    next()
  } else if (to.path === '/login') {
    next()
  } else if (!token) {
    next('/login')
  } else {
    next()
  }
})

export default router
