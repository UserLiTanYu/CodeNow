# CodeNow 已部署服务器更新流程

本文档用于把 CodeNow 新版本更新到已经完成首次部署的阿里云轻量应用服务器，不用于首次部署。

当前部署环境以本地《阿里云轻量服务器新手部署指南》的实际结果为准：

- Ubuntu 24.04 LTS，2 核 2 GB。
- 项目目录为 `/opt/codenow`。
- MySQL、Redis、Spring Boot 后端和 Vue/Nginx 前端均由 Docker Compose 管理。
- 前端绑定服务器 `80` 端口，后端绑定 `127.0.0.1:8080`。
- MySQL、Redis 和上传文件保存在 Docker 数据卷中。
- 当前处于 ICP 备案申请阶段，暂不切换域名、HTTPS 或宿主机 Nginx。

> 本次属于原服务器上的增量更新。不要重新克隆项目、不要重新创建 `.env`、不要重新初始化 MySQL，也不要执行 `docker compose down -v`。

## 1. 更新前提

本地修改必须已经提交并推送到 GitHub，服务器才能通过 `git pull` 获取。

本地检查：

```powershell
cd C:\Users\litan\Desktop\code\codenow
git status -sb
git log -3 --oneline --decorate
```

## 2. 登录服务器并检查现状

```powershell
ssh root@你的公网IP
```

登录后执行：

```bash
cd /opt/codenow
git status -sb
git branch --show-current
git rev-parse --short HEAD
docker compose ps -a
free -h
df -h /
```

继续更新前应确认：

- 当前分支为 `master`。
- 服务器 Git 工作区没有人工修改。
- MySQL 和 Redis 为 `healthy`。
- 系统盘空间充足。

如果服务器存在未提交修改，不要使用 `git reset --hard`，先确认修改来源。

## 3. 更新前完整备份

### 3.1 创建备份目录

```bash
cd /opt/codenow
BACKUP_TIME=$(date +%Y%m%d-%H%M%S)
BACKUP_DIR="$PWD/backups/update-$BACKUP_TIME"
mkdir -p "$BACKUP_DIR"
chmod 700 "$PWD/backups" "$BACKUP_DIR"
```

### 3.2 备份数据库

```bash
docker compose exec -T mysql sh -c \
  'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction --routines --triggers "$MYSQL_DATABASE"' \
  > "$BACKUP_DIR/codenow.sql"

test -s "$BACKUP_DIR/codenow.sql" && echo "数据库备份成功"
sha256sum "$BACKUP_DIR/codenow.sql"
```

只有看到“数据库备份成功”才能继续。

### 3.3 备份上传文件

```bash
docker volume ls | grep uploads

docker run --rm \
  -v codenow_uploads_data:/data:ro \
  -v "$BACKUP_DIR:/backup" \
  alpine:3.22 \
  tar czf /backup/uploads.tar.gz -C /data .

test -s "$BACKUP_DIR/uploads.tar.gz" && echo "上传文件备份成功"
sha256sum "$BACKUP_DIR/uploads.tar.gz"
```

如果真实上传卷名不是 `codenow_uploads_data`，将命令替换为 `docker volume ls` 显示的实际名称，不能创建同名空卷代替。

## 4. 拉取新版本

```bash
cd /opt/codenow
git fetch origin
git log --oneline HEAD..origin/master
```

保存旧版本号：

```bash
OLD_COMMIT=$(git rev-parse HEAD)
echo "$OLD_COMMIT" | tee "$BACKUP_DIR/old-commit.txt"
```

确认提交列表正确后更新：

```bash
git pull --ff-only origin master
git rev-parse --short HEAD
```

## 5. 补充上传限制配置

不要覆盖服务器原 `.env`。先检查：

```bash
grep -E '^(UPLOAD_MAX_FILE_SIZE|UPLOAD_MAX_REQUEST_SIZE)=' .env
```

如果没有结果，执行 `nano .env`，在末尾加入：

```env
UPLOAD_MAX_FILE_SIZE=25MB
UPLOAD_MAX_REQUEST_SIZE=26MB
```

保存后执行：

```bash
chmod 600 .env
```

当前 ICP 备案阶段继续保留原公网 IP 配置：

```env
FRONTEND_PORT=80
CORS_ALLOWED_ORIGINS=http://你的公网IP
FRONTEND_URL=http://你的公网IP
```

不要用 `.env.example` 覆盖生产 `.env`，否则数据库密码、JWT 密钥和 163 SMTP 授权码会丢失。

## 6. 执行数据库结构迁移

`init.sql` 只在 MySQL 数据卷首次创建时执行。已有服务器需要手动迁移。

先检查当前结构：

```bash
docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -e "
    SHOW COLUMNS FROM sys_user LIKE '\''status'\'';
    SHOW COLUMNS FROM sys_user LIKE '\''ban_reason'\'';
    SHOW COLUMNS FROM blog_category LIKE '\''parent_id'\'';
    SHOW COLUMNS FROM blog_article LIKE '\''sort'\'';
    SHOW TABLES LIKE '\''article_favorite'\'';
    SHOW TABLES LIKE '\''comment_like'\'';
    SHOW TABLES LIKE '\''user_notification'\'';
    SHOW TABLES LIKE '\''sys_login_log'\'';
  "'
```

注意：

- `migration-community-v1.sql` 和 `migration-community-v2.sql` 不是完全幂等脚本，只能在对应整组结构都不存在时执行一次。
- `migration-category-hierarchy.sql` 和 `migration-medium-priority.sql` 是幂等脚本。
- 如果同一组结构只存在一部分，应停止更新并检查数据库，不能直接重跑非幂等脚本。

当前服务器已经完成普通用户第一、二版迁移时，通常只需执行：

```bash
docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  < codenow-backend/migration-category-hierarchy.sql

docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  < codenow-backend/migration-medium-priority.sql
```

## 7. 是否同步新的学习目录

拉取代码不会自动把本地新建的分类树和学习文章目录写入服务器数据库。

`codenow-backend/seed-learning-catalog.sql` 会删除服务器现有文章、评论、收藏和部分相关通知，然后创建新的学习目录与占位文章。因此：

- 只更新功能和界面：跳过本节。
- 确定用新学习目录替换线上文章：确认第 3 节备份完整后，在维护时段执行。

```bash
docker compose stop backend

docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$MYSQL_DATABASE"' \
  < codenow-backend/seed-learning-catalog.sql

docker compose start backend
```

执行后检查：

```bash
docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$MYSQL_DATABASE" -e "
    SELECT COUNT(*) AS category_count FROM blog_category WHERE is_deleted=0;
    SELECT COUNT(*) AS article_count FROM blog_article WHERE is_deleted=0;
    SELECT id,name,parent_id FROM blog_category WHERE is_deleted=0 ORDER BY parent_id,sort,id LIMIT 20;
    SELECT id,title FROM blog_article WHERE is_deleted=0 ORDER BY id LIMIT 10;
  "'
```

这一步具有数据替换性质，不能默认执行。

## 8. 分阶段构建和更新容器

2 GB 服务器应分别构建后端和前端：

```bash
cd /opt/codenow
docker compose config --quiet
```

先更新后端：

```bash
docker compose build --pull=false backend
docker compose up -d --no-deps backend
docker compose logs --tail=200 backend
```

等待后端启动完成并检查接口：

```bash
curl --fail --show-error --silent \
  'http://127.0.0.1:8080/api/blog/articles?pageNum=1&pageSize=1'
```

接口正常后更新前端：

```bash
docker compose build --pull=false frontend
docker compose up -d --no-deps frontend
docker compose ps -a
```

如果基础镜像拉取超时，沿用首次部署指南中的 AWS ECR Public 镜像回退方案，不要执行 `docker compose pull`。

## 9. 更新后验收

服务器内检查：

```bash
curl -I http://127.0.0.1/
curl --fail --show-error --silent \
  'http://127.0.0.1:8080/api/blog/articles?pageNum=1&pageSize=1'

docker compose ps -a
docker stats --no-stream
docker inspect codenow-backend --format '状态={{.State.Status}} 重启次数={{.RestartCount}} OOM={{.State.OOMKilled}}'
```

如果备案阶段仍允许公网访问，通过 `http://你的公网IP` 验收；如果为了备案暂时关闭了 `80` 端口，只做服务器本机检查，不要擅自开放。

- [ ] 博客首页、分类页和文章详情正常。
- [ ] 管理员和普通用户登录跳转正确。
- [ ] 分类树和文章学习顺序正常。
- [ ] 评论、回复、点赞、通知和收藏正常。
- [ ] 默认头像与用户头像上传正常。
- [ ] `.md`、`.txt` 和 ZIP 文章包导入正常。
- [ ] 接近 25 MB 的 ZIP 不再出现 `413` 或 Network Error。
- [ ] 后台分页、筛选和时间格式正常。
- [ ] 163 邮箱验证码可以收到。

检查日志：

```bash
docker compose logs --tail=200 backend
docker compose logs --tail=100 frontend
```

## 10. 故障回滚

只回滚程序：

```bash
cd /opt/codenow
git switch --detach "$OLD_COMMIT"
docker compose build --pull=false backend
docker compose up -d --no-deps backend
docker compose build --pull=false frontend
docker compose up -d --no-deps frontend
docker compose ps -a
```

恢复跟踪主分支：

```bash
git switch master
```

只有迁移或学习目录导入破坏数据时才恢复数据库：

```bash
docker compose stop backend frontend

docker compose exec -T mysql sh -c \
  'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' \
  < "$BACKUP_DIR/codenow.sql"

docker compose start backend frontend
```

## 11. 禁止操作

```bash
docker compose down -v
docker volume rm <生产数据卷>
git reset --hard
```

- 不要覆盖服务器原 `.env`。
- 不要提交 `.env`、数据库备份或 SMTP 授权码。
- 不要在未备份时执行 `seed-learning-catalog.sql`。
- ICP 备案完成前不要在本次更新中提前切换域名和 HTTPS。

## 12. 本次更新速查

确认第一、二版数据库迁移已经完成，并且不替换线上文章时，执行顺序如下：

```bash
ssh root@你的公网IP
cd /opt/codenow

git status -sb
docker compose ps -a

# 按第3节备份数据库和上传文件

git fetch origin
git log --oneline HEAD..origin/master
git pull --ff-only origin master

# 按第5节补充上传大小配置
# 按第6节执行分类树和中优先级幂等迁移

docker compose config --quiet
docker compose build --pull=false backend
docker compose up -d --no-deps backend
curl --fail --show-error --silent \
  'http://127.0.0.1:8080/api/blog/articles?pageNum=1&pageSize=1'

docker compose build --pull=false frontend
docker compose up -d --no-deps frontend

docker compose ps -a
docker compose logs --tail=200 backend
```
