# 码上记（CodeNow）

> 一个支持 Markdown 写作、文章分类与标签管理的个人技术博客系统，帮助开发者记录和分享学习笔记。

---

## 技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| JDK | 21 | Java 运行环境 |
| Spring Boot | 3.4.7 | Web 应用框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架，内置通用 CRUD |
| MySQL | 8.0+ | 关系型数据库 |
| Redis | 7.x | 缓存热门文章、滑动窗口限流 |
| Sa-Token + JWT | 1.39.0 | 带 HMAC 签名的登录认证 |
| Knife4j | 4.5.0 | API 文档自动生成（基于 SpringDoc 2.8.6） |
| Aliyun OSS SDK | 3.17.4 | 文件上传到阿里云 OSS |
| Spring AOP | — | 操作日志切面、接口限流切面 |
| Spring Security | — | 仅用于 BCrypt 密码加密 |
| Lombok | — | 减少样板代码 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5+ | 前端框架 |
| Vite | 8.x | 构建工具 |
| Element Plus | 2.14+ | UI 组件库 |
| Vue Router | 5.x | 前端路由 |
| Pinia | 3.x | 状态管理 |
| Axios | 1.x | HTTP 请求库 |
| md-editor-v3 | 6.x | Markdown 编辑器组件 |
| marked | 15.x | Markdown 渲染为 HTML（博客前台） |
| highlight.js | 11.x | 代码语法高亮（博客前台） |

### 部署

| 技术 | 用途 |
|------|------|
| Docker + Docker Compose | 容器化部署，一键启动全部服务 |
| Nginx | 前端静态文件服务 + 后端 API 反向代理 |

---

## 项目目录结构

```
codenow/
├── codenow-backend/                    # 后端项目
│   ├── Dockerfile                      # 后端 Docker 镜像构建文件
│   ├── init.sql                        # 数据库初始化脚本（7 张表 + 默认管理员）
│   ├── pom.xml                         # Maven 依赖配置
│   └── src/main/
│       ├── java/com/codenow/
│       │   ├── annotation/             # 自定义注解（@OperationLog、@RateLimit）
│       │   ├── aspect/                 # AOP 切面（操作日志、接口限流）
│       │   ├── config/                 # 配置类（CORS、Sa-Token、Redis、OSS、异步）
│       │   ├── controller/             # 控制器（Auth、Article、Category、Tag、Blog、Comment、Upload、Log）
│       │   ├── service/                # 业务逻辑层
│       │   │   └── impl/               # Service 实现类
│       │   ├── mapper/                 # 数据访问层（MyBatis-Plus Mapper）
│       │   ├── entity/                 # 数据库实体类（对应表结构）
│       │   ├── dto/                    # 数据传输对象（请求/响应参数）
│       │   ├── common/                 # 公共类（统一返回格式 R、分页参数）
│       │   └── exception/              # 全局异常处理（含限流异常）
│       └── resources/
│           ├── application.yaml        # 开发环境配置
│           ├── application-prod.yml    # 生产环境配置
│           └── scripts/                # Lua 脚本（Redis 限流）
│
├── codenow-frontend/                   # 前端项目
│   ├── Dockerfile                      # 前端 Docker 镜像（多阶段构建）
│   ├── nginx.conf                      # Nginx 配置（反向代理）
│   ├── vite.config.js                  # Vite 配置（含 API 代理）
│   └── src/
│       ├── layout/                     # 布局组件
│       │   ├── MainLayout.vue          # 管理后台布局（侧边栏 + 顶导航）
│       │   └── BlogLayout.vue          # 博客前台布局（顶栏 + 内容区 + 侧边栏）
│       ├── components/                 # 公共组件
│       │   ├── CommentForm.vue         # 评论表单组件
│       │   ├── CommentTree.vue         # 递归评论树组件
│       │   └── ImageUpload.vue         # 图片上传组件（拖拽/点击）
│       ├── router/                     # 路由配置（含登录守卫）
│       ├── stores/                     # Pinia 状态管理
│       ├── utils/                      # 工具函数（Axios 封装）
│       └── views/                      # 页面组件
│           ├── login/                  # 登录页（管理端）
│           ├── article/                # 文章列表 + 文章编辑（管理端）
│           ├── category/               # 分类管理（管理端）
│           ├── tag/                    # 标签管理（管理端）
│           ├── comment/                # 评论管理（管理端）
│           ├── log/                    # 操作日志（管理端）
│           └── blog/                   # 博客前台页面（用户端）
│               ├── BlogHome.vue        # 博客首页（文章列表 + 热门文章）
│               ├── BlogArticle.vue     # 文章详情页（Markdown 渲染 + 评论区）
│               ├── BlogCategory.vue    # 分类文章页
│               └── BlogTag.vue         # 标签文章页
│
├── docker-compose.yml                  # Docker Compose 编排文件
├── .env.example                        # 环境变量模板
├── deploy.sh                           # Linux/Mac 一键部署脚本
├── deploy.bat                          # Windows 一键部署脚本
├── DEPLOY.md                           # 部署文档
├── 开发文档.md                          # 完整开发文档
├── 扩展功能开发文档.md                    # 扩展功能方案设计
├── 扩展功能一-Redis缓存热门文章-操作日志.md
├── 扩展功能二-操作日志AOP-操作日志.md
├── 扩展功能三-评论系统-操作日志.md
├── 扩展功能四-API接口限流-操作日志.md
├── 扩展功能五-文件上传-操作日志.md
├── 扩展功能六-Docker部署-操作日志.md
└── README.md                           # 本文件
```

---

## 环境要求

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21 | Spring Boot 3.x 最低要求 JDK 17，本项目使用 21 |
| Maven | 3.9+ | 后端构建工具 |
| Node.js | 22+ 或 24+ | 前端构建工具 |
| MySQL | 8.0+ | 数据库 |
| Redis | 7.x | 缓存热门文章、接口限流（必需） |
| Docker（可选） | 20+ | 容器化部署时需要 |

---

## 本地启动步骤

### 1. 克隆仓库

```bash
git clone <仓库地址>
cd codenow
```

### 2. 初始化数据库

```bash
mysql -u root -p --default-character-set=utf8mb4 < codenow-backend/init.sql
```

创建 `codenow` 数据库、7 张表和默认管理员账号（admin / 123456）。

### 3. 启动 Redis

确保 Redis 运行在 `localhost:6379`，密码 `123456`（可在 `application.yaml` 中修改）。

### 4. 启动后端

```bash
# Windows
cd codenow-backend
.\mvnw.cmd spring-boot:run

# Mac / Linux
cd codenow-backend
./mvnw spring-boot:run
```

### 5. 启动前端

```bash
cd codenow-frontend
npm install
npm run dev
```

### 6. 访问系统

- **管理后台**：`http://localhost:5173`（admin / 123456）
- **博客前台**：`http://localhost:5173/blog`（无需登录）

---

## Docker 一键部署

```bash
# 1. 配置环境变量
cp .env.example .env

# 2. 一键启动（Linux/Mac）
bash deploy.sh

# Windows
deploy.bat
```

访问 `http://localhost` 即可。详见 [DEPLOY.md](DEPLOY.md)。

---

## 接口文档

后端集成了 Knife4j，启动后端后访问：

```
http://localhost:8080/doc.html
```

接口分组：认证管理、文章管理、分类管理、标签管理、评论管理、文件上传、操作日志、博客前台。

---

## 功能模块清单

### 核心功能

- [x] 用户登录 / 登出 / 获取当前用户信息
- [x] 文章 CRUD（创建、查询、修改、删除）
- [x] 文章分页查询
- [x] 文章关联标签（创建时选择标签，查询时返回标签列表）
- [x] 文章查询返回分类名称和标签列表
- [x] 文章列表按分类筛选、按标签筛选
- [x] 文章状态切换（草稿 / 已发布）
- [x] 文章置顶 / 推荐
- [x] 分类 CRUD
- [x] 标签 CRUD
- [x] 全局异常处理（统一 `{code, message, data}` 返回格式）
- [x] 参数校验（`@NotBlank` + `@Valid`）
- [x] Sa-Token 登录认证 + 路由守卫
- [x] Knife4j API 文档自动生成

### 博客前台

- [x] 博客首页（文章卡片列表 + 分页）
- [x] 文章详情页（Markdown 渲染 + 代码高亮）
- [x] 博客前台按分类 / 标签筛选文章
- [x] 博客前台公开 API（无需登录）
- [x] 文章浏览量统计（访问详情页自动 +1）
- [x] 侧边栏热门文章（Redis ZSet 缓存 Top 10）
- [x] 评论系统（支持楼中楼回复、根评论分页、树形嵌套展示）

### 扩展功能

- [x] Redis 缓存热门文章（ZSet + TTL 5 分钟自动刷新）
- [x] AOP 操作日志（自动记录管理员操作，异步写入数据库）
- [x] API 接口限流（Redis + Lua 滑动窗口，Redis 不可用时降级放行）
- [x] 文件上传（阿里云 OSS，拖拽上传 + 图片预览）
- [x] Docker 容器化部署（MySQL + Redis + 后端 + 前端四服务编排）

### 管理后台页面

- [x] 登录页
- [x] 文章列表页（分页 + 筛选 + 置顶）
- [x] 文章编辑页（Markdown 编辑器 + 分类下拉 + 标签多选 + 图片上传）
- [x] 分类管理页（弹窗表单 CRUD）
- [x] 标签管理页
- [x] 评论管理页（分页 + 删除）
- [x] 操作日志页（分页查看）

### 计划中

- [ ] 全文搜索
- [ ] 文章归档（按月份）
- [ ] 访客统计
- [ ] CI/CD 自动化部署

---

## 开发进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| 第一阶段 | 环境搭建与项目初始化 | ✅ 已完成 |
| 第二阶段 | 后端核心开发 — 数据库与基础 CRUD | ✅ 已完成 |
| 第三阶段 | 后端进阶 — 业务逻辑完善 | ✅ 已完成 |
| 第四阶段 | 前端核心开发 — 页面与路由 | ✅ 已完成 |
| 第五阶段 | 前后端联调与细节打磨 | ✅ 已完成 |
| 第六阶段 | 用户端（前台博客）开发 | ✅ 已完成 |
| 扩展功能 | Redis 缓存、AOP 操作日志、评论系统、接口限流、文件上传、Docker 部署 | ✅ 已完成 |

---

## 数据库表结构

| 表名 | 说明 |
|------|------|
| `sys_user` | 用户表 |
| `blog_article` | 文章表 |
| `blog_category` | 分类表 |
| `blog_tag` | 标签表 |
| `blog_article_tag` | 文章-标签关联表 |
| `sys_operation_log` | 操作日志表 |
| `blog_comment` | 评论表 |

---

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 |

> 密码使用 BCrypt 加密存储在数据库中，上述为明文登录密码。
