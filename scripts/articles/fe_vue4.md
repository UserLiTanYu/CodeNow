# Vue 3 组合式函数（Composables）与高级模式

Vue 3 的组合式 API 不仅是一种新的代码组织方式，更是一种全新的逻辑复用范式。组合式函数（Composables）让我们能够将有状态的逻辑提取为可复用的函数单元，彻底解决了 Mixins 带来的命名冲突、隐式依赖等问题。本文将深入探讨组合式函数的设计模式，以及 Vue 3 提供的高级功能。

## 组合式函数的概念

组合式函数是一个利用组合式 API 来封装和复用**有状态逻辑**的函数。它以 `use` 开头，内部可以使用响应式 API、生命周期钩子和其他组合式函数。

```ts
// 一个简单的组合式函数
function useCounter(initialValue = 0) {
  const count = ref(initialValue)

  function increment() {
    count.value++
  }

  function decrement() {
    count.value--
  }

  function reset() {
    count.value = initialValue
  }

  return {
    count: readonly(count),
    increment,
    decrement,
    reset,
  }
}
```

在组件中使用：

```vue
<script setup lang="ts">
const { count, increment, decrement, reset } = useCounter(10)
</script>

<template>
  <div>
    <p>计数：{{ count }}</p>
    <button @click="increment">+</button>
    <button @click="decrement">-</button>
    <button @click="reset">重置</button>
  </div>
</template>
```

## 组合式函数 vs Mixins

Mixins 是 Vue 2 中主要的逻辑复用方式，但它存在多个设计缺陷：

| 问题 | Mixins | 组合式函数 |
|------|--------|-----------|
| 命名冲突 | 多个 mixin 的同名属性会冲突 | 返回值解构，显式命名 |
| 隐式来源 | 不知道属性来自哪个 mixin | 明确的导入和解构 |
| 类型推导 | 类型支持差 | 完整的 TypeScript 类型推导 |
| 数据共享 | 隐式共享状态 | 通过闭包显式管理 |
| 复用灵活性 | 只能整体使用 | 可以选择性使用部分功能 |

### Mixins 的问题示例

```ts
// Vue 2 Mixin 方式——问题重重
const myMixin = {
  data() {
    return {
      count: 0,
      title: '默认标题',  // 可能与组件的 title 冲突
    }
  },
  methods: {
    increment() {
      this.count++  // this 来源不明确
    }
  }
}

export default {
  mixins: [myMixin],
  data() {
    return {
      title: '页面标题',  // 覆盖了 mixin 的 title？
    }
  }
}
```

### 组合式函数的优势

```ts
// Vue 3 组合式函数——清晰明确
function useCount() {
  const count = ref(0)
  const increment = () => count.value++
  return { count, increment }
}

function useTitle(defaultTitle: string) {
  const title = ref(defaultTitle)
  return { title }
}

// 组件中使用
const { count, increment } = useCount()
const { title } = useTitle('页面标题')
// 命名冲突？解构时重命名即可：
const { title: pageTitle } = useTitle('页面标题')
```

## 常用内置组合式 API

### ref 与 reactive

```ts
// ref：适用于原始值和需要替换整个对象的场景
const count = ref(0)
const user = ref({ name: '张三', age: 25 })

// reactive：适用于复杂对象，不需要替换引用
const form = reactive({
  username: '',
  password: '',
  remember: false,
})

// reactive 的局限性
let state = reactive({ count: 0 })
state = reactive({ count: 1 })  // 丢失响应性！ref 没有这个问题
```

### computed

```ts
const firstName = ref('张')
const lastName = ref('三')

// 只读 computed
const fullName = computed(() => `${firstName.value}${lastName.value}`)

// 可写 computed
const fullNameWritable = computed({
  get() => `${firstName.value}${lastName.value}`,
  set(newValue: string) {
    firstName.value = newValue[0]
    lastName.value = newValue.slice(1)
  },
})

// 带缓存的计算——依赖不变时不会重新计算
const expensiveResult = computed(() => {
  console.log('计算执行了')
  return heavyComputation(data.value)
})
```

### watch 与 watchEffect

```ts
const keyword = ref('')
const page = ref(1)

// watch：明确指定要监听的源
watch(keyword, (newVal, oldVal) => {
  console.log(`关键词从 "${oldVal}" 变为 "${newVal}"`)
  page.value = 1  // 关键词变化时重置页码
})

// 监听多个源
watch([keyword, page], ([newKeyword, newPage]) => {
  fetchData(newKeyword, newPage)
})

// watchEffect：自动追踪依赖
watchEffect(async () => {
  console.log(`搜索：${keyword.value}，第 ${page.value} 页`)
  const data = await fetchResults(keyword.value, page.value)
  results.value = data
})

// 带选项的 watch
watch(
  keyword,
  debounce((newVal) => {
    search(newVal)
  }, 300),
  { immediate: true }
)
```

### 生命周期钩子

```ts
function useLifecycle() {
  onMounted(() => {
    console.log('组件已挂载')
  })

  onUnmounted(() => {
    console.log('组件已卸载')
    // 清理定时器、取消订阅等
  })

  onUpdated(() => {
    console.log('组件已更新')
  })
}
```

## 自定义组合式函数

### useFetch：数据请求

```ts
interface UseFetchOptions<T> {
  immediate?: boolean
  initialData?: T
  onSuccess?: (data: T) => void
  onError?: (error: Error) => void
}

function useFetch<T>(url: MaybeRef<string>, options: UseFetchOptions<T> = {}) {
  const data = ref<T | undefined>(options.initialData) as Ref<T | undefined>
  const loading = ref(false)
  const error = ref<Error | null>(null)

  async function execute() {
    loading.value = true
    error.value = null

    try {
      const response = await fetch(toValue(url))
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`)
      }
      const result = await response.json()
      data.value = result
      options.onSuccess?.(result)
    } catch (e) {
      error.value = e as Error
      options.onError?.(e as Error)
    } finally {
      loading.value = false
    }
  }

  if (options.immediate !== false) {
    execute()
  }

  // 当 URL 是 ref 时，自动重新请求
  if (isRef(url)) {
    watch(url, execute)
  }

  return {
    data: readonly(data),
    loading: readonly(loading),
    error: readonly(error),
    execute,
  }
}

// 使用示例
const { data: users, loading, error, execute: refresh } = useFetch<User[]>('/api/users')
```

### useLocalStorage：本地存储持久化

```ts
function useLocalStorage<T>(key: string, defaultValue: T) {
  const stored = localStorage.getItem(key)
  const data = ref<T>(
    stored ? JSON.parse(stored) : defaultValue
  ) as Ref<T>

  watch(
    data,
    (newValue) => {
      localStorage.setItem(key, JSON.stringify(newValue))
    },
    { deep: true }
  )

  function remove() {
    localStorage.removeItem(key)
    data.value = defaultValue
  }

  return [data, remove] as const
}

// 使用示例
const [theme, removeTheme] = useLocalStorage('theme', 'light')
const [settings, clearSettings] = useLocalStorage('settings', {
  fontSize: 14,
  language: 'zh-CN',
})
```

## 组合式函数的命名约定与设计原则

### 命名约定

```ts
// 必须以 use 开头
function useAuth() { ... }
function useTheme() { ... }
function useWindowSize() { ... }

// 不要这样命名
function getAuth() { ... }    // 不是组合式函数命名
function createAuth() { ... } // 不是组合式函数命名
```

### 设计原则

```ts
// 1. 返回值使用 readonly，防止外部修改
function useCounter() {
  const count = ref(0)
  return {
    count: readonly(count),  // 外部只能读取
    increment: () => count.value++,
  }
}

// 2. 参数支持 ref 和普通值
function useDebouncedRef<T>(value: Ref<T>, delay: number) {
  // 使用 toValue 统一处理 ref 和普通值
  const debounced = ref(toValue(value))
  
  watch(value, debounce((val) => {
    debounced.value = val
  }, delay))

  return debounced
}

// 3. 处理副作用的清理
function useEventListener(
  target: Ref<EventTarget | null>,
  event: string,
  handler: (e: Event) => void
) {
  onMounted(() => {
    target.value?.addEventListener(event, handler)
  })

  onUnmounted(() => {
    target.value?.removeEventListener(event, handler)
  })
}
```

## 依赖注入：provide/inject 类型安全封装

```ts
// 创建类型安全的注入键
import { type InjectionKey, type Ref } from 'vue'

interface AuthContext {
  user: Ref<User | null>
  login: (credentials: LoginCredentials) => Promise<void>
  logout: () => void
}

export const authKey: InjectionKey<AuthContext> = Symbol('auth')

// Provider 组合式函数
export function provideAuth() {
  const user = ref<User | null>(null)

  async function login(credentials: LoginCredentials) {
    const response = await api.login(credentials)
    user.value = response.user
  }

  function logout() {
    user.value = null
  }

  const auth: AuthContext = {
    user: readonly(user),
    login,
    logout,
  }

  provide(authKey, auth)
  return auth
}

// Consumer 组合式函数
export function useAuth() {
  const auth = inject(authKey)
  if (!auth) {
    throw new Error('useAuth 必须在 provideAuth 的子组件中使用')
  }
  return auth
}

// 祖先组件
defineComponent({
  setup() {
    provideAuth()
  },
})

// 后代组件
defineComponent({
  setup() {
    const { user, login, logout } = useAuth()
  },
})
```

## 模板引用（Template Refs）

```ts
// 基本用法
function useTemplateRef<T extends HTMLElement>(refName: string) {
  const el = ref<T | null>(null)

  // 在组件挂载后才能访问
  onMounted(() => {
    console.log(`${refName} 元素：`, el.value)
  })

  return el
}

// 在组件中使用
const inputRef = useTemplateRef<HTMLInputElement>('input')
```

```vue
<script setup lang="ts">
const inputRef = ref<HTMLInputElement | null>(null)

onMounted(() => {
  inputRef.value?.focus()
})
</script>

<template>
  <input ref="inputRef" />
</template>
```

## 异步组件与 Suspense

### defineAsyncComponent

```ts
import { defineAsyncComponent } from 'vue'

// 基本用法
const AsyncComponent = defineAsyncComponent(() => import('./HeavyComponent.vue'))

// 带选项的异步组件
const AsyncComponentWithOptions = defineAsyncComponent({
  loader: () => import('./HeavyComponent.vue'),
  loadingComponent: LoadingSpinner,
  errorComponent: ErrorDisplay,
  delay: 200,        // 延迟显示 loading 组件（避免闪烁）
  timeout: 10000,    // 超时时间
  suspensible: false, // 不作为 Suspense 的异步依赖
})
```

### Suspense

```vue
<template>
  <Suspense>
    <template #default>
      <AsyncUserProfile />
    </template>
    <template #fallback>
      <div class="loading">加载中...</div>
    </template>
  </Suspense>
</template>
```

```ts
// AsyncUserProfile.vue 必须是一个异步组件（setup 中有 await）
export default defineComponent({
  async setup() {
    const user = await fetchUser()
    return { user }
  },
})
```

## Teleport 组件

Teleport 允许我们将组件的 DOM 渲染到指定的目标位置，非常适合模态框、通知等需要"跳出"当前组件层级的场景。

```vue
<template>
  <button @click="showModal = true">打开模态框</button>
  
  <Teleport to="body">
    <div v-if="showModal" class="modal-overlay" @click.self="showModal = false">
      <div class="modal-content">
        <h2>模态框标题</h2>
        <p>模态框内容</p>
        <button @click="showModal = false">关闭</button>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
const showModal = ref(false)
</script>
```

### 动态目标

```vue
<template>
  <Teleport :to="teleportTarget">
    <div class="notification">通知内容</div>
  </Teleport>
</template>

<script setup lang="ts">
const teleportTarget = ref('body')

// 可以动态切换目标
function changeTarget(selector: string) {
  teleportTarget.value = selector
}
</script>
```

## KeepAlive 缓存

KeepAlive 用于缓存不活动的组件实例，避免重复渲染。

```vue
<template>
  <KeepAlive :include="cachedViews" :max="10">
    <component :is="currentComponent" />
  </KeepAlive>
</template>

<script setup lang="ts">
const cachedViews = ref(['UserProfile', 'Settings'])
const currentComponent = shallowRef(UserProfile)
</script>
```

### 生命周期钩子

```ts
// 被 KeepAlive 缓存的组件特有的生命周期钩子
export default defineComponent({
  activated() {
    console.log('组件被激活（进入缓存的组件重新显示）')
    // 重新获取数据、恢复定时器等
  },
  deactivated() {
    console.log('组件被停用（进入缓存）')
    // 清理定时器、保存状态等
  },
})
```

### 配合路由使用

```vue
<template>
  <router-view v-slot="{ Component }">
    <KeepAlive :include="cachedRoutes">
      <component :is="Component" />
    </KeepAlive>
  </router-view>
</template>

<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()

// 通过路由元信息控制缓存
const cachedRoutes = computed(() => {
  return route.matched
    .filter(r => r.meta.keepAlive)
    .map(r => r.components?.default?.name)
    .filter(Boolean)
})
</script>
```

## 自定义指令

除了内置指令（v-if、v-for、v-model 等），Vue 3 允许我们创建自定义指令。

### 指令钩子函数

```ts
const vDirective = {
  // 元素插入 DOM 前
  created(el, binding, vnode, prevVnode) {},
  
  // 元素插入 DOM 后
  beforeMount(el, binding, vnode, prevVnode) {},
  
  // 父组件更新后
  mounted(el, binding, vnode, prevVnode) {},
  
  // 父组件更新前
  beforeUpdate(el, binding, vnode, prevVnode) {},
  
  // 父组件更新后
  updated(el, binding, vnode, prevVnode) {},
  
  // 元素卸载前
  beforeUnmount(el, binding, vnode, prevVnode) {},
  
  // 元素卸载后
  unmounted(el, binding, vnode, prevVnode) {},
}
```

### v-lazy：图片懒加载

```ts
const vLazy = {
  mounted(el: HTMLImageElement, binding: string) {
    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            el.src = binding
            el.classList.add('loaded')
            observer.disconnect()
          }
        })
      },
      { rootMargin: '100px' }
    )

    el.dataset.src = binding
    el.src = ''  // 初始不加载
    observer.observe(el)

    // 保存 observer 用于清理
    el._lazyObserver = observer
  },
  unmounted(el: HTMLImageElement) {
    el._lazyObserver?.disconnect()
  },
}
```

### v-permission：权限控制

```ts
const vPermission = {
  mounted(el: HTMLElement, binding: string[]) {
    const userPermissions = getUserPermissions()
    const requiredPermissions = binding.value

    const hasPermission = requiredPermissions.some(
      permission => userPermissions.includes(permission)
    )

    if (!hasPermission) {
      el.parentNode?.removeChild(el)
    }
  },
}
```

```vue
<template>
  <button v-permission="['admin', 'editor']">编辑</button>
  <button v-permission="['admin']">删除</button>
</template>
```

### v-click-outside：点击外部

```ts
const vClickOutside = {
  mounted(el: HTMLElement, binding: Function) {
    el._clickOutsideHandler = (event: MouseEvent) => {
      if (!el.contains(event.target as Node)) {
        binding.value(event)
      }
    }
    document.addEventListener('click', el._clickOutsideHandler)
  },
  unmounted(el: HTMLElement) {
    document.removeEventListener('click', el._clickOutsideHandler)
  },
}
```

## 插件系统

Vue 3 的插件通过 `app.use()` 安装，可以全局注册组件、指令、提供全局属性等。

### 插件的基本结构

```ts
import type { App, Plugin } from 'vue'

interface MyPluginOptions {
  baseUrl: string
  debug?: boolean
}

const MyPlugin: Plugin = {
  install(app: App, options: MyPluginOptions) {
    // 1. 注册全局组件
    app.component('MyButton', MyButton)

    // 2. 注册全局指令
    app.directive('click-outside', vClickOutside)

    // 3. 提供全局属性
    app.config.globalProperties.$api = createApi(options.baseUrl)

    // 4. 使用 provide/inject 提供全局依赖
    app.provide('baseUrl', options.baseUrl)

    // 5. 添加全局混入（谨慎使用）
    if (options.debug) {
      app.mixin({
        created() {
          console.log(`${this.$options.name || 'Component'} created`)
        },
      })
    }
  },
}

// 安装插件
app.use(MyPlugin, { baseUrl: '/api', debug: true })
```

### 类型声明扩展

```ts
// 扩展全局属性类型
declare module 'vue' {
  interface ComponentCustomProperties {
    $api: ApiInstance
  }
}

// 扩展组件选项
declare module 'vue' {
  interface ComponentCustomOptions {
    middleware?: string[]
  }
}
```

组合式函数和这些高级模式是 Vue 3 开发的核心技能。掌握它们不仅能提高代码复用性，还能让你的应用结构更加清晰、可维护。在实际开发中，应该根据具体场景选择合适的模式，避免过度设计。