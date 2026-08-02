# 前后端接口契约与联调流程

在前后端分离的开发模式下，接口契约是团队协作的基石。一份清晰的接口文档能大幅减少沟通成本，而规范的联调流程则能快速发现和解决问题。本文将介绍如何通过接口文档驱动开发、Mock Server的使用、联调环境配置以及常见问题排查。

## 接口文档驱动开发

### OpenAPI规范

OpenAPI（原Swagger）是描述RESTful API的标准规范：

```yaml
# openapi.yaml
openapi: 3.0.3
info:
  title: 码上记博客系统API
  description: 技术博客系统RESTful API文档
  version: 1.0.0
  contact:
    name: 开发团队
    email: dev@codenow.com

servers:
  - url: http://localhost:8080
    description: 本地开发环境
  - url: https://api.codenow.com
    description: 生产环境

tags:
  - name: auth
    description: 认证相关
  - name: articles
    description: 文章管理
  - name: comments
    description: 评论管理

paths:
  /api/auth/login:
    post:
      tags: [auth]
      summary: 用户登录
      operationId: login
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LoginRequest'
            example:
              username: admin
              password: "123456"
              captcha: "1234"
              captchaKey: "abc123"
      responses:
        '200':
          description: 登录成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/LoginResponse'
        '400':
          description: 参数错误
        '401':
          description: 认证失败

  /api/articles:
    get:
      tags: [articles]
      summary: 获取文章列表
      operationId: getArticles
      parameters:
        - name: page
          in: query
          schema:
            type: integer
            default: 1
          description: 页码
        - name: size
          in: query
          schema:
            type: integer
            default: 20
            maximum: 100
          description: 每页数量
        - name: categoryId
          in: query
          schema:
            type: integer
          description: 分类ID
        - name: tagId
          in: query
          schema:
            type: integer
          description: 标签ID
        - name: keyword
          in: query
          schema:
            type: string
          description: 搜索关键词
        - name: status
          in: query
          schema:
            type: string
            enum: [DRAFT, PUBLISHED, ARCHIVED]
          description: 文章状态
      responses:
        '200':
          description: 成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ArticleListResponse'

    post:
      tags: [articles]
      summary: 创建文章
      operationId: createArticle
      security:
        - bearerAuth: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateArticleRequest'
      responses:
        '201':
          description: 创建成功
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/Article'

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    LoginRequest:
      type: object
      required: [username, password, captcha, captchaKey]
      properties:
        username:
          type: string
          description: 用户名
        password:
          type: string
          description: 密码
        captcha:
          type: string
          description: 验证码
        captchaKey:
          type: string
          description: 验证码key

    LoginResponse:
      type: object
      properties:
        code:
          type: integer
          example: 200
        data:
          type: object
          properties:
            token:
              type: string
            refreshToken:
              type: string

    Article:
      type: object
      properties:
        id:
          type: integer
          format: int64
        title:
          type: string
        content:
          type: string
        summary:
          type: string
        slug:
          type: string
        status:
          type: string
          enum: [DRAFT, PUBLISHED, ARCHIVED]
        category:
          $ref: '#/components/schemas/Category'
        tags:
          type: array
          items:
            $ref: '#/components/schemas/Tag'
        author:
          $ref: '#/components/schemas/UserBasic'
        views:
          type: integer
        createdAt:
          type: string
          format: date-time
        updatedAt:
          type: string
          format: date-time

    CreateArticleRequest:
      type: object
      required: [title, content, categoryId]
      properties:
        title:
          type: string
          minLength: 1
          maxLength: 200
        content:
          type: string
          minLength: 1
        summary:
          type: string
          maxLength: 500
        categoryId:
          type: integer
        tags:
          type: array
          items:
            type: string
        status:
          type: string
          enum: [DRAFT, PUBLISHED]
```

### 使用Apifox协作

Apifox是国内团队开发的API协作工具，集文档、Mock、测试于一体：

```markdown
## Apifox工作流

### 1. 接口设计阶段
- 后端在Apifox中定义接口
- 使用OpenAPI 3.0规范
- 定义请求/响应Schema

### 2. 前后端评审
- 评审接口设计合理性
- 确认字段命名和类型
- 讨论异常场景处理

### 3. 开发阶段
- 后端实现接口
- 前端使用Mock数据开发
- 自动生成TypeScript类型

### 4. 联调阶段
- 切换到开发环境
- 对比实际响应与文档
- 记录并修复问题

### 5. 测试阶段
- 自动化测试用例
- 性能测试基线
- 回归测试套件
```

## Mock Server

### 本地Mock方案

```typescript
// mock/server.ts
import { createServer, Model, Response } from 'miragejs';

export function makeServer({ environment = 'development' } = {}) {
  return createServer({
    environment,
    
    models: {
      article: Model,
      user: Model,
      comment: Model
    },
    
    seeds(server) {
      server.create('article', {
        id: 1,
        title: 'Spring Boot入门指南',
        content: '这是一篇关于Spring Boot的入门文章...',
        status: 'PUBLISHED',
        authorId: 1,
        categoryId: 1,
        views: 1234,
        createdAt: '2024-01-15T10:30:00Z'
      });
      
      server.create('article', {
        id: 2,
        title: 'Vue 3组合式API详解',
        content: 'Vue 3带来了全新的组合式API...',
        status: 'PUBLISHED',
        authorId: 1,
        categoryId: 2,
        views: 892,
        createdAt: '2024-01-14T15:20:00Z'
      });
    },
    
    routes() {
      this.namespace = 'api';
      
      // 文章列表
      this.get('/articles', (schema, request) => {
        const { page = 1, size = 20, keyword, categoryId, status } = request.queryParams;
        
        let articles = schema.articles.all();
        
        // 过滤
        if (keyword) {
          articles = articles.filter(a => 
            a.title.includes(keyword) || a.content.includes(keyword)
          );
        }
        if (categoryId) {
          articles = articles.filter(a => a.categoryId === Number(categoryId));
        }
        if (status) {
          articles = articles.filter(a => a.status === status);
        }
        
        // 分页
        const total = articles.length;
        const start = (Number(page) - 1) * Number(size);
        const items = articles.slice(start, start + Number(size));
        
        return {
          code: 200,
          data: {
            items: items.models,
            total,
            page: Number(page),
            size: Number(size),
            pages: Math.ceil(total / Number(size))
          }
        };
      });
      
      // 文章详情
      this.get('/articles/:id', (schema, request) => {
        const article = schema.articles.find(request.params.id);
        
        if (!article) {
          return new Response(404, {}, { code: 404, message: '文章不存在' });
        }
        
        return { code: 200, data: article };
      });
      
      // 创建文章
      this.post('/articles', (schema, request) => {
        const attrs = JSON.parse(request.requestBody);
        const article = schema.articles.create({
          ...attrs,
          id: Date.now(),
          views: 0,
          createdAt: new Date().toISOString()
        });
        
        return { code: 201, data: article };
      });
      
      // 登录
      this.post('/auth/login', (schema, request) => {
        const { username, password } = JSON.parse(request.requestBody);
        
        if (username === 'admin' && password === '123456') {
          return {
            code: 200,
            data: {
              token: 'mock-jwt-token-' + Date.now(),
              refreshToken: 'mock-refresh-token-' + Date.now()
            }
          };
        }
        
        return new Response(401, {}, { code: 401, message: '用户名或密码错误' });
      });
    }
  });
}
```

### Mock数据生成器

```typescript
// mock/generators/article.ts
import { faker } from '@faker-js/faker/locale/zh_CN';

export function generateArticle(overrides?: Partial<Article>): Article {
  return {
    id: faker.number.int({ min: 1, max: 10000 }),
    title: faker.lorem.sentence({ min: 3, max: 8 }),
    content: faker.lorem.paragraphs(10),
    summary: faker.lorem.paragraph({ min: 1, max: 3 }),
    slug: faker.helpers.slugify(faker.lorem.words(3)),
    status: faker.helpers.arrayElement(['DRAFT', 'PUBLISHED', 'ARCHIVED']),
    author: {
      id: faker.number.int({ min: 1, max: 100 }),
      name: faker.person.fullName(),
      avatar: faker.image.avatar()
    },
    category: {
      id: faker.number.int({ min: 1, max: 10 }),
      name: faker.helpers.arrayElement(['前端', '后端', '数据库', '运维', '算法'])
    },
    tags: Array.from({ length: faker.number.int({ min: 1, max: 5 }) }, () => ({
      id: faker.number.int({ min: 1, max: 50 }),
      name: faker.lorem.word()
    })),
    views: faker.number.int({ min: 0, max: 100000 }),
    createdAt: faker.date.past().toISOString(),
    updatedAt: faker.date.recent().toISOString(),
    ...overrides
  };
}

export function generateArticleList(count: number, overrides?: Partial<Article>): Article[] {
  return Array.from({ length: count }, () => generateArticle(overrides));
}
```

## 联调环境配置

### 多环境配置

```typescript
// config/env.ts
interface EnvConfig {
  apiBaseUrl: string;
  mockEnabled: boolean;
  debug: boolean;
}

const envConfigs: Record<string, EnvConfig> = {
  development: {
    apiBaseUrl: '/api',
    mockEnabled: true,
    debug: true
  },
  local: {
    apiBaseUrl: 'http://localhost:8080/api',
    mockEnabled: false,
    debug: true
  },
  staging: {
    apiBaseUrl: 'https://staging-api.codenow.com/api',
    mockEnabled: false,
    debug: false
  },
  production: {
    apiBaseUrl: 'https://api.codenow.com/api',
    mockEnabled: false,
    debug: false
  }
};

export function getEnvConfig(): EnvConfig {
  const mode = import.meta.env.MODE || 'development';
  return envConfigs[mode] || envConfigs.development;
}
```

```env
# .env.development
VITE_API_BASE_URL=/api
VITE_USE_MOCK=true
VITE_DEBUG=true

# .env.local
VITE_API_BASE_URL=http://localhost:8080/api
VITE_USE_MOCK=false
VITE_DEBUG=true

# .env.staging
VITE_API_BASE_URL=https://staging-api.codenow.com/api
VITE_USE_MOCK=false
VITE_DEBUG=false
```

### Vite代理配置

```typescript
// vite.config.ts
import { defineConfig, loadEnv } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd());
  
  return {
    plugins: [vue()],
    server: {
      port: 5173,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
          configure: (proxy, options) => {
            proxy.on('proxyReq', (proxyReq, req) => {
              console.log(`[Proxy] ${req.method} ${req.url} -> ${options.target}${req.url}`);
            });
            proxy.on('proxyRes', (proxyRes, req) => {
              console.log(`[Proxy Response] ${proxyRes.statusCode} ${req.url}`);
            });
            proxy.on('error', (err, req) => {
              console.error(`[Proxy Error] ${req.url}:`, err.message);
            });
          }
        }
      }
    }
  };
});
```

## CORS处理

### 后端配置

```java
// WebConfig.java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins(
                "http://localhost:5173",
                "http://localhost:3000",
                "https://codenow.com"
            )
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}

// 或使用注解
@RestController
@CrossOrigin(origins = {"http://localhost:5173"})
@RequestMapping("/api/articles")
public class ArticleController {
    // ...
}
```

### Nginx配置

```nginx
server {
    listen 80;
    server_name codenow.com;
    
    location /api/ {
        # CORS头
        add_header Access-Control-Allow-Origin $http_origin always;
        add_header Access-Control-Allow-Methods 'GET, POST, PUT, DELETE, OPTIONS' always;
        add_header Access-Control-Allow-Headers 'Authorization, Content-Type, X-Requested-With' always;
        add_header Access-Control-Allow-Credentials true always;
        add_header Access-Control-Max-Age 3600 always;
        
        # 预检请求直接返回
        if ($request_method = 'OPTIONS') {
            return 204;
        }
        
        proxy_pass http://backend;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 接口版本管理

### URL版本控制

```java
// 版本化控制器
@RestController
@RequestMapping("/api/v1/articles")
public class ArticleControllerV1 {
    
    @GetMapping("/{id}")
    public ArticleV1DTO getArticle(@PathVariable Long id) {
        // V1版本实现
    }
}

@RestController
@RequestMapping("/api/v2/articles")
public class ArticleControllerV2 {
    
    @GetMapping("/{id}")
    public ArticleV2DTO getArticle(@PathVariable Long id) {
        // V2版本实现，增加新字段
    }
}
```

### 版本兼容处理

```java
// 使用接口版本协商
@RestController
@RequestMapping("/api/articles")
public class ArticleController {
    
    @GetMapping(value = "/{id}", 
        produces = "application/vnd.codenow.v1+json")
    public ArticleV1DTO getArticleV1(@PathVariable Long id) {
        return articleService.findByIdV1(id);
    }
    
    @GetMapping(value = "/{id}", 
        produces = "application/vnd.codenow.v2+json")
    public ArticleV2DTO getArticleV2(@PathVariable Long id) {
        return articleService.findByIdV2(id);
    }
}
```

## 联调问题排查清单

### 常见问题

```markdown
## 联调问题排查清单

### 1. 网络层问题

- [ ] 请求是否发出？（浏览器Network面板）
- [ ] 请求URL是否正确？
- [ ] 请求方法是否正确？（GET/POST/PUT/DELETE）
- [ ] 请求头是否完整？（Authorization、Content-Type）
- [ ] 请求体格式是否正确？（JSON格式检查）
- [ ] 是否被CORS阻止？（查看Console错误）
- [ ] 是否被代理正确转发？

### 2. 认证问题

- [ ] Token是否过期？
- [ ] Token格式是否正确？（Bearer前缀）
- [ ] Token是否在请求头中？
- [ ] 权限是否足够？

### 3. 数据格式问题

- [ ] 字段名是否与文档一致？（大小写敏感）
- [ ] 字段类型是否正确？（string/number/boolean）
- [ ] 日期格式是否正确？（ISO 8601）
- [ ] 枚举值是否正确？
- [ ] 是否缺少必填字段？

### 4. 分页问题

- [ ] 页码从0还是1开始？
- [ ] 分页参数名是否正确？
- [ ] 响应中的总数字段名是否一致？

### 5. 文件上传问题

- [ ] Content-Type是否为multipart/form-data？
- [ ] 文件大小是否超限？
- [ ] 文件类型是否允许？

### 6. 响应解析问题

- [ ] 响应结构是否与文档一致？
- [ ] 嵌套层级是否正确？
- [ ] 空值处理是否正确？（null/undefined/空字符串）
```

### 调试工具

```typescript
// 请求日志中间件
service.interceptors.request.use(config => {
  if (import.meta.env.DEV) {
    console.group(`🚀 ${config.method?.toUpperCase()} ${config.url}`);
    console.log('Headers:', config.headers);
    console.log('Params:', config.params);
    console.log('Data:', config.data);
    console.groupEnd();
  }
  return config;
});

service.interceptors.response.use(response => {
  if (import.meta.env.DEV) {
    console.group(`✅ ${response.status} ${response.config.url}`);
    console.log('Response:', response.data);
    console.log('Duration:', Date.now() - response.config.headers['X-Start-Time'], 'ms');
    console.groupEnd();
  }
  return response;
}, error => {
  if (import.meta.env.DEV) {
    console.group(`❌ ${error.response?.status || 'Network Error'} ${error.config?.url}`);
    console.log('Error:', error.message);
    console.log('Response:', error.response?.data);
    console.groupEnd();
  }
  return Promise.reject(error);
});
```

接口契约和联调流程是前后端协作的润滑剂。通过标准化的接口文档、可靠的Mock方案、规范的联调流程，可以大幅减少沟通成本和联调时间。关键是要在团队中建立并遵守这些规范，让接口对接成为一件可预期、可控制的事情。
