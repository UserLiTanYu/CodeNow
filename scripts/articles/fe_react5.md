# React 状态管理与性能优化

当应用复杂度上升，组件之间的状态共享和页面性能就成为绕不开的话题。React 生态提供了多种状态管理方案，从内置的 Context 到 Redux Toolkit、Zustand、Jotai，每种都有其适用场景。本文将从状态管理方案选型讲起，深入探讨 React 性能优化的核心手段。

## 何时需要全局状态

并非所有状态都需要全局管理。以下是一些判断标准：

**需要全局状态的场景：**
- 用户登录信息、权限数据
- 主题、语言等全局偏好设置
- 多个不相关组件共享的数据（如购物车、通知）
- 需要跨路由持久化的状态

**不需要全局状态的场景：**
- 表单输入状态（用 `useState` 或 React Hook Form）
- 模态框开关状态（提升到父组件即可）
- 组件内部的 UI 状态（loading、error）
- 只在一个组件树分支中使用的状态

经验法则：先用 `useState` + `props drilling`，当发现 props 穿透了三层以上且中间层不消费该数据时，再考虑提升到全局状态。

## React Context

### 基础用法

```jsx
import { createContext, useContext, useState } from 'react'

const ThemeContext = createContext(undefined)

function ThemeProvider({ children }) {
  const [theme, setTheme] = useState('light')

  function toggleTheme() {
    setTheme((prev) => (prev === 'light' ? 'dark' : 'light'))
  }

  return (
    <ThemeContext.Provider value={{ theme, toggleTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

function useTheme() {
  const context = useContext(ThemeContext)
  if (context === undefined) {
    throw new Error('useTheme must be used within ThemeProvider')
  }
  return context
}
```

### 性能问题：过度渲染

Context 的最大问题是当 value 变化时，所有消费该 Context 的组件都会重渲染，即使它们只使用了 value 的一部分：

```jsx
// 问题：theme 变化时，所有用到 AppContext 的组件都会重渲染
const AppContext = createContext({ theme: 'light', user: null, locale: 'zh' })
```

解决方案一：拆分 Context：

```jsx
const ThemeContext = createContext('light')
const UserContext = createContext(null)
const LocaleContext = createContext('zh')

function AppProviders({ children }) {
  return (
    <ThemeContext.Provider value={theme}>
      <UserContext.Provider value={user}>
        <LocaleContext.Provider value={locale}>
          {children}
        </LocaleContext.Provider>
      </UserContext.Provider>
    </ThemeContext.Provider>
  )
}
```

解决方案二：使用 `useMemo` 稳定 value 引用：

```jsx
function AuthProvider({ children }) {
  const [user, setUser] = useState(null)

  const value = useMemo(
    () => ({ user, setUser, isLoggedIn: !!user }),
    [user]
  )

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  )
}
```

## Redux Toolkit

Redux Toolkit（RTK）是 Redux 官方推荐的工具集，大幅简化了 Redux 的样板代码。

### 配置 Store

```bash
npm install @reduxjs/toolkit react-redux
```

```js
// store/index.js
import { configureStore } from '@reduxjs/toolkit'
import userReducer from './slices/userSlice'
import cartReducer from './slices/cartSlice'

export const store = configureStore({
  reducer: {
    user: userReducer,
    cart: cartReducer,
  },
})
```

### createSlice

```js
// store/slices/userSlice.js
import { createSlice, createAsyncThunk } from '@reduxjs/toolkit'
import { loginApi } from '@/api/auth'

export const login = createAsyncThunk(
  'user/login',
  async (credentials, { rejectWithValue }) => {
    try {
      const response = await loginApi(credentials)
      localStorage.setItem('token', response.token)
      return response.user
    } catch (error) {
      return rejectWithValue(error.response.data)
    }
  }
)

const userSlice = createSlice({
  name: 'user',
  initialState: {
    info: null,
    loading: false,
    error: null,
  },
  reducers: {
    logout(state) {
      state.info = null
      localStorage.removeItem('token')
    },
    clearError(state) {
      state.error = null
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(login.pending, (state) => {
        state.loading = true
        state.error = null
      })
      .addCase(login.fulfilled, (state, action) => {
        state.loading = false
        state.info = action.payload
      })
      .addCase(login.rejected, (state, action) => {
        state.loading = false
        state.error = action.payload
      })
  },
})

export const { logout, clearError } = userSlice.actions
export default userSlice.reducer
```

### 在组件中使用

```jsx
import { useSelector, useDispatch } from 'react-redux'
import { login, logout } from '@/store/slices/userSlice'

function LoginButton() {
  const dispatch = useDispatch()
  const { info, loading, error } = useSelector((state) => state.user)

  if (info) {
    return (
      <div>
        <span>欢迎, {info.name}</span>
        <button onClick={() => dispatch(logout())}>退出</button>
      </div>
    )
  }

  return (
    <button
      disabled={loading}
      onClick={() => dispatch(login({ username: 'admin', password: '123456' }))}
    >
      {loading ? '登录中...' : '登录'}
    </button>
  )
}
```

RTK 的 `createAsyncThunk` 自动处理 pending/fulfilled/rejected 三种状态，配合 `extraReducers` 更新 state，无需手动管理 loading 状态。

## Zustand

Zustand 是一个极简的状态管理库，API 简洁，没有 Provider 包裹，没有样板代码。

```bash
npm install zustand
```

### 基础用法

```js
// store/useUserStore.js
import { create } from 'zustand'

export const useUserStore = create((set, get) => ({
  user: null,
  loading: false,

  login: async (credentials) => {
    set({ loading: true })
    try {
      const user = await loginApi(credentials)
      set({ user, loading: false })
      return user
    } catch (error) {
      set({ loading: false })
      throw error
    }
  },

  logout: () => {
    set({ user: null })
    localStorage.removeItem('token')
  },

  // getter：从当前状态派生值
  get isLoggedIn() {
    return !!get().user
  },
}))
```

在组件中使用：

```jsx
function Header() {
  const user = useUserStore((state) => state.user)
  const logout = useUserStore((state) => state.logout)

  return (
    <header>
      {user ? (
        <>
          <span>{user.name}</span>
          <button onClick={logout}>退出</button>
        </>
      ) : (
        <span>未登录</span>
      )}
    </header>
  )
}
```

### 按选择器订阅

Zustand 的选择器函数确保只有选中的状态变化时才触发重渲染：

```jsx
// 只有 user.name 变化时才重渲染
const userName = useUserStore((state) => state.user?.name)
```

### 中间件

```js
import { create } from 'zustand'
import { persist, devtools } from 'zustand/middleware'

export const useSettingsStore = create(
  devtools(
    persist(
      (set) => ({
        theme: 'light',
        locale: 'zh',
        setTheme: (theme) => set({ theme }),
        setLocale: (locale) => set({ locale }),
      }),
      { name: 'settings-storage' } // 自动持久化到 localStorage
    )
  )
)
```

`persist` 中间件让状态自动同步到 localStorage，页面刷新后恢复。`devtools` 中间件支持 Redux DevTools 调试。

## Jotai

Jotai 采用原子化（atom）的状态管理模式，类似 Recoil 但更轻量。

```bash
npm install jotai
```

### 基础原子

```jsx
import { atom, useAtom, useAtomValue, useSetAtom } from 'jotai'

const countAtom = atom(0)
const doubledAtom = atom((get) => get(countAtom) * 2)

function Counter() {
  const [count, setCount] = useAtom(countAtom)
  const doubled = useAtomValue(doubledAtom)

  return (
    <div>
      <p>Count: {count}</p>
      <p>Doubled: {doubled}</p>
      <button onClick={() => setCount((c) => c + 1)}>+1</button>
    </div>
  )
}
```

### 异步原子

```jsx
const userAtom = atom(async () => {
  const response = await fetch('/api/user')
  return response.json()
})

function UserProfile() {
  const user = useAtomValue(userAtom) // 自动处理 loading
  return <div>{user.name}</div>
}
```

### 可写原子

```jsx
const todosAtom = atom([
  { id: 1, text: '学习 React', done: false },
])

const addTodoAtom = atom(null, (get, set, text) => {
  const todos = get(todosAtom)
  set(todosAtom, [...todos, { id: Date.now(), text, done: false }])
})

function AddTodo() {
  const addTodo = useSetAtom(addTodoAtom)
  return <button onClick={() => addTodo('新任务')}>添加</button>
}
```

## 状态管理方案选择

| 特性 | Context | Redux Toolkit | Zustand | Jotai |
|------|---------|---------------|---------|-------|
| 学习成本 | 低 | 中高 | 低 | 低 |
| 样板代码 | 少 | 较多 | 极少 | 极少 |
| 性能 | 差（过度渲染） | 好 | 好 | 好 |
| DevTools | 无 | 完善 | 支持 | 支持 |
| 持久化 | 手动 | 需中间件 | 内置 | 需插件 |
| 异步处理 | 手动 | createAsyncThunk | 手动 | 原子内 |
| 适用规模 | 小型 | 大型 | 中小型 | 中小型 |
| TypeScript | 原生 | 优秀 | 优秀 | 优秀 |

**选择建议：**
- 简单全局状态（主题、语言）→ Context
- 大型团队、复杂业务逻辑 → Redux Toolkit
- 中小型项目、追求简洁 → Zustand
- 状态间有复杂依赖关系 → Jotai

## React 性能优化

### React.memo

`React.memo` 对组件进行浅比较，props 不变时跳过重渲染：

```jsx
const UserCard = React.memo(function UserCard({ user, onEdit }) {
  console.log('UserCard render:', user.name)
  return (
    <div>
      <h3>{user.name}</h3>
      <p>{user.email}</p>
      <button onClick={() => onEdit(user)}>编辑</button>
    </div>
  )
})
```

注意：`memo` 只在组件渲染开销较大时使用。简单的组件加 `memo` 反而增加了比较的开销。

自定义比较函数：

```jsx
const UserCard = React.memo(
  UserCardComponent,
  (prevProps, nextProps) => {
    return prevProps.user.id === nextProps.user.id
      && prevProps.user.name === nextProps.user.name
  }
)
```

### useMemo

`useMemo` 缓存计算结果，避免每次渲染重复计算：

```jsx
function ProductList({ products, filter }) {
  const filteredProducts = useMemo(
    () => products.filter((p) => p.category === filter && p.price > 100),
    [products, filter]
  )

  const totalPrice = useMemo(
    () => filteredProducts.reduce((sum, p) => sum + p.price, 0),
    [filteredProducts]
  )

  return (
    <div>
      <p>筛选后总价: {totalPrice}</p>
      {filteredProducts.map((p) => <ProductCard key={p.id} product={p} />)}
    </div>
  )
}
```

### useCallback

`useCallback` 缓存函数引用，配合 `memo` 避免子组件重渲染：

```jsx
function TodoApp() {
  const [todos, setTodos] = useState([])

  // 不使用 useCallback：每次 TodoApp 渲染，handleToggle 都是新引用
  // 导致所有 TodoItem 重渲染
  const handleToggle = useCallback((id) => {
    setTodos((prev) =>
      prev.map((t) => (t.id === id ? { ...t, done: !t.done } : t))
    )
  }, [])

  const handleDelete = useCallback((id) => {
    setTodos((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return (
    <ul>
      {todos.map((todo) => (
        <TodoItem
          key={todo.id}
          todo={todo}
          onToggle={handleToggle}
          onDelete={handleDelete}
        />
      ))}
    </ul>
  )
}

const TodoItem = React.memo(function TodoItem({ todo, onToggle, onDelete }) {
  return (
    <li>
      <input
        type="checkbox"
        checked={todo.done}
        onChange={() => onToggle(todo.id)}
      />
      <span>{todo.text}</span>
      <button onClick={() => onDelete(todo.id)}>删除</button>
    </li>
  )
})
```

### 优化的原则

不要过早优化。先用 React DevTools Profiler 找到真正卡的组件，再针对性优化。常见的优化误区：

1. 到处加 `memo` → 大部分组件的 props 都是新引用，`memo` 形同虚设
2. 到处加 `useMemo` / `useCallback` → 简单计算的缓存开销大于重算开销
3. 拆分过细的组件 → 增加了组件数量和 props 传递的复杂度

## 虚拟列表

当列表数据量超过 1000 条时，DOM 节点过多会导致滚动卡顿。虚拟列表只渲染可视区域内的元素。

### react-window

```bash
npm install react-window
```

```jsx
import { FixedSizeList } from 'react-window'

function VirtualList({ items }) {
  const Row = ({ index, style }) => (
    <div style={style} className="list-row">
      <span>{items[index].name}</span>
      <span>{items[index].email}</span>
    </div>
  )

  return (
    <FixedSizeList
      height={600}
      width="100%"
      itemCount={items.length}
      itemSize={48}
    >
      {Row}
    </FixedSizeList>
  )
}
```

### react-virtuoso

react-virtuoso 支持动态高度、分组、表格等更复杂的场景：

```bash
npm install react-virtuoso
```

```jsx
import { Virtuoso } from 'react-virtuoso'

function DynamicList({ items }) {
  return (
    <Virtuoso
      style={{ height: 600 }}
      totalCount={items.length}
      itemContent={(index) => (
        <div className="list-item" style={{ padding: 16 }}>
          <h4>{items[index].title}</h4>
          <p>{items[index].content}</p>
        </div>
      )}
    />
  )
}
```

表格虚拟化：

```jsx
import { TableVirtuoso } from 'react-virtuoso'

function VirtualTable({ data }) {
  return (
    <TableVirtuoso
      style={{ height: 600 }}
      data={data}
      fixedHeaderContent={() => (
        <tr>
          <th>ID</th>
          <th>名称</th>
          <th>邮箱</th>
        </tr>
      )}
      itemContent={(index, row) => (
        <>
          <td>{row.id}</td>
          <td>{row.name}</td>
          <td>{row.email}</td>
        </>
      )}
    />
  )
}
```

## 代码分割

### React.lazy + Suspense

按路由分割代码，首屏只加载当前页面的代码：

```jsx
import { lazy, Suspense } from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'

const Home = lazy(() => import('./pages/Home'))
const Dashboard = lazy(() => import('./pages/Dashboard'))
const Settings = lazy(() => import('./pages/Settings'))

function App() {
  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoading />}>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/dashboard" element={<Dashboard />} />
          <Route path="/settings" element={<Settings />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  )
}
```

路由级分割是最有效的代码分割方式。一个大型管理后台可以将每个页面分割为独立 chunk，首屏加载时间可以从 3MB 降到 300KB。

### 预加载

在用户可能跳转时提前加载目标页面：

```jsx
const Dashboard = lazy(() => import('./pages/Dashboard'))

function NavLink({ to, children }) {
  function handleMouseEnter() {
    // 鼠标悬停时预加载
    import('./pages/Dashboard')
  }

  return (
    <Link to={to} onMouseEnter={handleMouseEnter}>
      {children}
    </Link>
  )
}
```

### 组件级分割

对于模态框、抽屉等按需展示的组件，也可以做代码分割：

```jsx
const HeavyChart = lazy(() => import('./components/HeavyChart'))

function ReportPage() {
  const [showChart, setShowChart] = useState(false)

  return (
    <div>
      <button onClick={() => setShowChart(true)}>查看图表</button>
      {showChart && (
        <Suspense fallback={<ChartLoading />}>
          <HeavyChart data={reportData} />
        </Suspense>
      )}
    </div>
  )
}
```

## React DevTools Profiler

React DevTools 的 Profiler 面板是性能分析的核心工具。

### 使用步骤

1. 安装 React DevTools 浏览器扩展
2. 打开 Profiler 面板
3. 点击录制，执行操作，停止录制
4. 查看火焰图（Flamegraph）或排名图（Ranked）

### 火焰图解读

火焰图中每个方块代表一个组件：
- 灰色：未重渲染
- 黄色/橙色：重渲染了，耗时较短
- 红色：重渲染了，耗时较长

点击方块可以看到：
- `Render duration`：渲染耗时
- `Why did this render?`：重渲染原因（需要在设置中开启）
- `Props` / `State` 变化

### 常见性能问题定位

1. **父组件变化导致子组件级联重渲染** → 给子组件加 `memo`，给回调加 `useCallback`
2. **Context value 变化导致所有消费者重渲染** → 拆分 Context 或使用 `useMemo`
3. **列表项没有 key 或 key 不稳定** → 使用唯一 ID 而非 index
4. **在渲染期间创建新对象/数组** → 用 `useMemo` 缓存

## Web Vitals

React 应用的性能最终体现在用户体验指标上。Core Web Vitals 是 Google 定义的三个关键指标：

| 指标 | 全称 | 含义 | 目标值 |
|------|------|------|--------|
| LCP | Largest Contentful Paint | 最大内容绘制时间 | < 2.5s |
| FID | First Input Delay | 首次输入延迟 | < 100ms |
| CLS | Cumulative Layout Shift | 累积布局偏移 | < 0.1 |

### LCP 优化

LCP 衡量的是首屏最大内容元素的渲染时间：

- 使用 `React.lazy` 做路由分割，减少首屏 JS 体积
- 关键 CSS 内联，避免阻塞渲染
- 图片使用 `loading="eager"` 和 `fetchpriority="high"`
- 服务端渲染（SSR）或静态生成（SSG）提前输出 HTML

```jsx
// 首屏关键图片
<img
  src="/hero.webp"
  loading="eager"
  fetchpriority="high"
  alt="Hero"
/>
```

### FID 优化

FID 衡量的是用户首次交互到浏览器响应的时间：

- 避免长任务阻塞主线程（单个任务不超过 50ms）
- 使用 `useTransition` 标记非紧急更新：

```jsx
const [isPending, startTransition] = useTransition()

function handleSearch(query) {
  // 紧急更新：输入框立即响应
  setInputValue(query)

  // 非紧急更新：搜索结果可以延迟
  startTransition(() => {
    setSearchResults(filterData(query))
  })
}
```

- 使用 `useDeferredValue` 延迟非关键渲染：

```jsx
function SearchResults({ query }) {
  const deferredQuery = useDeferredValue(query)
  const results = useMemo(() => filterData(deferredQuery), [deferredQuery])

  return <ResultList items={results} />
}
```

### CLS 优化

CLS 衡量的是页面加载过程中元素的意外移动：

- 为图片和视频预留空间：

```css
img, video {
  aspect-ratio: 16 / 9;
  width: 100%;
  object-fit: cover;
}
```

- 骨架屏占位：

```jsx
function UserCard({ user, loading }) {
  if (loading) {
    return (
      <div className="skeleton">
        <div className="skeleton-avatar" />
        <div className="skeleton-text" />
      </div>
    )
  }

  return (
    <div className="user-card">
      <img src={user.avatar} alt={user.name} />
      <h3>{user.name}</h3>
    </div>
  )
}
```

- 避免在已有内容上方动态插入元素（如广告、通知条）
- 使用 CSS `transform` 做动画，而非改变 `top`/`left`/`margin`

### 性能监控

使用 `web-vitals` 库采集指标：

```bash
npm install web-vitals
```

```js
import { onLCP, onFID, onCLS } from 'web-vitals'

function sendToAnalytics(metric) {
  console.log(metric.name, metric.value)
  // 发送到监控平台
  fetch('/api/metrics', {
    method: 'POST',
    body: JSON.stringify(metric),
  })
}

onLCP(sendToAnalytics)
onFID(sendToAnalytics)
onCLS(sendToAnalytics)
```

在 React 组件中使用：

```jsx
import { useReportWebVitals } from 'next/web-vitals'

function reportWebVitals(metric) {
  console.log(metric)
}

// Next.js 中直接使用
export default function App({ Component, pageProps }) {
  useReportWebVitals(reportWebVitals)
  return <Component {...pageProps} />
}
```

将 Web Vitals 数据接入监控平台（如 Sentry、阿里云 ARMS），可以持续追踪线上用户的实际体验，及时发现性能退化。
