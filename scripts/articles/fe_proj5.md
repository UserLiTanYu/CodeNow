# 前端状态管理与路由设计

现代前端应用的复杂度越来越高，状态管理成为架构设计的核心问题。状态放错了位置，会导致组件耦合严重、数据流混乱、调试困难等问题。本文将探讨如何合理划分状态边界，以及如何设计可维护的路由系统。

## 状态分类与边界

### 三种状态类型

前端应用中的状态可以分为三类：

| 状态类型 | 存储位置 | 典型场景 | 示例 |
|----------|----------|----------|------|
| 全局状态 | Pinia | 用户信息、主题、全局配置 | 用户登录态、权限列表 |
| 局部状态 | 组件ref/reactive | 表单数据、临时UI状态 | 模态框开关、输入框内容 |
| URL状态 | 路由query/params | 筛选条件、分页、tab切换 | 搜索关键词、当前页码 |

### 状态归属判断

```typescript
// 判断一个状态应该放在哪里
function determineStateLocation(state: StateConfig): StateType {
  // 1. 多个组件共享？
  if (state.sharedAcrossComponents) {
    return 'pinia';
  }
  
  // 2. 需要持久化或URL可分享？
  if (state.needsPersistence || state.shouldBeShareable) {
    return 'url';
  }
  
  // 3. 仅当前组件使用？
  if (state.onlyUsedInCurrentComponent) {
    return 'local';
  }
  
  // 4. 跨页面保持？
  if (state.persistAcrossPages) {
    return 'pinia';
  }
  
  return 'local';
}
```

### URL状态的妙用

把筛选条件放到URL中，用户可以分享搜索结果、刷新后保持状态：

```typescript
// 不好的做法：状态只在内存中
const searchKeyword = ref('');
const currentPage = ref(1);

// 好的做法：状态同步到URL
const route = useRoute();
const router = useRouter();

const searchKeyword = computed({
  get: () => route.query.keyword as string || '',
  set: (value) => router.replace({ query: { ...route.query, keyword: value } })
});

const currentPage = computed({
  get: () => Number(route.query.page) || 1,
  set: (value) => router.replace({ query: { ...route.query, page: value } })
});

// 使用URL状态的Hook
export function useUrlState<T extends Record<string, any>>(defaults: T) {
  const route = useRoute();
  const router = useRouter();
  
  const state = computed<T>({
    get: () => {
      const result = { ...defaults };
      for (const key of Object.keys(defaults)) {
        const value = route.query[key];
        if (value !== undefined) {
          result[key] = parseValue(value, defaults[key]);
        }
      }
      return result;
    },
    set: (value) => {
      router.replace({ query: value as any });
    }
  });
  
  return state;
}
```

## Pinia模块化设计

### Store结构设计

```
src/stores/
├── index.ts              # Store导出
├── modules/
│   ├── user.ts           # 用户状态
│   ├── article.ts        # 文章状态
│   ├── app.ts            # 应用全局状态
│   ├── permission.ts     # 权限状态
│   └── dict.ts           # 字典数据
└── plugins/
    └── persist.ts        # 持久化插件
```

### 用户Store

```typescript
// stores/modules/user.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { login, logout, getUserInfo } from '@/api/auth';
import type { UserInfo, LoginForm } from '@/types';

export const useUserStore = defineStore('user', () => {
  // State
  const token = ref<string>(localStorage.getItem('token') || '');
  const userInfo = ref<UserInfo | null>(null);
  const roles = ref<string[]>([]);
  const permissions = ref<string[]>([]);
  
  // Getters
  const isLoggedIn = computed(() => !!token.value);
  const isAdmin = computed(() => roles.value.includes('ADMIN'));
  const username = computed(() => userInfo.value?.username || '');
  
  // Actions
  async function loginAction(loginForm: LoginForm) {
    const { data } = await login(loginForm);
    token.value = data.token;
    localStorage.setItem('token', data.token);
    await fetchUserInfo();
  }
  
  async function fetchUserInfo() {
    const { data } = await getUserInfo();
    userInfo.value = data;
    roles.value = data.roles;
    permissions.value = data.permissions;
  }
  
  async function logoutAction() {
    try {
      await logout();
    } finally {
      resetState();
    }
  }
  
  function resetState() {
    token.value = '';
    userInfo.value = null;
    roles.value = [];
    permissions.value = [];
    localStorage.removeItem('token');
  }
  
  function hasPermission(permission: string): boolean {
    if (isAdmin.value) return true;
    return permissions.value.includes(permission);
  }
  
  function hasRole(role: string): boolean {
    return roles.value.includes(role);
  }
  
  return {
    token,
    userInfo,
    roles,
    permissions,
    isLoggedIn,
    isAdmin,
    username,
    loginAction,
    fetchUserInfo,
    logoutAction,
    resetState,
    hasPermission,
    hasRole
  };
});
```

### 应用全局状态Store

```typescript
// stores/modules/app.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

export const useAppStore = defineStore('app', () => {
  // 侧边栏状态
  const sidebarCollapsed = ref(false);
  const sidebarWidth = computed(() => sidebarCollapsed.value ? 64 : 210);
  
  // 主题配置
  const theme = ref<'light' | 'dark'>(
    (localStorage.getItem('theme') as 'light' | 'dark') || 'light'
  );
  
  // 设备类型
  const device = ref<'desktop' | 'mobile'>('desktop');
  
  // 页面加载状态
  const pageLoading = ref(false);
  
  // 全局消息
  const notifications = ref<Notification[]>([]);
  
  // Actions
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }
  
  function setTheme(newTheme: 'light' | 'dark') {
    theme.value = newTheme;
    document.documentElement.setAttribute('data-theme', newTheme);
    localStorage.setItem('theme', newTheme);
  }
  
  function setDevice(newDevice: 'desktop' | 'mobile') {
    device.value = newDevice;
    if (newDevice === 'mobile') {
      sidebarCollapsed.value = true;
    }
  }
  
  function addNotification(notification: Notification) {
    notifications.value.unshift(notification);
    if (notifications.value.length > 50) {
      notifications.value.pop();
    }
  }
  
  return {
    sidebarCollapsed,
    sidebarWidth,
    theme,
    device,
    pageLoading,
    notifications,
    toggleSidebar,
    setTheme,
    setDevice,
    addNotification
  };
});
```

### Store持久化

```typescript
// stores/plugins/persist.ts
import type { PiniaPluginContext } from 'pinia';

export function piniaPersistPlugin(context: PiniaPluginContext) {
  const { store } = context;
  
  // 从localStorage恢复状态
  const savedState = localStorage.getItem(`pinia-${store.$id}`);
  if (savedState) {
    try {
      store.$patch(JSON.parse(savedState));
    } catch (e) {
      console.error(`Failed to restore state for store ${store.$id}`, e);
    }
  }
  
  // 监听状态变化，保存到localStorage
  store.$subscribe((mutation, state) => {
    localStorage.setItem(`pinia-${store.$id}`, JSON.stringify(state));
  });
}

// 使用
import { createPinia } from 'pinia';
import { piniaPersistPlugin } from './plugins/persist';

const pinia = createPinia();
pinia.use(piniaPersistPlugin);
```

## 路由守卫与权限控制

### 路由配置

```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

// 公共路由
export const constantRoutes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/404',
    component: () => import('@/views/error/404.vue'),
    meta: { hidden: true }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '仪表盘', icon: 'Dashboard', affix: true }
      }
    ]
  }
];

// 动态路由（根据权限加载）
export const asyncRoutes: RouteRecordRaw[] = [
  {
    path: '/article',
    component: () => import('@/layout/index.vue'),
    redirect: '/article/list',
    meta: { title: '文章管理', icon: 'Document', roles: ['ADMIN', 'AUTHOR'] },
    children: [
      {
        path: 'list',
        name: 'ArticleList',
        component: () => import('@/views/article/list.vue'),
        meta: { title: '文章列表', roles: ['ADMIN', 'AUTHOR'] }
      },
      {
        path: 'create',
        name: 'ArticleCreate',
        component: () => import('@/views/article/edit.vue'),
        meta: { title: '创建文章', roles: ['ADMIN', 'AUTHOR'] }
      },
      {
        path: 'edit/:id',
        name: 'ArticleEdit',
        component: () => import('@/views/article/edit.vue'),
        meta: { title: '编辑文章', roles: ['ADMIN', 'AUTHOR'], hidden: true }
      }
    ]
  },
  {
    path: '/system',
    component: () => import('@/layout/index.vue'),
    redirect: '/system/user',
    meta: { title: '系统管理', icon: 'Setting', roles: ['ADMIN'] },
    children: [
      {
        path: 'user',
        name: 'UserManagement',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', roles: ['ADMIN'] }
      },
      {
        path: 'role',
        name: 'RoleManagement',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理', roles: ['ADMIN'] }
      }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior: () => ({ top: 0 })
});

export default router;
```

### 路由守卫

```typescript
// router/guards.ts
import router from './index';
import { useUserStore } from '@/stores/modules/user';
import { usePermissionStore } from '@/stores/modules/permission';
import NProgress from 'nprogress';
import 'nprogress/nprogress.css';

NProgress.configure({ showSpinner: false });

const whiteList = ['/login', '/register', '/blog'];

router.beforeEach(async (to, from, next) => {
  NProgress.start();
  
  const userStore = useUserStore();
  const permissionStore = usePermissionStore();
  
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 码上记` : '码上记';
  
  if (userStore.isLoggedIn) {
    if (to.path === '/login') {
      // 已登录，跳转首页
      next({ path: '/' });
    } else {
      // 检查是否已获取用户信息
      if (userStore.roles.length === 0) {
        try {
          await userStore.fetchUserInfo();
          
          // 根据角色生成可访问路由
          const accessRoutes = await permissionStore.generateRoutes(userStore.roles);
          accessRoutes.forEach(route => {
            router.addRoute(route);
          });
          
          // 使用replace确保addRoute生效
          next({ ...to, replace: true });
        } catch (error) {
          // 获取信息失败，清除token重新登录
          await userStore.logoutAction();
          next(`/login?redirect=${to.path}`);
        }
      } else {
        // 检查路由权限
        if (hasRoutePermission(to, userStore.roles)) {
          next();
        } else {
          next('/404');
        }
      }
    }
  } else {
    // 未登录
    if (whiteList.includes(to.path)) {
      next();
    } else {
      next(`/login?redirect=${to.path}`);
    }
  }
});

router.afterEach(() => {
  NProgress.done();
});

function hasRoutePermission(to: any, roles: string[]): boolean {
  if (to.meta?.roles) {
    return roles.some(role => to.meta.roles.includes(role));
  }
  return true;
}
```

## 动态路由注入

### 权限Store

```typescript
// stores/modules/permission.ts
import { defineStore } from 'pinia';
import { ref } from 'vue';
import { constantRoutes, asyncRoutes } from '@/router';
import type { RouteRecordRaw } from 'vue-router';

export const usePermissionStore = defineStore('permission', () => {
  const routes = ref<RouteRecordRaw[]>([]);
  const addRoutes = ref<RouteRecordRaw[]>([]);
  
  function hasPermission(roles: string[], route: RouteRecordRaw): boolean {
    if (route.meta?.roles) {
      return roles.some(role => (route.meta!.roles as string[]).includes(role));
    }
    return true;
  }
  
  function filterAsyncRoutes(routes: RouteRecordRaw[], roles: string[]): RouteRecordRaw[] {
    const result: RouteRecordRaw[] = [];
    
    routes.forEach(route => {
      const tmp = { ...route };
      if (hasPermission(roles, tmp)) {
        if (tmp.children) {
          tmp.children = filterAsyncRoutes(tmp.children, roles);
        }
        result.push(tmp);
      }
    });
    
    return result;
  }
  
  async function generateRoutes(roles: string[]): Promise<RouteRecordRaw[]> {
    let accessedRoutes: RouteRecordRaw[];
    
    if (roles.includes('ADMIN')) {
      accessedRoutes = asyncRoutes;
    } else {
      accessedRoutes = filterAsyncRoutes(asyncRoutes, roles);
    }
    
    addRoutes.value = accessedRoutes;
    routes.value = constantRoutes.concat(accessedRoutes);
    
    return accessedRoutes;
  }
  
  function resetRoutes() {
    routes.value = [];
    addRoutes.value = [];
  }
  
  return {
    routes,
    addRoutes,
    generateRoutes,
    resetRoutes
  };
});
```

## 页面缓存策略

### KeepAlive配置

```vue
<!-- layout/index.vue -->
<template>
  <div class="app-wrapper">
    <sidebar />
    <div class="main-container">
      <navbar />
      <tags-view />
      <app-main />
    </div>
  </div>
</template>

<!-- app-main.vue -->
<template>
  <section class="app-main">
    <router-view v-slot="{ Component, route }">
      <transition name="fade-transform" mode="out-in">
        <keep-alive :include="cachedViews">
          <component :is="Component" :key="route.fullPath" />
        </keep-alive>
      </transition>
    </router-view>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useTagsViewStore } from '@/stores/modules/tagsView';

const tagsViewStore = useTagsViewStore();

const cachedViews = computed(() => tagsViewStore.cachedViews);
</script>
```

### TagsView缓存管理

```typescript
// stores/modules/tagsView.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { RouteLocationNormalized } from 'vue-router';

export interface TagView {
  name: string;
  path: string;
  fullPath: string;
  title: string;
  meta: any;
  query?: any;
  params?: any;
}

export const useTagsViewStore = defineStore('tagsView', () => {
  const visitedViews = ref<TagView[]>([]);
  const cachedViews = ref<string[]>([]);
  
  function addVisitedView(view: RouteLocationNormalized) {
    if (visitedViews.value.some(v => v.path === view.path)) {
      // 更新已存在的视图
      const index = visitedViews.value.findIndex(v => v.path === view.path);
      visitedViews.value[index] = { ...visitedViews.value[index], ...view };
      return;
    }
    
    visitedViews.value.push({
      name: view.name as string,
      path: view.path,
      fullPath: view.fullPath,
      title: view.meta?.title as string || 'no-name',
      meta: view.meta,
      query: view.query,
      params: view.params
    });
  }
  
  function addCachedView(view: RouteLocationNormalized) {
    const name = view.name as string;
    if (!name) return;
    
    if (cachedViews.value.includes(name)) {
      return;
    }
    
    // 只缓存设置了keepAlive的路由
    if (view.meta?.keepAlive !== false) {
      cachedViews.value.push(name);
    }
  }
  
  function delVisitedView(view: TagView) {
    const index = visitedViews.value.findIndex(v => v.path === view.path);
    if (index > -1) {
      visitedViews.value.splice(index, 1);
    }
  }
  
  function delCachedView(view: TagView) {
    const index = cachedViews.value.indexOf(view.name);
    if (index > -1) {
      cachedViews.value.splice(index, 1);
    }
  }
  
  function delOtherViews(view: TagView) {
    visitedViews.value = visitedViews.value.filter(v => {
      return v.meta?.affix || v.path === view.path;
    });
    
    cachedViews.value = cachedViews.value.filter(name => name === view.name);
  }
  
  function delAllViews() {
    visitedViews.value = visitedViews.value.filter(v => v.meta?.affix);
    cachedViews.value = [];
  }
  
  return {
    visitedViews,
    cachedViews,
    addVisitedView,
    addCachedView,
    delVisitedView,
    delCachedView,
    delOtherViews,
    delAllViews
  };
});
```

## 面包屑导航生成

```vue
<!-- components/Breadcrumb/index.vue -->
<template>
  <el-breadcrumb separator="/">
    <transition-group name="breadcrumb">
      <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="item.path">
        <span
          v-if="item.redirect === 'noRedirect' || index === breadcrumbs.length - 1"
          class="no-redirect"
        >
          {{ item.meta.title }}
        </span>
        <router-link v-else :to="item.redirect || item.path">
          {{ item.meta.title }}
        </router-link>
      </el-breadcrumb-item>
    </transition-group>
  </el-breadcrumb>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { RouteLocationMatched } from 'vue-router';

const route = useRoute();
const router = useRouter();
const breadcrumbs = ref<RouteLocationMatched[]>([]);

function getBreadcrumbs() {
  let matched = route.matched.filter(item => item.meta && item.meta.title);
  
  const first = matched[0];
  if (!isDashboard(first)) {
    matched = [
      { path: '/dashboard', meta: { title: '首页' } } as RouteLocationMatched,
      ...matched
    ];
  }
  
  breadcrumbs.value = matched.filter(item => {
    return item.meta && item.meta.title && item.meta.breadcrumb !== false;
  });
}

function isDashboard(route: RouteLocationMatched) {
  const name = route?.name as string;
  if (!name) return false;
  return name.trim().toLocaleLowerCase() === 'Dashboard'.toLocaleLowerCase();
}

watch(() => route.path, getBreadcrumbs, { immediate: true });
</script>

<style scoped>
.no-redirect {
  color: #97a8be;
  cursor: text;
}
</style>
```

状态管理和路由设计是前端架构的基石。好的状态管理应该让数据流清晰可追踪，好的路由设计应该让权限控制简单可靠。在实际项目中，需要根据团队规模和项目复杂度选择合适的方案，避免过度设计。
