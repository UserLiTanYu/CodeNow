# 前端接口对接与错误处理

前端与后端的交互是应用开发中最常见的工作。一个健壮的接口封装层能让开发效率大幅提升，同时提供良好的用户体验。本文将介绍如何封装Axios、统一管理Loading状态、处理错误码映射以及实现Token刷新机制。

## Axios封装

### 基础配置

```typescript
// utils/request.ts
import axios from 'axios';
import type { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { ElMessage, ElMessageBox } from 'element-plus';
import { useUserStore } from '@/stores/modules/user';
import router from '@/router';

// 创建axios实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json'
  }
});

export default service;
```

### 请求拦截器

```typescript
// 请求拦截器
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore();
    
    // 添加Token
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`;
    }
    
    // 添加请求ID（用于链路追踪）
    config.headers['X-Request-Id'] = generateRequestId();
    
    // GET请求添加时间戳防止缓存
    if (config.method === 'get') {
      config.params = {
        ...config.params,
        _t: Date.now()
      };
    }
    
    return config;
  },
  (error) => {
    console.error('请求拦截器错误:', error);
    return Promise.reject(error);
  }
);

function generateRequestId(): string {
  return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
}
```

### 响应拦截器

```typescript
// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data;
    
    // 文件下载等直接返回
    if (response.config.responseType === 'blob') {
      return response;
    }
    
    // 业务状态码判断
    if (res.code === 200 || res.code === 0) {
      return res;
    }
    
    // 业务错误处理
    handleBusinessError(res);
    return Promise.reject(new Error(res.message || '请求失败'));
  },
  (error) => {
    return handleHttpError(error);
  }
);

// HTTP错误处理
function handleHttpError(error: any): Promise<never> {
  let message = '网络异常，请稍后重试';
  
  if (error.response) {
    const { status, data } = error.response;
    
    switch (status) {
      case 400:
        message = data?.message || '请求参数错误';
        break;
      case 401:
        message = '登录已过期，请重新登录';
        handleUnauthorized();
        break;
      case 403:
        message = '没有权限访问该资源';
        break;
      case 404:
        message = '请求的资源不存在';
        break;
      case 408:
        message = '请求超时，请稍后重试';
        break;
      case 429:
        message = '请求过于频繁，请稍后重试';
        break;
      case 500:
        message = '服务器内部错误';
        break;
      case 502:
        message = '网关错误';
        break;
      case 503:
        message = '服务暂不可用';
        break;
      default:
        message = data?.message || `请求失败(${status})`;
    }
  } else if (error.code === 'ECONNABORTED') {
    message = '请求超时，请稍后重试';
  } else if (error.message === 'Network Error') {
    message = '网络连接失败，请检查网络';
  }
  
  // 取消请求不提示
  if (!axios.isCancel(error)) {
    ElMessage.error(message);
  }
  
  return Promise.reject(error);
}

// 业务错误处理
function handleBusinessError(res: any) {
  const { code, message } = res;
  
  switch (code) {
    case 40001: // Token过期
      handleUnauthorized();
      break;
    case 40003: // 无权限
      ElMessageBox.confirm('您的权限不足，无法执行此操作', '权限提示', {
        confirmButtonText: '我知道了',
        showCancelButton: false,
        type: 'warning'
      });
      break;
    default:
      ElMessage.error(message || '操作失败');
  }
}

// 处理未授权
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function handleUnauthorized() {
  if (isRefreshing) {
    return;
  }
  
  ElMessageBox.confirm('登录已过期，请重新登录', '提示', {
    confirmButtonText: '重新登录',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const userStore = useUserStore();
    userStore.resetState();
    router.push(`/login?redirect=${router.currentRoute.value.fullPath}`);
  });
}
```

## 请求重试机制

```typescript
// utils/retry.ts
import axios from 'axios';
import type { AxiosRequestConfig, AxiosResponse } from 'axios';

interface RetryConfig extends AxiosRequestConfig {
  retry?: number;
  retryDelay?: number;
  retryCount?: number;
}

export async function requestWithRetry(config: RetryConfig): Promise<AxiosResponse> {
  const { retry = 3, retryDelay = 1000, ...axiosConfig } = config;
  let retryCount = 0;
  
  const doRequest = async (): Promise<AxiosResponse> => {
    try {
      return await axios(axiosConfig);
    } catch (error: any) {
      retryCount++;
      
      // 判断是否需要重试
      if (
        retryCount < retry &&
        shouldRetry(error) &&
        !axios.isCancel(error)
      ) {
        console.log(`请求重试 ${retryCount}/${retry}: ${axiosConfig.url}`);
        
        // 指数退避
        const delay = retryDelay * Math.pow(2, retryCount - 1);
        await sleep(delay);
        
        return doRequest();
      }
      
      throw error;
    }
  };
  
  return doRequest();
}

function shouldRetry(error: any): boolean {
  // 网络错误或5xx错误才重试
  if (!error.response) {
    return true;
  }
  
  const { status } = error.response;
  return status >= 500 || status === 408 || status === 429;
}

function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms));
}

// 使用示例
const response = await requestWithRetry({
  url: '/api/articles',
  method: 'get',
  retry: 3,
  retryDelay: 1000
});
```

## 取消请求

```typescript
// utils/cancelToken.ts
import axios, { AxiosRequestConfig } from 'axios';

const pendingRequests = new Map<string, AbortController>();

export function getRequestKey(config: AxiosRequestConfig): string {
  const { method, url, params, data } = config;
  return `${method}-${url}-${JSON.stringify(params)}-${JSON.stringify(data)}`;
}

export function addPendingRequest(config: AxiosRequestConfig): void {
  const key = getRequestKey(config);
  
  if (pendingRequests.has(key)) {
    // 取消之前的重复请求
    pendingRequests.get(key)?.abort();
  }
  
  const controller = new AbortController();
  config.signal = controller.signal;
  pendingRequests.set(key, controller);
}

export function removePendingRequest(config: AxiosRequestConfig): void {
  const key = getRequestKey(config);
  pendingRequests.delete(key);
}

export function cancelAllPendingRequests(): void {
  pendingRequests.forEach(controller => controller.abort());
  pendingRequests.clear();
}

// 在拦截器中使用
service.interceptors.request.use(config => {
  addPendingRequest(config);
  return config;
});

service.interceptors.response.use(
  response => {
    removePendingRequest(response.config);
    return response;
  },
  error => {
    if (error.config) {
      removePendingRequest(error.config);
    }
    return Promise.reject(error);
  }
);
```

## Loading状态统一管理

```typescript
// utils/loading.ts
import { ref } from 'vue';

const loadingCount = ref(0);
const globalLoading = ref(false);

export function useLoading() {
  function startLoading() {
    loadingCount.value++;
    if (loadingCount.value === 1) {
      globalLoading.value = true;
    }
  }
  
  function stopLoading() {
    loadingCount.value--;
    if (loadingCount.value <= 0) {
      loadingCount.value = 0;
      globalLoading.value = false;
    }
  }
  
  return {
    loadingCount,
    globalLoading,
    startLoading,
    stopLoading
  };
}

// 在拦截器中使用
const { startLoading, stopLoading } = useLoading();

service.interceptors.request.use(config => {
  if (config.showLoading !== false) {
    startLoading();
  }
  return config;
});

service.interceptors.response.use(
  response => {
    if (response.config.showLoading !== false) {
      stopLoading();
    }
    return response;
  },
  error => {
    if (error.config?.showLoading !== false) {
      stopLoading();
    }
    return Promise.reject(error);
  }
);
```

## API模块化组织

### 目录结构

```
src/api/
├── index.ts              # 统一导出
├── request.ts            # axios实例
├── types.ts              # 类型定义
├── modules/
│   ├── auth.ts           # 认证相关
│   ├── article.ts        # 文章相关
│   ├── comment.ts        # 评论相关
│   ├── user.ts           # 用户相关
│   ├── category.ts       # 分类相关
│   └── tag.ts            # 标签相关
└── interceptors.ts       # 拦截器
```

### API模块示例

```typescript
// api/modules/article.ts
import request from '../request';
import type { 
  Article, 
  ArticleListParams, 
  ArticleListResponse,
  CreateArticleParams,
  UpdateArticleParams 
} from '../types';

export const articleApi = {
  // 获取文章列表
  getList(params: ArticleListParams): Promise<ArticleListResponse> {
    return request.get('/articles', { params });
  },
  
  // 获取文章详情
  getDetail(id: number): Promise<Article> {
    return request.get(`/articles/${id}`);
  },
  
  // 创建文章
  create(data: CreateArticleParams): Promise<Article> {
    return request.post('/articles', data);
  },
  
  // 更新文章
  update(id: number, data: UpdateArticleParams): Promise<Article> {
    return request.put(`/articles/${id}`, data);
  },
  
  // 删除文章
  delete(id: number): Promise<void> {
    return request.delete(`/articles/${id}`);
  },
  
  // 发布文章
  publish(id: number): Promise<Article> {
    return request.put(`/articles/${id}/publish`);
  },
  
  // 获取文章归档
  getArchive(): Promise<Record<string, Article[]>> {
    return request.get('/articles/archive');
  }
};

// api/modules/auth.ts
export const authApi = {
  // 登录
  login(data: LoginForm): Promise<LoginResponse> {
    return request.post('/auth/login', data);
  },
  
  // 获取验证码
  getCaptcha(): Promise<CaptchaResponse> {
    return request.get('/auth/captcha');
  },
  
  // 获取用户信息
  getUserInfo(): Promise<UserInfo> {
    return request.get('/auth/userinfo');
  },
  
  // 刷新Token
  refreshToken(): Promise<LoginResponse> {
    return request.post('/auth/refresh');
  },
  
  // 登出
  logout(): Promise<void> {
    return request.post('/auth/logout');
  }
};

// api/index.ts
export { articleApi } from './modules/article';
export { authApi } from './modules/auth';
export { commentApi } from './modules/comment';
export { userApi } from './modules/user';
export { categoryApi } from './modules/category';
export { tagApi } from './modules/tag';
```

## Token刷新机制

```typescript
// utils/tokenRefresh.ts
import axios from 'axios';
import { authApi } from '@/api/modules/auth';

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: any) => void;
}> = [];

function processQueue(error: any, token: string | null = null) {
  failedQueue.forEach(prom => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token!);
    }
  });
  failedQueue = [];
}

export function setupTokenRefresh(instance: AxiosInstance) {
  instance.interceptors.response.use(
    response => response,
    async error => {
      const originalRequest = error.config;
      
      // 401错误且不是刷新Token的请求
      if (error.response?.status === 401 && !originalRequest._retry) {
        if (isRefreshing) {
          // 正在刷新，将请求加入队列
          return new Promise((resolve, reject) => {
            failedQueue.push({ resolve, reject });
          }).then(token => {
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return instance(originalRequest);
          });
        }
        
        originalRequest._retry = true;
        isRefreshing = true;
        
        try {
          const { data } = await authApi.refreshToken();
          const newToken = data.token;
          
          // 更新Token
          localStorage.setItem('token', newToken);
          
          // 处理队列中的请求
          processQueue(null, newToken);
          
          // 重试原始请求
          originalRequest.headers.Authorization = `Bearer ${newToken}`;
          return instance(originalRequest);
        } catch (refreshError) {
          processQueue(refreshError, null);
          
          // 刷新失败，跳转登录
          localStorage.removeItem('token');
          window.location.href = '/login';
          
          return Promise.reject(refreshError);
        } finally {
          isRefreshing = false;
        }
      }
      
      return Promise.reject(error);
    }
  );
}
```

## Mock数据方案

### Mock配置

```typescript
// mock/index.ts
import Mock from 'mockjs';

// 文章列表
Mock.mock(/\/api\/articles/, 'get', (options: any) => {
  const params = new URLSearchParams(options.url.split('?')[1]);
  const page = Number(params.get('page')) || 1;
  const size = Number(params.get('size')) || 20;
  
  return {
    code: 200,
    message: 'success',
    data: {
      items: Mock.mock({
        [`list|${size}`]: [{
          'id|+1': (page - 1) * size + 1,
          'title': '@ctitle(10, 30)',
          'summary': '@cparagraph(1, 3)',
          'author': '@cname',
          'category': '@pick(["前端", "后端", "数据库", "运维"])',
          'tags|1-3': ['@cword(2, 4)'],
          'views|100-10000': 1,
          'createdAt': '@datetime("yyyy-MM-dd HH:mm:ss")',
          'status': '@pick(["DRAFT", "PUBLISHED"])'
        }]
      }).list,
      total: 156,
      page,
      size,
      pages: Math.ceil(156 / size)
    }
  };
});

// 文章详情
Mock.mock(/\/api\/articles\/\d+/, 'get', (options: any) => {
  const id = options.url.match(/\/api\/articles\/(\d+)/)?.[1];
  
  return {
    code: 200,
    message: 'success',
    data: {
      id: Number(id),
      title: '@ctitle(15, 40)',
      content: '@cparagraph(20, 50)',
      summary: '@cparagraph(2, 4)',
      author: { id: 1, name: '@cname', avatar: '@image("100x100")' },
      category: { id: 1, name: '@pick(["前端", "后端", "数据库"])' },
      tags|3-6: [{ id: '@increment', name: '@cword(2, 4)' }],
      views|100-10000: 1,
      createdAt: '@datetime("yyyy-MM-dd HH:mm:ss")',
      updatedAt: '@datetime("yyyy-MM-dd HH:mm:ss")'
    }
  };
});

// 登录
Mock.mock(/\/api\/auth\/login/, 'post', () => {
  return {
    code: 200,
    message: 'success',
    data: {
      token: Mock.Random.string('abcdefghijklmnopqrstuvwxyz0123456789', 32),
      refreshToken: Mock.Random.string('abcdefghijklmnopqrstuvwxyz0123456789', 32)
    }
  };
});
```

### 开发环境配置

```typescript
// main.ts
async function bootstrap() {
  // 开发环境启用Mock
  if (import.meta.env.DEV && import.meta.env.VITE_USE_MOCK === 'true') {
    await import('./mock');
  }
  
  // ... 其他初始化
}

bootstrap();
```

```env
# .env.development
VITE_USE_MOCK=true
VITE_API_BASE_URL=/api
```

良好的接口封装应该让业务开发者无需关心底层细节，专注于业务逻辑。通过拦截器统一处理认证、Loading、错误提示，通过模块化组织让API调用清晰可维护，通过Mock方案让前后端可以并行开发。
