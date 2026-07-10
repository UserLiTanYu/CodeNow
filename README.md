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
| Sa-Token | 1.39.0 | 轻量级权限认证框架 |
| Knife4j | 4.5.0 | API 文档自动生成（基于 SpringDoc 2.8.6） |
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

---

## 项目目录结构

```
codenow/
├── codenow-backend/                    # 后端项目
│   ├── init.sql                        # 数据库初始化脚本（5 张表 + 默认管理员）
│   ├── pom.xml                         # Maven 依赖配置
│   └── src/main/java/com/codenow/
│       ├── config/                     # 配置类（CORS、Sa-Token、MyBatis-Plus、Security）
│       ├── controller/                 # 控制器（Auth、Article、Category、Tag）
│       ├── service/                    # 业务逻辑层
│       │   └── impl/                   # Service 实现类
│       ├── mapper/                     # 数据访问层（MyBatis-Plus Mapper）
│       ├── entity/                     # 数据库实体类（对应表结构）
│       ├── dto/                        # 数据传输对象（请求/响应参数）
│       ├── common/                     # 公共类（统一返回格式 R、分页参数）
│       └── exception/                  # 全局异常处理
│
├── codenow-frontend/                   # 前端项目
│   ├── vite.config.js                  # Vite 配置（含 API 代理）
│   └── src/
│       ├── layout/                     # 主布局（侧边栏 + 顶导航）
│       ├── router/                     # 路由配置（含登录守卫）
│       ├── stores/                     # Pinia 状态管理
│       ├── utils/                      # 工具函数（Axios 封装）
│       └── views/                      # 页面组件
│           ├── login/                  # 登录页
│           ├── article/                # 文章列表 + 文章编辑
│           ├── category/               # 分类管理
│           └── tag/                    # 标签管理
│
├── 开发文档.md                          # 完整开发文档（含六阶段规划）
├── 阶段一-环境搭建与项目初始化-操作日志.md
├── 阶段二-后端核心开发-操作日志.md
├── 阶段三-后端进阶与文档完善-操作日志.md
└── README.md                           # 本文件
```

---

## 环境要求

| 工具 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21 | Spring Boot 3.x 最低要求 JDK 17，本项目使用 21 |
| Maven | 3.9+ | 后端构建工具 |
| Node.js | 22+ 或 24+ | 前端构建工具（package.json engines 要求） |
| MySQL | 8.0+ | 数据库 |
| Redis | 可选 | Sa-Token 分布式 Session，开发阶段可不装 |

---

## 本地启动步骤

### 1. 克隆仓库

```bash
git clone <仓库地址>
cd codenow
```

### 2. 初始化数据库

打开 MySQL 客户端，执行建表脚本：

```bash
mysql -u root -p --default-character-set=utf8mb4 < codenow-backend/init.sql
```

这会创建 `codenow` 数据库、5 张表和一个默认管理员账号（admin / 123456）。

### 3. 启动后端

```bash
cd codenow-backend
./mvnw spring-boot:run
```

启动成功后看到 `Started CodenowBackendApplication in x.x seconds`，后端运行在 `http://localhost:8080`。

### 4. 启动前端

新开一个终端：

```bash
cd codenow-frontend
npm install
npm run dev
```

启动成功后看到 `Local: http://localhost:5173/`，前端运行在 `http://localhost:5173`。

### 5. 访问系统

- 打开浏览器访问 `http://localhost:5173`
- 自动跳转到登录页，输入 admin / 123456 登录
- 登录后进入管理后台

---

## 接口文档

后端集成了 Knife4j（基于 SpringDoc OpenAPI），启动后端后访问：

```
http://localhost:8080/doc.html
```

在 Knife4j 页面中可以：
- 查看所有接口分组（认证管理、文章管理、分类管理、标签管理）
- 点击任意接口查看请求参数说明和示例值
- 点击「Try it out」直接在页面上测试接口（需在请求头中手动添加 `Authorization: <token>`）

获取 Token 方式：调用 `POST /api/auth/login`，传入 `{"username":"admin","password":"123456"}`，返回的 `token` 字段即为认证凭证。

---

## 功能模块清单

### 已实现

- [x] 用户登录 / 登出 / 获取当前用户信息
- [x] 文章 CRUD（创建、查询、修改、删除）
- [x] 文章分页查询
- [x] 文章关联标签（创建时选择标签，查询时返回标签列表）
- [x] 文章查询返回分类名称和标签列表
- [x] 文章列表按分类筛选、按标签筛选
- [x] 文章状态切换（草稿 / 已发布）
- [x] 分类 CRUD
- [x] 标签 CRUD
- [x] 全局异常处理（统一 `{code, message, data}` 返回格式）
- [x] 参数校验（`@NotBlank` + `@Valid`）
- [x] Sa-Token 登录认证 + 路由守卫
- [x] Knife4j API 文档自动生成
- [x] 前端管理后台布局（侧边栏 + 顶导航）
- [x] 前端登录页
- [x] 前端文章列表页（分页 + 筛选）
- [x] 前端文章编辑页（Markdown 编辑器 + 分类下拉 + 标签多选）
- [x] 前端分类管理页（弹窗表单 CRUD）
- [x] 前端标签管理页

### 计划中

- [ ] 文章置顶 / 推荐
- [ ] 文章浏览量统计
- [ ] 评论系统
- [ ] 全文搜索
- [ ] 文章归档（按月份）
- [ ] 访客统计
- [ ] Docker 容器化部署
- [ ] Nginx 反向代理 + HTTPS

---

## 开发进度

| 阶段 | 内容 | 状态 |
|------|------|------|
| 第一阶段 | 环境搭建与项目初始化 | ✅ 已完成 |
| 第二阶段 | 后端核心开发 — 数据库与基础 CRUD | ✅ 已完成 |
| 第三阶段 | 后端进阶 — 业务逻辑完善 | ✅ 已完成 |
| 第四阶段 | 前端核心开发 — 页面与路由 | ✅ 已完成 |
| 第五阶段 | 前后端联调与细节打磨 | ⏳ 待开始 |
| 第六阶段 | 部署上线 | ⏳ 待开始 |

---

## 默认账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | 123456 | 管理员 |

> 密码使用 BCrypt 加密存储在数据库中，上述为明文登录密码。
