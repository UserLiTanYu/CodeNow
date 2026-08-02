# Vue Router 与 Pinia 状态管理深入

现代单页应用的核心是路由和状态管理。Vue Router 4 基于 Vue 3 的组合式 API 重新设计，提供了更灵活的路由系统；Pinia 作为 Vuex 的继任者，以更简洁的 API 和更好的 TypeScript 支持成为官方推荐的状态管理方案。本文将深入探讨两者的高级用法和最佳实践。

## Vue Router 4 路由配置

### 嵌套路由

嵌套路由是构建复杂页面结构的基础：

```ts
import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/dashboard',
      component: DashboardLayout,
      children: [
        {
          path: '',           // 默认子路由
          name: 'DashboardHome',
          component: DashboardHome,
        },
        {
          path: 'analytics',
          name: 'Analytics',
          component: Analytics,
        },
        {
          path: 'settings',
          name: 'DashboardSettings',
          component: DashboardSettings,
          children: [
            {
              path: 'profile',
              name: 'Profile',
              component: Profile,
            },
            {
              path: 'security',
              name: 'Security',
              component: Security,
            },
          ],
        },
      ],
    },
  ],
})
```

父组件需要 `<router-view>` 来渲染子路由：

```vue
<!-- DashboardLayout.vue -->
<template>
  <div class="dashboard">
    <nav>
      <router-link to="/dashboard">概览</router-link>
      <router-link to="/dashboard/analytics">分析</router-link>
      <router-link to="/dashboard/settings">设置</router-link>
    </nav>
    <main>
      <router-view />
    </main>
  </div>
</template>
```

### 命名视图

当同一层级需要渲染多个组件时，使用命名视图：

```ts
const routes = [
  {
    path: '/layout',
    components: {
      default: MainContent,    // 默认视图
      sidebar: Sidebar,        // 命名视图
      header: HeaderNav,       // 命名视图
    },
  },
]
```

```vue
<template>
  <div class="layout">
    <router-view name="header" />
    <div class="body">
      <router-view name="sidebar" />
      <router-view />  <!-- default 视图 -->
    </div>
  </div>
</template>
```

### 重定向与别名

```ts
const routes = [
  // 重定向：访问 /old-page 时跳转到 /new-page
  {
    path: '/old-page',
    redirect: '/new-page',
  },
  // 函数式重定向：动态决定目标
  {
    path: '/legacy/:id',
    redirect: (to) => ({
      name: 'NewPage',
      params: { id: to.params.id },
    }),
  },
  // 别名：/home 是 / 的别名，URL 保持 /home 不变
  {
    path: '/',
    component: Home,
    alias: ['/home', '/index'],
  },
]
```

## 路由守卫

路由守卫用于在导航过程中执行逻辑，如权限检查、数据预加载等。

### 全局守卫

```ts
// 全局前置守卫
router.beforeEach((to, from) => {
  // 返回 false 取消导航
  // 返回路由地址（字符串或对象）进行重定向
  // 返回 undefined 或 true 放行

  const isAuthenticated = useAuthStore().isAuthenticated

  if (to.meta.requiresAuth && !isAuthenticated) {
    return {
      name: 'Login',
      query: { redirect: to.fullPath },
    }
  }
})

// 全局后置钩子
router.afterEach((to, from) => {
  // 更新页面标题
  document.title = to.meta.title
    ? `${to.meta.title} - MyApp`
    : 'MyApp'
})

// 全局解析守卫（在 beforeEach 之后、beforeRouteEnter 之前）
router.beforeResolve(async (to) => {
  // 确保异步组件已加载
})
```

### 路由独享守卫

```ts
const routes = [
  {
    path: '/admin',
    component: AdminLayout,
    beforeEnter: (to, from) => {
      const user = useAuthStore().user
      if (user?.role !== 'admin') {
        return { name: 'Forbidden' }
      }
    },
    children: [
      {
        path: 'users',
        component: UserManagement,
        beforeEnter: (to, from) => {
          // 只对这个路由生效
        },
      },
    ],
  },
]
```

### 组件内守卫

```vue
<script setup lang="ts">
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'

// 路由参数变化时调用（组件复用时）
onBeforeRouteUpdate(async (to, from) => {
  // 当 /user/:id 变化时，重新获取用户数据
  if (to.params.id !== from.params.id) {
    await fetchUser(to.params.id as string)
  }
})

// 离开当前路由前调用
onBeforeRouteLeave((to, from) => {
  // 表单未保存时提示用户
  if (hasUnsavedChanges.value) {
    const answer = window.confirm('有未保存的更改，确定要离开吗？')
    if (!answer) return false
  }
})
</script>
```

## 路由元信息（meta）

路由元信息用于存储路由的附加数据，如权限要求、页面标题、缓存策略等。

### 定义 meta 类型

```ts
// router.d.ts
import 'vue-router'

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth?: boolean
    roles?: string[]
    title?: string
    keepAlive?: boolean
    transition?: string
    layout?: string
  }
}
```

### 使用 meta

```ts
const routes = [
  {
    path: '/admin',
    component: AdminLayout,
    meta: {
      requiresAuth: true,
      roles: ['admin'],
      title: '管理后台',
    },
    children: [
      {
        path: 'articles',
        name: 'ArticleList',
        component: ArticleList,
        meta: {
          title: '文章管理',
          keepAlive: true,  // 需要缓存
        },
      },
      {
        path: 'articles/:id/edit',
        name: 'ArticleEdit',
        component: ArticleEdit,
        meta: {
          title: '编辑文章',
          keepAlive: false,  // 不缓存编辑页
        },
      },
    ],
  },
]
```

### 根据 meta 处理逻辑

```ts
// 权限检查
router.beforeEach((to) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  if (to.meta.roles && !to.meta.roles.includes(authStore.user?.role)) {
    return { name: 'Forbidden' }
  }
})

// 页面标题
router.afterEach((to) => {
  document.title = to.meta.title
    ? `${to.meta.title} - MyApp`
    : 'MyApp'
})

// 缓存策略（配合 KeepAlive）
const cachedViews = computed(() =>
  router.getRoutes()
    .filter(route => route.meta.keepAlive)
    .map(route => route.name)
    .filter(Boolean) as string[]
)
```

## 动态路由

动态路由允许在运行时动态添加或移除路由，常用于权限路由。

### addRoute 与 removeRoute

```ts
// 动态添加路由
router.addRoute({
  path: '/admin',
  name: 'Admin',
  component: AdminLayout,
  meta: { requiresAuth: true, roles: ['admin'] },
})

// 添加子路由
router.addRoute('Layout', {
  path: 'settings',
  name: 'Settings',
  component: Settings,
})

// 移除路由
router.removeRoute('Admin')

// 检查路由是否存在
router.hasRoute('Admin')
```

### 权限路由实现

```ts
// router/guard.ts
import type { RouteRecordRaw } from 'vue-router'

// 根据用户角色动态生成路由
function generateRoutes(roles: string[]): RouteRecordRaw[] {
  const adminRoutes: RouteRecordRaw[] = [
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      children: [
        {
          path: 'users',
          name: 'UserManagement',
          component: () => import('@/views/admin/Users.vue'),
          meta: { roles: ['admin'] },
        },
        {
          path: 'settings',
          name: 'SystemSettings',
          component: () => import('@/views/admin/Settings.vue'),
          meta: { roles: ['admin'] },
        },
      ],
    },
  ]

  const editorRoutes: RouteRecordRaw[] = [
    {
      path: '/editor',
      component: () => import('@/layouts/EditorLayout.vue'),
      children: [
        {
          path: 'articles',
          name: 'ArticleEditor',
          component: () => import('@/views/editor/Articles.vue'),
          meta: { roles: ['admin', 'editor'] },
        },
      ],
    },
  ]

  const routes: RouteRecordRaw[] = []

  if (roles.includes('admin')) {
    routes.push(...adminRoutes)
  }

  if (roles.includes('editor') || roles.includes('admin')) {
    routes.push(...editorRoutes)
  }

  return routes
}

// 在登录成功后调用
async function setupDynamicRoutes() {
  const authStore = useAuthStore()
  const roles = authStore.user?.roles || []

  const dynamicRoutes = generateRoutes(roles)

  dynamicRoutes.forEach(route => {
    router.addRoute(route)
  })

  // 标记路由已添加
  authStore.routesAdded = true
}
```

## 路由懒加载

### 基本懒加载

```ts
// 静态导入（打包在一个文件中）
import Home from '@/views/Home.vue'

// 懒加载（按需加载）
const Home = () => import('@/views/Home.vue')

// 带注释的懒加载（webpack 特有）
const Home = () => import(/* webpackChunkName: "home" */ '@/views/Home.vue')
```

### Webpack 魔法注释

```ts
const routes = [
  {
    path: '/',
    component: () => import(
      /* webpackChunkName: "home" */
      /* webpackPrefetch: true */
      '@/views/Home.vue'
    ),
  },
  {
    path: '/about',
    component: () => import(
      /* webpackChunkName: "about" */
      /* webpackPreload: true */
      '@/views/About.vue'
    ),
  },
]
```

- `webpackPrefetch`：浏览器空闲时预加载，优先级低
- `webpackPreload`：当前导航需要，优先级高

### Vite 中的懒加载

```ts
// Vite 原生支持 import()，无需魔法注释
const routes = [
  {
    path: '/',
    component: () => import('@/views/Home.vue'),
  },
]

// 使用 glob 批量导入
const viewModules = import.meta.glob('@/views/*.vue')

const routes = Object.entries(viewModules).map(([path, component]) => {
  const name = path.match(/\/views\/(.+)\.vue$/)?.[1]
  return {
    path: `/${name?.toLowerCase()}`,
    name,
    component,
  }
})
```

## 导航故障处理

Vue Router 4 提供了 `NavigationFailureType` 来处理导航失败：

```ts
import { NavigationFailureType, isNavigationFailure } from 'vue-router'

const result = await router.push('/some-page')

if (isNavigationFailure(result, NavigationFailureType.aborted)) {
  // 导航被守卫取消
  console.log('导航被取消')
} else if (isNavigationFailure(result, NavigationFailureType.duplicated)) {
  // 重复导航到同一位置
  console.log('已经在当前页面')
} else if (isNavigationFailure(result, NavigationFailureType.cancelled)) {
  // 导航被新的导航取消
  console.log('导航被取消（新的导航）')
}
```

## Pinia 核心概念

Pinia 是 Vue 的轻量级状态管理库，提供了一种简洁、类型安全的方式来管理全局状态。

### 创建 Store

```ts
// stores/counter.ts
import { defineStore } from 'pinia'

export const useCounterStore = defineStore('counter', {
  // State：存储状态
  state: () => ({
    count: 0,
    name: '计数器',
  }),

  // Getters：计算属性
  getters: {
    doubleCount: (state) => state.count * 2,
    displayName: (state) => `${state.name}: ${state.count}`,
  },

  // Actions：方法（同步和异步）
  actions: {
    increment() {
      this.count++
    },
    async fetchCount() {
      const response = await api.getCount()
      this.count = response.count
    },
  },
})
```

### 组合式 API 风格

```ts
// stores/counter.ts
import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

export const useCounterStore = defineStore('counter', () => {
  // State
  const count = ref(0)
  const name = ref('计数器')

  // Getters
  const doubleCount = computed(() => count.value * 2)
  const displayName = computed(() => `${name.value}: ${count.value}`)

  // Actions
  function increment() {
    count.value++
  }

  async function fetchCount() {
    const response = await api.getCount()
    count.value = response.count
  }

  return {
    count,
    name,
    doubleCount,
    displayName,
    increment,
    fetchCount,
  }
})
```

## Pinia vs Vuex 对比

| 特性 | Pinia | Vuex 4 |
|------|-------|--------|
| TypeScript 支持 | 原生支持，类型推导完善 | 需要额外类型声明 |
| Mutations | 无 mutations，直接修改 state | 必须通过 mutations |
| 模块化 | 天然模块化，每个 store 独立 | 嵌套模块，需要命名空间 |
| API 风格 | 支持 Options 和 Composition 两种风格 | 只支持 Options 风格 |
| 包体积 | ~1KB | ~10KB |
| DevTools | 完整支持 | 完整支持 |
| SSR | 原生支持 | 需要额外配置 |

### Vuex 4 的问题

```ts
// Vuex 4：冗长的代码
const store = useStore()

// 读取状态
const count = computed(() => store.state.counter.count)

// 修改状态
store.commit('counter/INCREMENT')
// 或者
store.dispatch('counter/incrementAsync')
```

### Pinia 的简洁

```ts
// Pinia：简洁直观
const counter = useCounterStore()

// 读取状态
const count = computed(() => counter.count)

// 修改状态
counter.increment()
// 或者直接修改
counter.count++
```

## Store 拆分策略

按功能模块划分 store，避免创建过大的单体 store：

```ts
// stores/auth.ts - 认证相关
export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(null)
  const isAuthenticated = computed(() => !!token.value)

  async function login(credentials: LoginCredentials) {
    const response = await api.login(credentials)
    user.value = response.user
    token.value = response.token
  }

  function logout() {
    user.value = null
    token.value = null
  }

  return { user, token, isAuthenticated, login, logout }
})

// stores/cart.ts - 购物车相关
export const useCartStore = defineStore('cart', () => {
  const items = ref<CartItem[]>([])

  const total = computed(() =>
    items.value.reduce((sum, item) => sum + item.price * item.quantity, 0)
  )

  function addItem(product: Product) {
    const existing = items.value.find(item => item.id === product.id)
    if (existing) {
      existing.quantity++
    } else {
      items.value.push({ ...product, quantity: 1 })
    }
  }

  function removeItem(productId: string) {
    items.value = items.value.filter(item => item.id !== productId)
  }

  return { items, total, addItem, removeItem }
})

// stores/ui.ts - UI 状态
export const useUIStore = defineStore('ui', () => {
  const sidebarCollapsed = ref(false)
  const theme = ref<'light' | 'dark'>('light')
  const loading = ref(false)

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setTheme(newTheme: 'light' | 'dark') {
    theme.value = newTheme
    document.documentElement.setAttribute('data-theme', newTheme)
  }

  return { sidebarCollapsed, theme, loading, toggleSidebar, setTheme }
})
```

### Store 之间的交互

```ts
// stores/user.ts
export const useUserStore = defineStore('user', () => {
  const authStore = useAuthStore()  // 使用其他 store

  const profile = ref<UserProfile | null>(null)

  async function fetchProfile() {
    if (!authStore.isAuthenticated) return
    profile.value = await api.getProfile(authStore.user!.id)
  }

  return { profile, fetchProfile }
})
```

## Pinia 插件

### 持久化插件

```bash
npm install pinia-plugin-persistedstate
```

```ts
// main.ts
import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)

app.use(pinia)
```

```ts
// stores/settings.ts
export const useSettingsStore = defineStore('settings', {
  state: () => ({
    theme: 'light' as 'light' | 'dark',
    language: 'zh-CN',
    fontSize: 14,
  }),
  persist: {
    key: 'app-settings',
    storage: localStorage,
    pick: ['theme', 'language'],  // 只持久化指定字段
  },
})
```

### 自定义日志插件

```ts
function loggerPlugin({ store }: { store: PiniaPluginContext }) {
  store.$onAction(({ name, args, after, onError }) => {
    const startTime = Date.now()
    console.log(`[${store.$id}] Action "${name}" 开始`, args)

    after((result) => {
      const duration = Date.now() - startTime
      console.log(`[${store.$id}] Action "${name}" 完成 (${duration}ms)`)
    })

    onError((error) => {
      console.error(`[${store.$id}] Action "${name}" 失败`, error)
    })
  })

  store.$subscribe((mutation, state) => {
    console.log(`[${store.$id}] State 变化:`, mutation.type, mutation.events)
  })
}

pinia.use(loggerPlugin)
```

## SSR 中的状态管理

### 服务端渲染

```ts
// stores/app.ts
export const useAppStore = defineStore('app', () => {
  const data = ref<AppData | null>(null)

  async function fetchData() {
    data.value = await api.getAppData()
  }

  return { data, fetchData }
})
```

```ts
// server.ts (SSR 入口)
import { renderToString } from 'vue/server-renderer'
import { createPinia } from 'pinia'

async function render(url: string) {
  const pinia = createPinia()
  const app = createApp(App)

  app.use(pinia)
  app.use(router)

  await router.push(url)
  await router.isReady()

  // 获取当前路由需要的 store 数据
  const appStore = useAppStore(pinia)
  await appStore.fetchData()

  const html = await renderToString(app, { pinia })

  // 将 pinia 状态序列化到 HTML 中
  const state = JSON.stringify(pinia.state.value)

  return { html, state }
}
```

### 客户端水合

```ts
// client.ts (客户端入口)
import { createPinia } from 'pinia'

const pinia = createPinia()

// 从服务端渲染的 HTML 中恢复状态
if (window.__INITIAL_STATE__) {
  pinia.state.value = JSON.parse(window.__INITIAL_STATE__)
}

app.use(pinia)
```

### $patch 批量更新

在 SSR 中，使用 `$patch` 批量更新状态可以减少序列化次数：

```ts
const store = useAppStore()

// 不推荐：多次触发序列化
store.name = '新名称'
store.count = 42
store.items = newItems

// 推荐：批量更新
store.$patch({
  name: '新名称',
  count: 42,
  items: newItems,
})

// 或者使用函数式 $patch
store.$patch((state) => {
  state.name = '新名称'
  state.count = 42
  state.items.push(newItem)
})
```

## 路由与状态管理的最佳实践

### 何时使用路由状态 vs Store 状态

| 场景 | 使用路由状态 | 使用 Store |
|------|------------|-----------|
| 页面标识（id） | ✅ URL 参数 | ❌ |
| 筛选条件 | ✅ 查询参数 | ❌ |
| 分页 | ✅ 查询参数 | ❌ |
| 用户信息 | ❌ | ✅ |
| 购物车 | ❌ | ✅ |
| 主题设置 | ❌ | ✅ |
| 弹窗开关 | ❌ | ✅ |
| 表单数据 | ❌ | ✅ |

### 路由参数与 Store 同步

```ts
// composables/useRouteSync.ts
import { useRoute, useRouter } from 'vue-router'

export function useRouteSync() {
  const route = useRoute()
  const router = useRouter()

  // 将路由参数同步到 store
  function syncFiltersToStore() {
    const store = useFilterStore()
    const query = route.query

    store.$patch({
      keyword: (query.keyword as string) || '',
      category: (query.category as string) || '',
      page: Number(query.page) || 1,
    })
  }

  // 将 store 状态同步到路由
  function syncStoreToRoute() {
    const store = useFilterStore()

    router.replace({
      query: {
        keyword: store.keyword || undefined,
        category: store.category || undefined,
        page: store.page > 1 ? String(store.page) : undefined,
      },
    })
  }

  // 监听路由变化
  watch(() => route.query, syncFiltersToStore, { immediate: true })

  // 监听 store 变化
  const store = useFilterStore()
  watch(
    () => ({ keyword: store.keyword, category: store.category, page: store.page }),
    syncStoreToRoute,
    { deep: true }
  )

  return { syncFiltersToStore, syncStoreToRoute }
}
```

### 路由级数据预加载

```ts
// router/index.ts
import { useArticleStore } from '@/stores/article'

const routes = [
  {
    path: '/articles/:id',
    name: 'ArticleDetail',
    component: ArticleDetail,
    meta: {
      // 定义需要预加载的 store action
      preload: async (to) => {
        const articleStore = useArticleStore()
        await articleStore.fetchArticle(to.params.id as string)
      },
    },
  },
]

// 全局守卫中执行预加载
router.beforeEach(async (to) => {
  if (to.meta.preload) {
    await to.meta.preload(to)
  }
})
```

### 避免常见陷阱

```ts
// 错误：在 store 外部直接解构会丢失响应性
const { count, name } = useCounterStore()  // ❌ 不是响应式的

// 正确：使用 storeToRefs
import { storeToRefs } from 'pinia'

const store = useCounterStore()
const { count, name } = storeToRefs(store)  // ✅ 保持响应性
const { increment } = store  // ✅ 方法可以解构

// 错误：在组件外部使用 store
export function useHelper() {
  const store = useCounterStore()  // ❌ 可能在错误的上下文中调用
}

// 正确：在函数内部使用
export function useHelper() {
  // 确保在组件 setup 或其他 store/composable 中调用
  const store = useCounterStore()
  return { store }
}
```

掌握 Vue Router 和 Pinia 的高级用法，能让你的 Vue 应用更加健壮和可维护。路由负责页面的组织和导航，Store 负责全局状态的管理，两者各司其职，共同构建出清晰的前端架构。