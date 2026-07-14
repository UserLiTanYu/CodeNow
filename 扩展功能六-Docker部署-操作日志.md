# 扩展功能六：Docker 容器化部署 — 操作日志

> 本功能实现项目的容器化部署，通过 docker-compose 一键启动 MySQL + Redis + 后端 + 前端四个服务，配合 Nginx 反向代理实现完整的博客系统。

---

## 一、编写后端 Dockerfile

文件：`codenow-backend/Dockerfile`

- 基础镜像：`eclipse-temurin:21-jre-alpine`（轻量 JDK 21）
- 设置时区为 Asia/Shanghai
- 复制 JAR 包到 /app
- JVM 参数：`-Xms256m -Xmx512m -XX:+UseG1GC`
- 暴露 8080 端口
- 启动命令：`java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod`

---

## 二、编写生产环境配置

文件：`codenow-backend/src/main/resources/application-prod.yml`

与开发环境的区别：
- 数据库连接地址改为容器名 `mysql`（通过环境变量注入）
- Redis 连接地址改为容器名 `redis`
- 关闭 SQL 日志（NoLoggingImpl）
- 关闭 Knife4j / Swagger（生产环境不暴露 API 文档）
- 日志级别调整为 info / warn

所有敏感配置通过 `${ENV_VAR:default}` 语法从环境变量读取。

---

## 三、编写前端 Dockerfile

文件：`codenow-frontend/Dockerfile`

采用多阶段构建：
- **第一阶段（builder）**：基于 `node:22-alpine`，执行 `npm ci` + `npm run build`
- **第二阶段（运行）**：基于 `nginx:alpine`，复制构建产物和 Nginx 配置

优势：最终镜像只包含 Nginx + 静态文件，不包含 Node.js 和 node_modules，镜像体积从 ~500MB 缩小到 ~30MB。

---

## 四、编写 Nginx 配置

文件：`codenow-frontend/nginx.conf`

核心配置：
- 前端静态文件：`root /usr/share/nginx/html`，`try_files` 支持 Vue Router history 模式
- 后端 API 代理：`location /api/` 转发到 `http://backend:8080/api/`
- 文件上传大小限制：`client_max_body_size 10m`
- gzip 压缩：对文本和 JSON 文件启用压缩

---

## 五、编写 docker-compose.yml

文件：`docker-compose.yml`

编排 4 个服务：

| 服务 | 镜像 | 端口 | 依赖 |
|------|------|------|------|
| mysql | mysql:8.0 | 3306 | 无 |
| redis | redis:7-alpine | 6379 | 无 |
| backend | 构建自 codenow-backend/Dockerfile | 8080 | mysql(健康) + redis(健康) |
| frontend | 构建自 codenow-frontend/Dockerfile | 80 | backend |

关键配置：
- **健康检查**：MySQL 使用 `mysqladmin ping`，Redis 使用 `redis-cli ping`
- **依赖顺序**：`depends_on` + `condition: service_healthy` 确保数据库就绪后再启动后端
- **数据卷**：`mysql_data` 和 `redis_data` 持久化存储
- **自定义网络**：`codenow-net` 桥接网络，容器间通过服务名访问
- **环境变量**：通过 `${VAR:-default}` 语法从 `.env` 文件读取

---

## 六、配置环境变量管理

文件：`.env`（不提交到 Git）+ `.env.example`（提交到 Git 模板）

管理的敏感信息：
- DB_PASSWORD — MySQL 密码
- REDIS_PASSWORD — Redis 密码
- OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET — 阿里云 OSS 密钥

`.env` 已在 `.gitignore` 中排除，不会泄露到代码仓库。

---

## 七、编写部署脚本和说明

文件：`deploy.sh`（Linux/Mac）、`deploy.bat`（Windows）

脚本流程：
1. 检查 Docker 是否安装
2. 检查 .env 文件是否存在（不存在则从模板创建）
3. 构建后端 JAR 包（`mvnw clean package`）
4. 构建 Docker 镜像（`docker compose build`）
5. 启动所有服务（`docker compose up -d`）
6. 等待服务启动并显示状态

文件：`DEPLOY.md`

包含完整的部署文档：
- 架构说明（Nginx → 前端 + 后端 → MySQL + Redis）
- 本地部署步骤（Docker Desktop）
- 服务器部署步骤（Ubuntu + Docker Engine）
- 环境变量说明表格
- 常用 Docker 命令
- 故障排查指南

---

## 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `codenow-backend/Dockerfile` | 新建 | 后端 Docker 镜像构建文件 |
| `codenow-backend/src/main/resources/application-prod.yml` | 新建 | 生产环境配置 |
| `codenow-frontend/Dockerfile` | 新建 | 前端多阶段构建文件 |
| `codenow-frontend/nginx.conf` | 新建 | Nginx 反向代理配置 |
| `docker-compose.yml` | 新建 | Docker Compose 编排文件 |
| `.env` | 新建 | 环境变量配置（不提交 Git） |
| `.env.example` | 新建 | 环境变量模板（提交 Git） |
| `deploy.sh` | 新建 | Linux/Mac 一键部署脚本 |
| `deploy.bat` | 新建 | Windows 一键部署脚本 |
| `DEPLOY.md` | 新建 | 完整部署文档 |

---

## 验收结果

| 验收项 | 结果 |
|--------|------|
| docker-compose up 一键启动所有服务 | ✅ |
| 浏览器通过 http://localhost 访问博客 | ✅ |
| MySQL 数据持久化（容器重启不丢失） | ✅ mysql_data volume |
| 敏感信息通过 .env 管理 | ✅ .env + .env.example |
| 部署步骤文档完整 | ✅ DEPLOY.md |
| 多阶段构建优化前端镜像体积 | ✅ |
| Nginx 反向代理 API 请求 | ✅ |
| 服务健康检查和依赖顺序 | ✅ |
