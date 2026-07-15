# 码上记（CodeNow）部署指南

## 架构说明

```
浏览器 → Nginx(:80) → 前端静态文件
                   ↘ /api/ → 后端(:8080) → MySQL(:3306)
                                           → Redis(:6379)
```

## 本地部署（Docker）

### 前置条件

- 安装 [Docker Desktop](https://www.docker.com/products/docker-desktop/)（Windows/Mac）或 Docker Engine（Linux）
- 确认 `docker --version` 和 `docker compose version` 正常输出

### 一键部署

```bash
# 1. 克隆项目
git clone <仓库地址>
cd codenow

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，设置数据库密码、Redis 密码和 JWT 签名密钥（必填）

# 3. 一键启动
# Linux / Mac
bash deploy.sh

# Windows
deploy.bat

# 或直接使用 docker compose
docker compose up -d
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 博客前台 | http://localhost/blog |
| 管理后台 | http://localhost |
| 后端 API | http://localhost:8080 |
| 默认账号 | admin / 123456 |

### 常用命令

```bash
# 查看服务状态
docker compose ps

# 查看日志
docker compose logs -f backend   # 后端日志
docker compose logs -f frontend  # 前端/Nginx 日志
docker compose logs -f mysql     # MySQL 日志

# 停止所有服务
docker compose down

# 停止并删除数据卷（慎用，会清空数据库）
docker compose down -v

# 重新构建并启动（代码更新后）
docker compose up -d --build
```

---

## 服务器部署

### 1. 服务器准备

推荐配置：2 核 4G 内存，Ubuntu 22.04 LTS

```bash
# 连接服务器
ssh root@你的服务器IP

# 安装 Docker
apt update && apt install -y docker.io docker-compose-plugin
systemctl enable docker && systemctl start docker

# 验证安装
docker --version
docker compose version
```

### 2. 上传项目

```bash
# 方式一：Git 克隆
git clone <仓库地址>
cd codenow

# 方式二：scp 上传
scp -r ./codenow root@服务器IP:/opt/
ssh root@服务器IP
cd /opt/codenow
```

### 3. 配置环境变量

```bash
cp .env.example .env
vim .env
```

修改以下关键配置：

```env
DB_PASSWORD=你的安全密码
REDIS_PASSWORD=你的Redis密码
JWT_SECRET=至少32位的随机字符串
DB_SSL_MODE=REQUIRED
OSS_ACCESS_KEY_ID=你的阿里云AccessKey
OSS_ACCESS_KEY_SECRET=你的阿里云Secret
```

### 4. 构建并启动

```bash
# 构建后端 JAR
cd codenow-backend
./mvnw clean package
cd ..

# 启动所有服务
docker compose up -d

# 查看状态
docker compose ps
```

### 5. 配置域名（可选）

如果需要通过域名访问，修改 `codenow-frontend/nginx.conf`：

```nginx
server_name 你的域名;
```

然后重新构建前端：

```bash
docker compose up -d --build frontend
```

### 6. 配置 HTTPS（可选）

使用 Let's Encrypt 免费证书：

```bash
# 安装 certbot
apt install -y certbot

# 申请证书
certbot certonly --standalone -d 你的域名

# 修改 nginx.conf 启用 HTTPS
# 将证书文件挂载到 Nginx 容器
```

---

## 环境变量说明

| 变量 | 默认值 | 说明 |
|------|--------|------|
| DB_PASSWORD | 无（必填） | MySQL root 密码 |
| DB_NAME | codenow | 数据库名称 |
| DB_SSL_MODE | REQUIRED | 生产数据库 TLS 模式 |
| REDIS_PASSWORD | 无（必填） | Redis 密码 |
| JWT_SECRET | 无（必填） | Sa-Token JWT HMAC 签名密钥，至少 32 位 |
| BACKEND_PORT | 8080 | 后端 API 端口 |
| FRONTEND_PORT | 80 | 前端 Nginx 端口 |
| OSS_ENDPOINT | - | 阿里云 OSS Endpoint |
| OSS_BUCKET | - | OSS Bucket 名称 |
| OSS_ACCESS_KEY_ID | - | OSS AccessKey ID |
| OSS_ACCESS_KEY_SECRET | - | OSS AccessKey Secret |

> MySQL 与 Redis 仅加入 Compose 内部网络，不再映射宿主机端口。需要维护数据库时，优先使用 `docker compose exec mysql mysql ...`，不要长期暴露端口。

### 已有数据库升级

从旧版本升级时，在启动新后端前执行一次：

```bash
docker compose exec -T mysql mysql -uroot -p"$DB_PASSWORD" codenow < codenow-backend/migration-medium-priority.sql
```

该迁移为 `blog_article_tag` 增加逻辑删除字段。全新部署无需执行，`init.sql` 已包含最新结构。

---

## 故障排查

**MySQL 启动失败**
```bash
docker compose logs mysql
# 常见原因：密码不符合策略、端口被占用
```

**后端连接不上数据库**
```bash
docker compose logs backend
# 检查 DB_HOST 是否为 mysql（容器名）
# 确认 MySQL 健康检查通过
```

**前端页面空白**
```bash
docker compose logs frontend
# 检查 Nginx 配置是否正确
# 确认后端 API 代理配置
```

**Redis 连接失败**
```bash
docker compose logs redis
# 检查 REDIS_PASSWORD 是否一致
```

---

## 发布前部署演练

自定义前端域名或端口时，必须将浏览器实际来源加入精确白名单，例如：

```env
FRONTEND_PORT=18081
BACKEND_PORT=18080
CORS_ALLOWED_ORIGINS=http://localhost:18081
STORAGE_TYPE=local
```

`STORAGE_TYPE=local` 适合本地/测试演练，上传文件保存在 `uploads_data` 卷中；生产环境建议使用 `STORAGE_TYPE=oss` 并配置 OSS 凭据。

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps

# 升级脚本可重复执行
docker compose exec -T mysql mysql -uroot -p"$DB_PASSWORD" "$DB_NAME" < codenow-backend/migration-medium-priority.sql

# Linux / CI 端到端冒烟
BACKEND_URL=http://localhost:18080 FRONTEND_URL=http://localhost:18081 bash scripts/smoke-test.sh

# Windows PowerShell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
```

## 回滚步骤

回滚必须与旧应用镜像切换在同一维护窗口内进行：

1. 使用 `mysqldump --single-transaction --routines --triggers` 完成全库备份，并验证备份非空。
2. 停止写流量，保留当前镜像标签和 Compose 配置。
3. 执行 `codenow-backend/rollback-medium-priority.sql`，再切换到旧应用镜像。
4. 若回滚异常，优先从全库备份恢复，再重新部署已验证的新镜像。
5. 恢复后重新运行冒烟脚本，核对文章、评论和上传文件。

警告：不要使用 `docker compose down -v` 进行普通回滚，该命令会删除数据库、Redis 和本地上传卷。
