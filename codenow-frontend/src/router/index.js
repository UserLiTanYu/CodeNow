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
  ],
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
