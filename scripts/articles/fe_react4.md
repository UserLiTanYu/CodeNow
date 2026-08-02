# React 表单处理与数据请求

表单和数据请求是前端开发中最核心的两个场景。React 生态在这方面有着丰富的工具链，从底层的受控组件模式到高层的 React Hook Form，从原生 fetch 到 TanStack Query，每一层都有其适用场景。本文将系统梳理 React 中表单处理与数据请求的最佳实践。

## 受控组件 vs 非受控组件

### 受控组件

表单值由 React state 控制，每次输入都触发重渲染：

```jsx
function ControlledForm() {
  const [form, setForm] = useState({ name: '', email: '' })

  function handleChange(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  function handleSubmit(e) {
    e.preventDefault()
    console.log('提交:', form)
  }

  return (
    <form onSubmit={handleSubmit}>
      <input
        value={form.name}
        onChange={(e) => handleChange('name', e.target.value)}
      />
      <input
        value={form.email}
        onChange={(e) => handleChange('email', e.target.value)}
      />
      <button type="submit">提交</button>
    </form>
  )
}
```

受控组件的优势是可以在每次输入时做校验、格式化、联动，但当表单字段很多时，每次按键都会触发整个表单重渲染。

### 非受控组件

表单值由 DOM 自身管理，通过 `useRef` 在需要时读取：

```jsx
function UncontrolledForm() {
  const nameRef = useRef()
  const emailRef = useRef()

  function handleSubmit(e) {
    e.preventDefault()
    console.log('提交:', {
      name: nameRef.current.value,
      email: emailRef.current.value,
    })
  }

  return (
    <form onSubmit={handleSubmit}>
      <input ref={nameRef} defaultValue="" />
      <input ref={emailRef} defaultValue="" />
      <button type="submit">提交</button>
    </form>
  )
}
```

非受控组件避免了频繁重渲染，适合简单表单或对性能敏感的场景。

### 选择建议

| 场景 | 推荐方案 |
|------|---------|
| 需要实时校验或联动 | 受控组件 |
| 字段少于 5 个 | 受控组件 |
| 字段多、性能敏感 | 非受控组件 |
| 文件上传 | 非受控组件 |
| 第三方表单库 | 库内部管理 |

## React Hook Form

React Hook Form 是 React 生态中最流行的表单库，基于非受控组件实现，性能优异。

### 基础用法

```bash
npm install react-hook-form
```

```jsx
import { useForm } from 'react-hook-form'

function UserForm() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    defaultValues: {
      name: '',
      email: '',
      age: '',
    },
  })

  function onSubmit(data) {
    console.log('表单数据:', data)
  }

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <div>
        <input
          {...register('name', { required: '姓名不能为空' })}
          placeholder="姓名"
        />
        {errors.name && <span>{errors.name.message}</span>}
      </div>

      <div>
        <input
          {...register('email', {
            required: '邮箱不能为空',
            pattern: {
              value: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
              message: '邮箱格式不正确',
            },
          })}
          placeholder="邮箱"
        />
        {errors.email && <span>{errors.email.message}</span>}
      </div>

      <div>
        <input
          {...register('age', {
            required: '年龄不能为空',
            min: { value: 0, message: '年龄不能为负数' },
            max: { value: 150, message: '年龄不能超过150' },
            valueAsNumber: true,
          })}
          placeholder="年龄"
          type="number"
        />
        {errors.age && <span>{errors.age.message}</span>}
      </div>

      <button type="submit">提交</button>
    </form>
  )
}
```

### 核心 API

`register` 函数返回 `ref`、`onChange`、`onBlur` 等属性，展开到输入框即可：

```jsx
const { register } = useForm()
// register('name') 返回 { name, ref, onChange, onBlur }
```

`handleSubmit` 接收两个参数：成功回调和错误回调：

```jsx
<form onSubmit={handleSubmit(onSuccess, onError)}>
```

`watch` 可以监听特定字段的值变化：

```jsx
const { watch } = useForm()
const nameValue = watch('name')
```

`setValue` 和 `getValues` 用于手动设置和读取值：

```jsx
const { setValue, getValues } = useForm()
setValue('name', '张三')
const allValues = getValues()
```

`reset` 重置表单：

```jsx
const { reset } = useForm()
reset({ name: '', email: '' })
```

## Zod 表单校验

Zod 是 TypeScript-first 的校验库，与 React Hook Form 配合使用非常流畅。

```bash
npm install zod @hookform/resolvers
```

### Schema 定义

```js
import { z } from 'zod'

const userSchema = z.object({
  name: z.string().min(2, '姓名至少2个字符').max(50, '姓名最多50个字符'),
  email: z.string().email('邮箱格式不正确'),
  age: z
    .number({ invalid_type_error: '请输入数字' })
    .min(0, '年龄不能为负数')
    .max(150, '年龄不能超过150'),
  password: z.string().min(8, '密码至少8个字符'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: '两次密码不一致',
  path: ['confirmPassword'],
})
```

### 与 React Hook Form 集成

```jsx
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { userSchema } from './schema'

function UserForm() {
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm({
    resolver: zodResolver(userSchema),
    defaultValues: {
      name: '',
      email: '',
      age: 0,
      password: '',
      confirmPassword: '',
    },
  })

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {/* 表单字段 */}
    </form>
  )
}
```

Zod 还可以用于 API 响应的运行时校验：

```js
const userResponseSchema = z.object({
  id: z.number(),
  name: z.string(),
  email: z.string().email(),
  createdAt: z.string().datetime(),
})

async function fetchUser(id) {
  const res = await fetch(`/api/users/${id}`)
  const data = await res.json()
  return userResponseSchema.parse(data) // 运行时校验
}
```

## 表单性能优化

### Controller 包装

对于第三方组件库（如 Ant Design、MUI），使用 `Controller` 包装：

```jsx
import { useForm, Controller } from 'react-hook-form'
import { Select, DatePicker } from 'antd'

function FormWithThirdParty() {
  const { control, handleSubmit } = useForm()

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <Controller
        name="role"
        control={control}
        rules={{ required: '请选择角色' }}
        render={({ field, fieldState }) => (
          <>
            <Select {...field} options={roleOptions} />
            {fieldState.error && <span>{fieldState.error.message}</span>}
          </>
        )}
      />

      <Controller
        name="birthday"
        control={control}
        render={({ field }) => (
          <DatePicker
            value={field.value}
            onChange={(date) => field.onChange(date)}
          />
        )}
      />
    </form>
  )
}
```

### 避免不必要的重渲染

React Hook Form 默认是非受控的，不会因为每次输入而重渲染整个表单。但如果使用了 `watch` 或在表单外层读取了 `formState`，会导致重渲染。

解决方案是使用 `useFormContext` 拆分组件，并用 `shouldUnregister: true` 减少内存占用：

```jsx
const { control } = useForm({ shouldUnregister: true })
```

对于大型表单，可以将每个字段拆分为独立组件，配合 `memo` 避免级联重渲染：

```jsx
const FormField = memo(function FormField({ name, control, label }) {
  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState }) => (
        <div>
          <label>{label}</label>
          <input {...field} />
          {fieldState.error && <span>{fieldState.error.message}</span>}
        </div>
      )}
    />
  )
})
```

## 数据请求：fetch API 封装

### 基础封装

```js
// utils/request.js
class HttpClient {
  constructor(baseURL = '') {
    this.baseURL = baseURL
  }

  async request(url, options = {}) {
    const fullURL = this.baseURL + url
    const config = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    }

    if (config.body && typeof config.body === 'object') {
      config.body = JSON.stringify(config.body)
    }

    const response = await fetch(fullURL, config)

    if (!response.ok) {
      const error = await response.json().catch(() => ({}))
      throw new HttpError(response.status, error.message || '请求失败', error)
    }

    return response.json()
  }

  get(url, options) {
    return this.request(url, { method: 'GET', ...options })
  }

  post(url, body, options) {
    return this.request(url, { method: 'POST', body, ...options })
  }

  put(url, body, options) {
    return this.request(url, { method: 'PUT', body, ...options })
  }

  delete(url, options) {
    return this.request(url, { method: 'DELETE', ...options })
  }
}

class HttpError extends Error {
  constructor(status, message, data) {
    super(message)
    this.status = status
    this.data = data
  }
}

export const http = new HttpClient('/api')
```

### 错误处理与 Loading 状态

在组件中使用时，通常需要管理 loading 和 error 状态：

```jsx
function useApi(apiFn) {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const execute = useCallback(
    async (...args) => {
      setLoading(true)
      setError(null)
      try {
        const result = await apiFn(...args)
        setData(result)
        return result
      } catch (err) {
        setError(err)
        throw err
      } finally {
        setLoading(false)
      }
    },
    [apiFn]
  )

  return { data, loading, error, execute }
}
```

## React Query（TanStack Query）

TanStack Query 是 React 生态中最成熟的服务端状态管理方案。

```bash
npm install @tanstack/react-query
```

### 基础配置

```jsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 分钟内数据视为新鲜
      retry: 1, // 失败重试 1 次
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <YourApp />
    </QueryClientProvider>
  )
}
```

### useQuery

```jsx
import { useQuery } from '@tanstack/react-query'

function UserList() {
  const { data, isLoading, error, refetch } = useQuery({
    queryKey: ['users'],
    queryFn: () => http.get('/users'),
  })

  if (isLoading) return <div>加载中...</div>
  if (error) return <div>出错了: {error.message}</div>

  return (
    <ul>
      {data.map((user) => (
        <li key={user.id}>{user.name}</li>
      ))}
    </ul>
  )
}
```

带参数的查询：

```jsx
function UserProfile({ userId }) {
  const { data } = useQuery({
    queryKey: ['user', userId],
    queryFn: () => http.get(`/users/${userId}`),
    enabled: !!userId, // 只在 userId 存在时请求
  })
}
```

### useMutation

```jsx
import { useMutation, useQueryClient } from '@tanstack/react-query'

function CreateUser() {
  const queryClient = useQueryClient()
  const mutation = useMutation({
    mutationFn: (userData) => http.post('/users', userData),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] })
      ElMessage.success('创建成功')
    },
    onError: (error) => {
      ElMessage.error(error.message)
    },
  })

  return (
    <button
      onClick={() => mutation.mutate({ name: '张三', email: 'zhangsan@example.com' })}
      disabled={mutation.isPending}
    >
      {mutation.isPending ? '创建中...' : '创建用户'}
    </button>
  )
}
```

### 缓存策略

TanStack Query 的缓存基于 `queryKey`，相同的 `queryKey` 共享缓存：

```jsx
// 自动缓存，5 分钟内重复请求不会发网络请求
useQuery({ queryKey: ['users'], queryFn: fetchUsers })

// 不同参数不同缓存
useQuery({ queryKey: ['user', 1], queryFn: () => fetchUser(1) })
useQuery({ queryKey: ['user', 2], queryFn: () => fetchUser(2) })
```

预取数据：

```jsx
const queryClient = useQueryClient()

function UserList() {
  return (
    <ul>
      {users.map((user) => (
        <li
          key={user.id}
          onMouseEnter={() => {
            queryClient.prefetchQuery({
              queryKey: ['user', user.id],
              queryFn: () => fetchUser(user.id),
            })
          }}
        >
          {user.name}
        </li>
      ))}
    </ul>
  )
}
```

## SWR

SWR 是 Vercel 团队维护的数据请求库，API 更简洁：

```bash
npm install swr
```

```jsx
import useSWR from 'swr'

const fetcher = (url) => fetch(url).then((res) => res.json())

function UserProfile({ id }) {
  const { data, error, isLoading, mutate } = useSWR(
    `/api/users/${id}`,
    fetcher
  )

  if (isLoading) return <Skeleton />
  if (error) return <Error message={error.message} />

  return (
    <div>
      <h1>{data.name}</h1>
      <button onClick={() => mutate()}>刷新</button>
    </div>
  )
}
```

SWR 的 `stale-while-revalidate` 策略：先返回缓存数据，同时在后台重新请求，请求完成后更新 UI。这与 TanStack Query 的理念类似，但 SWR 更轻量。

## 请求拦截与 Token 管理

### axios 拦截器

虽然 fetch 已经足够好用，但 axios 的拦截器在 Token 管理上更方便：

```bash
npm install axios
```

```js
// utils/http.js
import axios from 'axios'
import { refreshTokenApi } from '@/api/auth'

const http = axios.create({ baseURL: '/api', timeout: 10000 })

let isRefreshing = false
let failedQueue = []

function processQueue(error, token = null) {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error)
    } else {
      promise.resolve(token)
    }
  })
  failedQueue = []
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const originalRequest = error.config

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject })
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`
          return http(originalRequest)
        })
      }

      originalRequest._retry = true
      isRefreshing = true

      try {
        const { token } = await refreshTokenApi()
        localStorage.setItem('token', token)
        originalRequest.headers.Authorization = `Bearer ${token}`
        processQueue(null, token)
        return http(originalRequest)
      } catch (refreshError) {
        processQueue(refreshError)
        localStorage.removeItem('token')
        window.location.href = '/login'
        return Promise.reject(refreshError)
      } finally {
        isRefreshing = false
      }
    }

    return Promise.reject(error)
  }
)

export default http
```

这段代码实现了：请求时自动附加 Token，401 时自动刷新 Token，多个并发请求排队等待刷新完成。

## 乐观更新

乐观更新的核心思想是先更新 UI，再发送请求。如果请求失败，回滚 UI：

```jsx
import { useMutation, useQueryClient } from '@tanstack/react-query'

function TodoItem({ todo }) {
  const queryClient = useQueryClient()

  const toggleMutation = useMutation({
    mutationFn: () => http.patch(`/todos/${todo.id}`, { done: !todo.done }),

    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: ['todos'] })

      const previousTodos = queryClient.getQueryData(['todos'])

      queryClient.setQueryData(['todos'], (old) =>
        old.map((t) =>
          t.id === todo.id ? { ...t, done: !t.done } : t
        )
      )

      return { previousTodos }
    },

    onError: (err, variables, context) => {
      queryClient.setQueryData(['todos'], context.previousTodos)
      ElMessage.error('操作失败，已回滚')
    },

    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: ['todos'] })
    },
  })

  return (
    <li>
      <input
        type="checkbox"
        checked={todo.done}
        onChange={() => toggleMutation.mutate()}
      />
      {todo.text}
    </li>
  )
}
```

`onMutate` 中保存快照，`onError` 中回滚，`onSettled` 中重新同步服务端数据。

## 错误边界

React 的错误边界可以捕获子组件树中的渲染错误，展示降级 UI：

```jsx
import { Component } from 'react'

class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false, error: null }
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error }
  }

  componentDidCatch(error, errorInfo) {
    console.error('ErrorBoundary caught:', error, errorInfo)
    // 上报错误到监控平台
    reportError({ error, errorInfo })
  }

  render() {
    if (this.state.hasError) {
      return (
        this.props.fallback || (
          <div>
            <h2>出错了</h2>
            <p>{this.state.error?.message}</p>
            <button onClick={() => this.setState({ hasError: false })}>
              重试
            </button>
          </div>
        )
      )
    }
    return this.props.children
  }
}
```

在应用中合理使用错误边界：

```jsx
function App() {
  return (
    <ErrorBoundary fallback={<AppCrashFallback />}>
      <Header />
      <ErrorBoundary fallback={<PageErrorFallback />}>
        <Routes />
      </ErrorBoundary>
    </ErrorBoundary>
  )
}
```

不同层级使用不同的错误边界，避免一个组件的错误导致整个页面崩溃。

## Suspense 与数据加载

### React.lazy 代码分割

```jsx
import { lazy, Suspense } from 'react'

const AdminDashboard = lazy(() => import('./pages/AdminDashboard'))
const UserSettings = lazy(() => import('./pages/UserSettings'))

function App() {
  return (
    <Suspense fallback={<PageLoading />}>
      <Routes>
        <Route path="/admin" element={<AdminDashboard />} />
        <Route path="/settings" element={<UserSettings />} />
      </Routes>
    </Suspense>
  )
}
```

### Suspense 数据加载

React 18+ 的 Suspense 可以配合数据获取使用：

```jsx
import { Suspense, use } from 'react'

function UserPage({ userPromise }) {
  const user = use(userPromise) // 在 Suspense 边界内使用

  return <div>{user.name}</div>
}

function App() {
  const userPromise = useMemo(() => fetchUser(1), [])

  return (
    <Suspense fallback={<Loading />}>
      <UserPage userPromise={userPromise} />
    </Suspense>
  )
}
```

TanStack Query 也支持 Suspense 模式：

```jsx
function UserList() {
  const { data } = useSuspenseQuery({
    queryKey: ['users'],
    queryFn: fetchUsers,
  })

  return <ul>{data.map((u) => <li key={u.id}>{u.name}</li>)}</ul>
}

function App() {
  return (
    <Suspense fallback={<Loading />}>
      <UserList />
    </Suspense>
  )
}
```

Suspense 模式让组件代码更简洁，不需要手动处理 loading 状态，由外层的 Suspense 统一管理。
