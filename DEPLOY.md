# 码上记（CodeNow）生产部署手册

本文档适用于将 CodeNow 部署到一台 Ubuntu 服务器。项目使用 Docker Compose 编排 MySQL、Redis、Spring Boot 后端、Vue/Nginx 前端和本地上传卷初始化服务。

> 当前版本已经通过全新部署、重复迁移、数据库回滚恢复、登录、文章发布、评论、图片上传和真实浏览器访问演练。公网开放前仍必须完成强密码、HTTPS 和异地备份配置。

## 1. 部署架构

```text
浏览器
  │
  ├── HTTPS :443
  ▼
宿主机 Nginx（TLS 终止）
  │
  └── 127.0.0.1:8081
        ▼
前端容器 Nginx :80
  ├── 静态 Vue 页面
  └── /api/* ──► 后端容器 :8080
                    ├── MySQL :3306
                    ├── Redis :6379
                    └── uploads_data / OSS
```

MySQL 和 Redis 只接入 Compose 内部网络，不映射到公网。后端端口建议只绑定宿主机回环地址，外部请求统一经过前端反向代理。

## 2. 上线前准备

### 2.1 推荐配置

- Ubuntu 22.04/24.04 LTS
- 2 核 CPU、4 GB 内存或更高；2 GB 仅建议用于低流量个人部署，并配置 Swap 与较小 Java 堆
- 20 GB 以上可用磁盘
- 一个已解析到服务器公网 IP 的域名
- 云安全组开放 `22`、`80`、`443`
- Git 仓库读取权限

不要向公网开放 `3306` 和 `6379`。

### 2.2 代码状态

部署前应确认 CI 的以下检查全部通过：

- `backend-tests`
- `frontend-quality`
- `compose-smoke`

推荐通过 Git Tag 固定生产版本：

```bash
git tag -a v1.0.0 -m "CodeNow v1.0.0"
git push origin v1.0.0
```

## 3. 安装 Docker

生产服务器建议使用 Docker 官方 APT 仓库安装 Docker Engine 与 Compose 插件：

- [Docker Engine on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)
- [Docker Compose Plugin](https://docs.docker.com/compose/install/linux/)

安装完成后验证：

```bash
sudo systemctl enable --now docker
sudo docker --version
sudo docker compose version
```

以下命令默认当前用户有权运行 Docker；否则在 `docker` 前添加 `sudo`。

## 4. 获取代码

当前仓库为公开仓库，生产服务器可以通过 HTTPS 直接克隆，不需要配置 GitHub 密码、访问令牌或 SSH Deploy Key。

```bash
sudo mkdir -p /opt/codenow
sudo chown "$USER":"$USER" /opt/codenow
git clone https://github.com/UserLiTanYu/CodeNow.git /opt/codenow
cd /opt/codenow
```

生产环境建议检出已验证的 Tag：

```bash
git checkout v1.0.0
```

## 5. 配置环境变量

创建生产配置：

```bash
cp .env.example .env
chmod 600 .env
nano .env
```

使用以下命令分别生成数据库密码、Redis 密码和 JWT 密钥：

```bash
openssl rand -hex 32
```

### 5.1 推荐的 HTTPS 配置

```env
DB_PASSWORD=<独立的64位十六进制随机值>
DB_NAME=codenow
DB_SSL_MODE=REQUIRED

REDIS_PASSWORD=<另一个独立随机值>

JWT_SECRET=<另一个至少32字节的随机值>
JAVA_OPTS=-Xms128m -Xmx384m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError

# 2 GB 小型服务器的 MySQL 保守配置
MYSQL_INNODB_BUFFER_POOL_SIZE=128M
MYSQL_MAX_CONNECTIONS=50
MYSQL_TABLE_OPEN_CACHE=400
MYSQL_TABLE_OPEN_CACHE_INSTANCES=1

# 仅允许宿主机访问容器入口
BACKEND_PORT=127.0.0.1:8080
FRONTEND_PORT=127.0.0.1:8081

# 必须与浏览器地址完全一致，不要使用通配符
CORS_ALLOWED_ORIGINS=https://blog.example.com

# 小型部署可使用 local；生产对象存储使用 oss
STORAGE_TYPE=local

OSS_ENDPOINT=
OSS_REGION=
OSS_BUCKET=
OSS_ACCESS_KEY_ID=
OSS_ACCESS_KEY_SECRET=
```

将 `blog.example.com` 替换为真实域名。

### 5.2 环境变量说明

| 变量 | 必填 | 说明 |
|---|---:|---|
| `DB_PASSWORD` | 是 | MySQL root 密码，使用随机强密码 |
| `DB_NAME` | 否 | 数据库名，默认 `codenow` |
| `DB_SSL_MODE` | 否 | 数据库 TLS 模式，生产默认 `REQUIRED` |
| `MYSQL_INNODB_BUFFER_POOL_SIZE` | 否 | InnoDB 缓冲池；2 GB 服务器建议 `128M` |
| `MYSQL_MAX_CONNECTIONS` | 否 | MySQL 最大连接数；小型博客建议 `50`，扩容前先观察 `Max_used_connections` |
| `MYSQL_TABLE_OPEN_CACHE` | 否 | 打开表缓存；小型博客建议 `400` |
| `MYSQL_TABLE_OPEN_CACHE_INSTANCES` | 否 | 表缓存实例数；小型服务器建议 `1` |
| `REDIS_PASSWORD` | 是 | Redis 密码 |
| `JWT_SECRET` | 是 | JWT HMAC 密钥，至少 32 字节 |
| `JAVA_OPTS` | 否 | Java 运行参数；2 GB 服务器建议 `-Xms128m -Xmx384m` |
| `BACKEND_PORT` | 否 | 后端宿主机绑定，推荐 `127.0.0.1:8080` |
| `FRONTEND_PORT` | 否 | 前端宿主机绑定；HTTPS 反代时推荐 `127.0.0.1:8081` |
| `CORS_ALLOWED_ORIGINS` | 是 | 浏览器实际来源，多个来源使用英文逗号分隔 |
| `STORAGE_TYPE` | 是 | `local` 或 `oss` |
| `OSS_*` | 使用 OSS 时 | OSS Endpoint、Region、Bucket 和凭据 |

`.env` 已被 Git 忽略，禁止提交到仓库、聊天记录或工单。

## 6. 首次启动

先检查 Compose 配置是否可以正确解析：

```bash
docker compose config --quiet
```

构建并启动：

```bash
docker compose up -d --build
```

查看状态：

```bash
docker compose ps -a
```

预期状态：

| 服务 | 预期状态 |
|---|---|
| `mysql` | `healthy` |
| `redis` | `healthy` |
| `backend` | `Up` |
| `frontend` | `healthy` |
| `storage-init` | `Exited (0)`，属于正常状态 |

首次创建 `mysql_data` 数据卷时，MySQL 会自动执行 `codenow-backend/init.sql`。修改 `init.sql` 不会自动更新已有数据卷。

查看启动日志：

```bash
docker compose logs --tail=100 mysql
docker compose logs --tail=100 redis
docker compose logs --tail=100 backend
docker compose logs --tail=100 frontend
```

## 7. 配置域名与 HTTPS

### 7.1 安装宿主机 Nginx 和 Certbot

```bash
sudo apt update
sudo apt install -y nginx certbot python3-certbot-nginx
```

### 7.2 创建反向代理配置

```bash
sudo nano /etc/nginx/sites-available/codenow
```

写入：

```nginx
server {
    listen 80;
    listen [::]:80;
    server_name blog.example.com;

    client_max_body_size 10m;

    location / {
        proxy_pass http://127.0.0.1:8081;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

启用配置：

```bash
sudo ln -s /etc/nginx/sites-available/codenow /etc/nginx/sites-enabled/codenow
sudo nginx -t
sudo systemctl reload nginx
```

申请并自动配置证书：

```bash
sudo certbot --nginx -d blog.example.com
sudo certbot renew --dry-run
```

部署完成后访问：

```text
博客前台：https://blog.example.com/blog
管理后台：https://blog.example.com
```

## 8. 修改默认管理员密码

初始账号为 `admin / 123456`。公网开放前必须更换密码。

在 Ubuntu 安装 BCrypt 生成工具：

```bash
sudo apt install -y apache2-utils
```

生成 BCrypt 哈希并更新数据库：

```bash
read -s -p "New admin password: " ADMIN_PASSWORD
echo
ADMIN_HASH=$(htpasswd -bnBC 12 "" "$ADMIN_PASSWORD" | tr -d ':\n')
printf "UPDATE sys_user SET password='%s' WHERE username='admin';\n" "$ADMIN_HASH" \
  | docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
unset ADMIN_PASSWORD ADMIN_HASH
```

更新后使用新密码重新登录验证。

## 9. 部署验收

### 9.1 基础检查

```bash
curl --fail --show-error --silent https://blog.example.com/blog > /dev/null
curl --fail --show-error --silent \
  'https://blog.example.com/api/blog/articles?pageNum=1&pageSize=1'
```

### 9.2 完整业务冒烟

冒烟脚本会创建测试分类、标签、文章、待审核评论和上传文件：

```bash
BACKEND_URL=http://127.0.0.1:8080 \
FRONTEND_URL=http://127.0.0.1:8081 \
ADMIN_USERNAME=admin \
ADMIN_PASSWORD='<新管理员密码>' \
bash scripts/smoke-test.sh
```

预期输出：

```text
smoke test passed: article=<ID> upload=<URL>
```

冒烟数据会保留在数据库中，验收后可在管理后台删除。

## 10. 数据备份

Docker 官方建议对持久化卷建立备份和恢复流程：[Volumes backup and restore](https://docs.docker.com/engine/storage/volumes/)。

### 10.1 MySQL 备份

```bash
cd /opt/codenow
mkdir -p backups
chmod 700 backups

docker compose exec -T mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers "$MYSQL_DATABASE"' \
  > "backups/codenow-$(date +%Y%m%d-%H%M%S).sql"

test -s "$(ls -t backups/codenow-*.sql | head -n 1)"
```

必须把备份同步到服务器之外的对象存储或备份服务器，并定期进行恢复抽检。

### 10.2 本地上传文件备份

仅当 `STORAGE_TYPE=local` 时需要备份上传卷：

```bash
docker run --rm \
  -v codenow_uploads_data:/data:ro \
  -v "$PWD/backups:/backup" \
  alpine:3.22 \
  tar czf "/backup/uploads-$(date +%Y%m%d-%H%M%S).tar.gz" -C /data .
```

如果 Compose 项目目录不是 `codenow`，先用以下命令确认真实卷名：

```bash
docker volume ls
```

### 10.3 恢复 MySQL

恢复会覆盖现有数据，必须在维护窗口执行：

```bash
docker compose stop backend frontend

docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  < backups/<备份文件>.sql

docker compose start backend frontend
```

恢复后必须重新运行核心冒烟测试。

### 10.4 从本地 MySQL 同步业务数据

如果生产环境已经修改管理员密码，不要直接用本地数据库整体覆盖生产库。可只迁移以下业务表，保留生产环境的 `sys_user` 和 `sys_operation_log`：

```text
blog_category
blog_tag
blog_article
blog_article_tag
blog_comment
```

在 Windows 本地使用 `--result-file` 生成 UTF-8 数据文件，避免 PowerShell 重定向改变 SQL 文件编码：

```powershell
mysqldump --default-character-set=utf8mb4 -uroot -p `
  --single-transaction --no-create-info --complete-insert `
  --skip-triggers --set-gtid-purged=OFF --no-tablespaces `
  --result-file="C:\path\codenow-business-data.sql" `
  codenow blog_category blog_tag blog_article blog_article_tag blog_comment

Get-FileHash "C:\path\codenow-business-data.sql" -Algorithm SHA256
```

上传后必须在服务器再次计算 SHA-256，并在导入前完成以下检查：

1. `INSERT INTO` 只涉及上述五张表。
2. 文件不包含 `DROP`、`CREATE`、`DELETE`、`UPDATE`、`REPLACE` 或 `TRUNCATE`。
3. 本地文章的 `author_id` 在生产 `sys_user` 中存在。
4. 创建生产库即时回滚备份并确认文件非空。
5. 停止后端，删除旧业务数据后导入；导入失败时保持后端停止并恢复备份。

新版 `mysqldump` 可能输出 `LOCK TABLES`、`UNLOCK TABLES` 和 `ALTER TABLE ... DISABLE/ENABLE KEYS`。这些语句会干扰事务边界，应先从用于导入的副本中移除，再把业务表清理与导入包装在同一事务中。原始导出文件必须保留，用于审计和重新生成。

导入后检查五张业务表行数、管理员数量和中文编码。终端显示 `????` 不一定代表数据损坏，可用以下查询确认 UTF-8 字节：

```sql
SELECT id, HEX(title), CHAR_LENGTH(title), LENGTH(title)
FROM blog_article ORDER BY id LIMIT 3;
```

正常中文的 `HEX(title)` 会包含大量以 `E` 开头的 UTF-8 字节，且 `LENGTH(title)` 大于 `CHAR_LENGTH(title)`。最后清理应用使用的 Redis 数据库、启动后端并通过只读文章接口验收；不要运行会再次创建测试文章的完整冒烟脚本。

## 11. 版本升级

### 11.1 升级前

1. 确认新版本 CI 全部通过。
2. 记录当前生产 Tag 或提交号。
3. 完成 MySQL 和本地上传文件备份。
4. 阅读新版本迁移说明。

### 11.2 执行升级

```bash
cd /opt/codenow
git fetch --tags
git checkout <新版本Tag>
docker compose config --quiet
docker compose build
```

已有数据库需要在新后端启动前执行幂等迁移：

```bash
docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  < codenow-backend/migration-medium-priority.sql
```

重新创建应用容器：

```bash
docker compose up -d
docker compose ps -a
```

最后执行健康检查和业务冒烟。

Docker 官方建议生产环境使用独立配置并在更新代码后重建、重新创建对应服务：[Use Compose in production](https://docs.docker.com/compose/how-tos/production/)。

## 12. 回滚方案

### 12.1 应用回滚

```bash
cd /opt/codenow
git checkout <上一稳定版本Tag>
docker compose up -d --build
```

### 12.2 数据库结构回滚

只有上一应用版本不兼容新字段时，才执行结构回滚：

```bash
docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  < codenow-backend/rollback-medium-priority.sql
```

推荐顺序：

1. 停止外部写流量。
2. 备份当前数据库。
3. 执行数据库回滚脚本。
4. 切换到上一稳定 Tag。
5. 启动并运行冒烟测试。
6. 若失败，从升级前全库备份恢复。

禁止使用以下命令代替回滚：

```bash
docker compose down -v
```

`-v` 会删除 MySQL、Redis 和本地上传数据卷。

## 13. 常用运维命令

```bash
# 服务状态
docker compose ps -a

# 实时日志
docker compose logs -f --tail=200 backend
docker compose logs -f --tail=200 frontend

# 重启单个服务
docker compose restart backend

# 更新后只重建应用服务
docker compose build backend frontend
docker compose up -d --no-deps backend frontend

# 停止但保留数据
docker compose down

# 查看磁盘占用
docker system df
docker volume ls

# 查看容器和宿主机内存
docker stats --no-stream
free -h
```

不要在未确认数据备份的情况下清理卷。

## 14. 故障排查

### 14.1 前端登录失败或博客没有数据

```bash
docker compose ps -a
docker compose logs --tail=200 backend
curl -i 'http://127.0.0.1:8080/api/blog/articles?pageNum=1&pageSize=1'
```

重点检查：

- MySQL、Redis 是否为 `healthy`
- `CORS_ALLOWED_ORIGINS` 是否与浏览器协议、域名、端口完全一致
- 已有数据库是否执行了最新迁移
- 前端反向代理是否能访问后端

### 14.2 上传返回 500

```bash
docker compose ps -a
docker compose logs --tail=200 backend
docker compose run --rm storage-init
docker compose restart backend
```

若使用 OSS，检查所有 `OSS_*` 配置和 Bucket 权限；若使用本地存储，确认 `uploads_data` 卷存在且磁盘未满。

### 14.3 前端容器不健康

```bash
docker compose logs --tail=200 frontend
docker compose exec frontend wget --spider http://127.0.0.1:80
```

### 14.4 数据库迁移失败

先停止继续发布，保留日志并确认备份可用：

```bash
docker compose logs --tail=200 mysql
docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SHOW COLUMNS FROM blog_article_tag;"'
```

不要在不清楚数据影响时手工删除表或数据卷。

### 14.5 2 GB 服务器的 MySQL 内存偏高

先检查实际峰值连接、打开表数量和容器状态，不要直接关闭 `performance_schema`：

```bash
docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "SHOW VARIABLES WHERE Variable_name IN (\"innodb_buffer_pool_size\", \"max_connections\", \"table_open_cache\", \"table_open_cache_instances\", \"performance_schema\"); SHOW GLOBAL STATUS WHERE Variable_name IN (\"Threads_connected\", \"Max_used_connections\", \"Open_tables\", \"Opened_tables\");"'
docker inspect codenow-mysql --format '状态={{.State.Status}} 重启次数={{.RestartCount}} OOM={{.State.OOMKilled}}'
docker stats --no-stream codenow-mysql
```

项目对 2 GB 服务器默认使用 128 MB InnoDB 缓冲池、50 个最大连接、400 个打开表缓存和 1 个表缓存实例。调整 `MYSQL_MAX_CONNECTIONS` 前，应保证它明显高于长期观测到的 `Max_used_connections`。这些参数只在 MySQL 容器启动时生效，修改后需要重建 MySQL 容器；数据保存在 `mysql_data` 卷中，不要添加 `-v`。

### 14.6 管理后台没有自动跳转登录页

浏览器会按来源保存 `localStorage`，重启容器或替换数据库不会自动删除 `http://localhost:5173`、公网 IP 或域名下的旧 Token。当前版本首次进入管理路由时会调用 `/api/auth/me` 校验 Token；后端返回 401/403 时，前端会清除 Token 并跳转到登录页，同时用 `redirect` 参数保留原目标页面。

检查浏览器当前来源是否残留 Token：

```javascript
localStorage.getItem('token')
```

需要手动清理时执行：

```javascript
localStorage.removeItem('token')
location.href = '/login'
```

如果认证接口因网络或服务启动过程暂时不可达，路由守卫不会删除可能仍有效的 Token；此时应先检查后端状态和 `/api/auth/me` 响应。后端正常但 Token 失效时，预期 HTTP 状态为 401；无权限时预期为 403，不应返回通用 500。

## 15. 上线检查清单

- [ ] 使用已通过 CI 的固定 Tag
- [ ] `.env` 权限为 `600`，且未提交 Git
- [ ] 数据库、Redis、JWT 使用互不相同的随机强密钥
- [ ] 默认管理员密码已修改
- [ ] `CORS_ALLOWED_ORIGINS` 只包含真实域名
- [ ] HTTPS 证书有效，自动续期测试成功
- [ ] MySQL、Redis 未暴露公网端口
- [ ] MySQL 备份和上传文件备份已复制到服务器之外
- [ ] 已完成备份恢复抽检
- [ ] Compose 服务状态符合预期
- [ ] 登录、文章、评论、上传冒烟测试通过
- [ ] 已记录当前版本、上一稳定版本和回滚负责人

完成以上检查后，才建议正式开放公网访问。
