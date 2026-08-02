# Docker Compose 全栈部署实战

容器化部署是现代应用交付的标准方式。Docker Compose让我们可以用一个配置文件定义和运行多容器应用，极大简化了开发、测试和生产环境的部署流程。本文将详细介绍如何使用Docker Compose编排一个完整的博客系统。

## 项目结构

```
codenow/
├── docker-compose.yml          # 主编排文件
├── .env                        # 环境变量
├── codenow-backend/
│   ├── Dockerfile              # 后端镜像
│   └── ...
├── codenow-frontend/
│   ├── Dockerfile              # 前端镜像
│   └── ...
├── nginx/
│   ├── nginx.conf              # Nginx配置
│   └── conf.d/
│       └── default.conf        # 站点配置
├── mysql/
│   ├── init.sql                # 初始化脚本
│   └── conf/
│       └── my.cnf              # MySQL配置
└── scripts/
    └── deploy.sh               # 部署脚本
```

## docker-compose.yml

```yaml
version: '3.8'

services:
  # MySQL数据库
  mysql:
    image: mysql:8.0
    container_name: codenow-mysql
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: codenow
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
      TZ: Asia/Shanghai
    ports:
      - "${DB_PORT:-3306}:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./mysql/init.sql:/docker-entrypoint-initdb.d/init.sql
      - ./mysql/conf/my.cnf:/etc/mysql/conf.d/my.cnf
    networks:
      - codenow-network
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-p${DB_PASSWORD}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # Redis缓存
  redis:
    image: redis:7-alpine
    container_name: codenow-redis
    restart: unless-stopped
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    environment:
      TZ: Asia/Shanghai
    ports:
      - "${REDIS_PORT:-6379}:6379"
    volumes:
      - redis_data:/data
    networks:
      - codenow-network
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # 后端应用
  backend:
    build:
      context: ./codenow-backend
      dockerfile: Dockerfile
    container_name: codenow-backend
    restart: unless-stopped
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/codenow?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_DATA_REDIS_HOST: redis
      SPRING_DATA_REDIS_PASSWORD: ${REDIS_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      STORAGE_TYPE: ${STORAGE_TYPE:-local}
      UPLOAD_PATH: /uploads
      TZ: Asia/Shanghai
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    volumes:
      - upload_data:/uploads
    networks:
      - codenow-network
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # 前端应用
  frontend:
    build:
      context: ./codenow-frontend
      dockerfile: Dockerfile
      args:
        VITE_API_BASE_URL: /api
    container_name: codenow-frontend
    restart: unless-stopped
    networks:
      - codenow-network
    depends_on:
      - backend

  # Nginx反向代理
  nginx:
    image: nginx:alpine
    container_name: codenow-nginx
    restart: unless-stopped
    ports:
      - "${NGINX_PORT:-80}:80"
      - "${NGINX_SSL_PORT:-443}:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf
      - ./nginx/conf.d:/etc/nginx/conf.d
      - ./nginx/ssl:/etc/nginx/ssl
      - upload_data:/uploads
    networks:
      - codenow-network
    depends_on:
      - backend
      - frontend

volumes:
  mysql_data:
    driver: local
  redis_data:
    driver: local
  upload_data:
    driver: local

networks:
  codenow-network:
    driver: bridge
```

## 环境变量配置

```env
# .env
# 数据库配置
DB_PASSWORD=your_secure_password_here
DB_PORT=3306

# Redis配置
REDIS_PASSWORD=your_redis_password_here
REDIS_PORT=6379

# JWT密钥
JWT_SECRET=your_jwt_secret_key_at_least_32_chars

# 存储类型
STORAGE_TYPE=local

# 端口配置
BACKEND_PORT=8080
NGINX_PORT=80
NGINX_SSL_PORT=443

# CORS配置
CORS_ALLOWED_ORIGINS=http://localhost,https://codenow.com
```

## 后端Dockerfile

```dockerfile
# codenow-backend/Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# 复制Maven配置和依赖
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# 下载依赖（利用Docker缓存）
RUN ./mvnw dependency:go-offline -B

# 复制源代码并构建
COPY src src
RUN ./mvnw package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 安装必要工具
RUN apk add --no-cache curl tzdata

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制构建产物
COPY --from=builder /app/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /uploads

# JVM参数
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:+HeapDumpOnOutOfMemoryError"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

## 前端Dockerfile

```dockerfile
# codenow-frontend/Dockerfile
FROM node:22-alpine AS builder

ARG VITE_API_BASE_URL=/api

WORKDIR /app

# 复制依赖文件
COPY package.json package-lock.json ./

# 安装依赖
RUN npm ci

# 复制源代码
COPY . .

# 设置环境变量
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL

# 构建
RUN npm run build

# 运行阶段
FROM nginx:alpine

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 复制Nginx配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

## Nginx配置

```nginx
# nginx/nginx.conf
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log warn;
pid /var/run/nginx.pid;

events {
    worker_connections 1024;
    use epoll;
    multi_accept on;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;
    
    log_format main '$remote_addr - $remote_user [$time_local] "$request" '
                    '$status $body_bytes_sent "$http_referer" '
                    '"$http_user_agent" "$http_x_forwarded_for"';
    
    access_log /var/log/nginx/access.log main;
    
    sendfile on;
    tcp_nopush on;
    tcp_nodelay on;
    keepalive_timeout 65;
    types_hash_max_size 2048;
    
    # Gzip压缩
    gzip on;
    gzip_vary on;
    gzip_proxied any;
    gzip_comp_level 6;
    gzip_types text/plain text/css text/xml application/json application/javascript 
               application/xml application/xml+rss text/javascript application/x-javascript;
    
    # 包含站点配置
    include /etc/nginx/conf.d/*.conf;
}
```

```nginx
# nginx/conf.d/default.conf
server {
    listen 80;
    server_name localhost;
    
    # 安全头
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    
    # 前端静态文件
    location / {
        root /usr/share/nginx/html;
        index index.html;
        try_files $uri $uri/ /index.html;
    }
    
    # API代理
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # 超时设置
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # 缓冲设置
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }
    
    # 文件上传大小限制
    client_max_body_size 50m;
    
    # 静态资源缓存
    location ~* \.(jpg|jpeg|png|gif|ico|css|js|woff2|woff|ttf|svg)$ {
        root /usr/share/nginx/html;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
    
    # 上传文件访问
    location /uploads/ {
        alias /uploads/;
        expires 7d;
        add_header Cache-Control "public";
    }
    
    # 健康检查
    location /health {
        access_log off;
        return 200 "OK";
        add_header Content-Type text/plain;
    }
}
```

## MySQL配置

```ini
# mysql/conf/my.cnf
[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone='+08:00'

# InnoDB配置
innodb_buffer_pool_size=256M
innodb_log_file_size=64M
innodb_flush_log_at_trx_commit=1
innodb_lock_wait_timeout=50

# 连接配置
max_connections=200
max_connect_errors=100
wait_timeout=28800
interactive_timeout=28800

# 慢查询日志
slow_query_log=1
slow_query_log_file=/var/log/mysql/slow.log
long_query_time=2

[client]
default-character-set=utf8mb4

[mysql]
default-character-set=utf8mb4
```

## 部署脚本

```bash
#!/bin/bash
# scripts/deploy.sh

set -e

echo "=== 码上记博客系统部署脚本 ==="

# 检查.env文件
if [ ! -f .env ]; then
    echo "错误: .env文件不存在，请先创建.env文件"
    exit 1
fi

# 加载环境变量
source .env

# 检查必要变量
required_vars=("DB_PASSWORD" "REDIS_PASSWORD" "JWT_SECRET")
for var in "${required_vars[@]}"; do
    if [ -z "${!var}" ]; then
        echo "错误: 环境变量 $var 未设置"
        exit 1
    fi
done

# 创建必要目录
echo "创建目录..."
mkdir -p nginx/ssl
mkdir -p mysql/conf
mkdir -p uploads

# 停止旧容器
echo "停止旧容器..."
docker compose down

# 拉取最新镜像（可选）
if [ "$1" = "--pull" ]; then
    echo "拉取最新镜像..."
    docker compose pull
fi

# 构建并启动
echo "构建并启动服务..."
docker compose up -d --build

# 等待服务就绪
echo "等待服务启动..."
sleep 10

# 检查服务状态
echo "检查服务状态..."
docker compose ps

# 运行健康检查
echo "运行健康检查..."
max_retries=30
retry_count=0

while [ $retry_count -lt $max_retries ]; do
    if curl -f http://localhost:${NGINX_PORT:-80}/health > /dev/null 2>&1; then
        echo "健康检查通过！"
        break
    fi
    
    retry_count=$((retry_count + 1))
    echo "等待服务就绪... ($retry_count/$max_retries)"
    sleep 2
done

if [ $retry_count -eq $max_retries ]; then
    echo "错误: 服务启动超时"
    docker compose logs --tail=50
    exit 1
fi

echo ""
echo "=== 部署完成 ==="
echo "访问地址: http://localhost:${NGINX_PORT:-80}"
echo "API文档: http://localhost:${NGINX_PORT:-80}/doc.html"
echo ""
echo "查看日志: docker compose logs -f"
echo "停止服务: docker compose down"
```

## Windows部署脚本

```powershell
# scripts/deploy.ps1

Write-Host "=== 码上记博客系统部署脚本 ===" -ForegroundColor Green

# 检查.env文件
if (-not (Test-Path .env)) {
    Write-Host "错误: .env文件不存在" -ForegroundColor Red
    exit 1
}

# 加载环境变量
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], "Process")
    }
}

# 停止旧容器
Write-Host "停止旧容器..." -ForegroundColor Yellow
docker compose down

# 构建并启动
Write-Host "构建并启动服务..." -ForegroundColor Yellow
docker compose up -d --build

# 等待服务就绪
Write-Host "等待服务启动..." -ForegroundColor Yellow
Start-Sleep -Seconds 15

# 检查服务状态
Write-Host "服务状态:" -ForegroundColor Cyan
docker compose ps

Write-Host ""
Write-Host "=== 部署完成 ===" -ForegroundColor Green
Write-Host "访问地址: http://localhost:80"
```

## 部署验收脚本

```bash
#!/bin/bash
# scripts/smoke-test.sh

set -e

BASE_URL="${1:-http://localhost:80}"
echo "=== 部署验收测试 ==="
echo "测试地址: $BASE_URL"

# 测试健康检查
echo -n "测试健康检查... "
if curl -sf "$BASE_URL/health" > /dev/null; then
    echo "✓ 通过"
else
    echo "✗ 失败"
    exit 1
fi

# 测试首页
echo -n "测试首页访问... "
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL")
if [ "$HTTP_CODE" = "200" ]; then
    echo "✓ 通过"
else
    echo "✗ 失败 (HTTP $HTTP_CODE)"
    exit 1
fi

# 测试API
echo -n "测试API健康检查... "
HEALTH_RESPONSE=$(curl -sf "$BASE_URL/api/health")
if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    echo "✓ 通过"
else
    echo "✗ 失败"
    exit 1
fi

# 测试登录接口
echo -n "测试登录接口... "
LOGIN_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test","captcha":"test","captchaKey":"test"}')
if [ "$LOGIN_CODE" = "400" ] || [ "$LOGIN_CODE" = "401" ]; then
    echo "✓ 通过"
else
    echo "✗ 失败 (HTTP $LOGIN_CODE)"
    exit 1
fi

echo ""
echo "=== 所有验收测试通过 ==="
```

Docker Compose让全栈应用的部署变得简单可控。通过合理的服务编排、健康检查、数据持久化配置，可以确保应用在各种环境下稳定运行。关键是要注意生产环境的安全配置，包括密码管理、网络隔离和资源限制。
