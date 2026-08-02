# Dockerfile 最佳实践与多阶段构建

## Docker 镜像分层原理

Docker 镜像由多个只读层组成，每一层代表 Dockerfile 中的一条指令。理解分层机制是优化镜像的基础。

### 分层缓存机制

```dockerfile
# 层1: 基础镜像
FROM node:18

# 层2: 安装系统依赖
RUN apt-get update && apt-get install -y curl

# 层3: 复制 package.json
COPY package.json package-lock.json ./

# 层4: 安装 npm 依赖
RUN npm ci --production

# 层5: 复制源代码
COPY . .

# 层6: 构建应用
RUN npm run build
```

当修改源代码重新构建时，只有层5和层6会重新执行，层1-4会使用缓存。

### 缓存失效规则

| 指令 | 缓存行为 |
|------|---------|
| FROM | 基础镜像更新时失效 |
| COPY/ADD | 文件内容变化时失效 |
| RUN | 指令字符串变化时失效 |
| ENV | 指令字符串变化时失效 |
| EXPOSE/VOLUME/CMD/ENTRYPOINT | 不影响缓存 |

## 分层缓存优化

### 优化依赖安装

```dockerfile
# 不好的做法：每次修改代码都要重新安装依赖
FROM node:18
WORKDIR /app
COPY . .
RUN npm install
RUN npm run build

# 好的做法：利用缓存分层
FROM node:18
WORKDIR /app

# 先复制依赖声明文件
COPY package.json package-lock.json ./

# 安装依赖（这一层会被缓存）
RUN npm ci --production

# 再复制源代码
COPY . .

# 构建应用
RUN npm run build
```

### 合并 RUN 指令

```dockerfile
# 不好的做法：创建多个层
RUN apt-get update
RUN apt-get install -y curl
RUN apt-get install -y wget
RUN apt-get clean

# 好的做法：合并为一个层
RUN apt-get update && \
    apt-get install -y \
        curl \
        wget \
        vim \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*
```

### 使用 .dockerignore

```bash
# .dockerignore 文件
node_modules
npm-debug.log
.git
.gitignore
.env
.env.local
dist
build
coverage
.idea
.vscode
*.md
!README.md
Dockerfile
docker-compose.yml
.dockerignore
```

## 多阶段构建

多阶段构建是 Docker 17.05 引入的特性，允许在一个 Dockerfile 中使用多个 FROM 指令，最终只保留最后一个阶段的镜像。

### 基本多阶段构建

```dockerfile
# 阶段1: 构建阶段
FROM node:18 AS builder

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# 阶段2: 运行阶段
FROM node:18-slim AS runner

WORKDIR /app

# 从构建阶段复制产物
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./

# 暴露端口
EXPOSE 3000

# 启动应用
CMD ["node", "dist/main.js"]
```

### Java 应用多阶段构建

```dockerfile
# 阶段1: 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

# 先复制 pom.xml 缓存依赖
COPY pom.xml .
RUN mvn dependency:go-offline

# 复制源代码并构建
COPY src ./src
RUN mvn package -DskipTests

# 阶段2: 运行阶段
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# 安装必要的工具
RUN apk add --no-cache curl

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 从构建阶段复制 JAR
COPY --from=builder /app/target/*.jar app.jar

# 切换到非 root 用户
USER appuser

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Go 应用多阶段构建

```dockerfile
# 阶段1: 构建阶段
FROM golang:1.21-alpine AS builder

WORKDIR /app

# 安装依赖
COPY go.mod go.sum ./
RUN go mod download

# 复制源代码
COPY . .

# 编译（静态链接）
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/server ./cmd/server

# 阶段2: 运行阶段
FROM scratch

# 从构建阶段复制证书（HTTPS 需要）
COPY --from=builder /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/

# 从构建阶段复制可执行文件
COPY --from=builder /app/server /server

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["/server"]
```

### 前端应用多阶段构建

```dockerfile
# 阶段1: 构建前端资源
FROM node:18-alpine AS frontend-builder

WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ .
RUN npm run build

# 阶段2: Nginx 服务
FROM nginx:alpine

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

# 从构建阶段复制前端资源
COPY --from=frontend-builder /app/dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
```

## 非 root 用户运行

### 创建专用用户

```dockerfile
FROM node:18-alpine

# 创建应用用户和组
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci --production

COPY . .

# 更改文件所有权
RUN chown -R appuser:appgroup /app

# 切换到非 root 用户
USER appuser

EXPOSE 3000
CMD ["node", "server.js"]
```

### 使用特定 UID/GID

```dockerfile
FROM python:3.11-slim

# 使用固定的 UID/GID（便于权限管理）
ARG APP_UID=10001
ARG APP_GID=10001

RUN groupadd -g ${APP_GID} appgroup && \
    useradd -u ${APP_UID} -g ${APP_GID} -s /bin/bash appuser

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

USER appuser

CMD ["python", "app.py"]
```

## 健康检查配置

### 基本健康检查

```dockerfile
# HTTP 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8080/health || exit 1

# TCP 端口检查
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
    CMD nc -z localhost 3306 || exit 1

# 自定义脚本检查
COPY healthcheck.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/healthcheck.sh
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD /usr/local/bin/healthcheck.sh
```

### 健康检查脚本示例

```bash
#!/bin/bash
# healthcheck.sh

# 检查应用进程
if ! pgrep -f "node server.js" > /dev/null; then
    echo "应用进程未运行"
    exit 1
fi

# 检查 HTTP 服务
if ! curl -sf http://localhost:3000/health > /dev/null; then
    echo "HTTP 健康检查失败"
    exit 1
fi

# 检查数据库连接
if ! nc -z mysql 3306 2>/dev/null; then
    echo "数据库连接失败"
    exit 1
fi

echo "健康检查通过"
exit 0
```

## 镜像体积优化

### 选择合适的基础镜像

| 基础镜像 | 大小 | 适用场景 |
|---------|------|---------|
| node:18 | ~900MB | 开发环境 |
| node:18-slim | ~200MB | 生产环境 |
| node:18-alpine | ~170MB | 生产环境（推荐） |
| distroless | ~20-50MB | 最小化生产环境 |

### Alpine 镜像优化

```dockerfile
FROM node:18-alpine

# 安装必要的系统依赖
RUN apk add --no-cache \
    tini \
    curl \
    && rm -rf /var/cache/apk/*

# 使用 tini 作为 init 进程
ENTRYPOINT ["/sbin/tini", "--"]

CMD ["node", "server.js"]
```

### Distroless 镜像

```dockerfile
# 阶段1: 构建
FROM node:18 AS builder
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npm run build

# 阶段2: 使用 distroless 运行
FROM gcr.io/distroless/nodejs18-debian12
WORKDIR /app
COPY --from=builder /app/dist ./dist
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./
CMD ["dist/main.js"]
```

### 减少层数和清理缓存

```dockerfile
FROM python:3.11-slim

# 合并 RUN 指令并清理缓存
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        gcc \
        libpq-dev \
    && pip install --no-cache-dir -r requirements.txt \
    && apt-get purge -y --auto-remove gcc \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY . .

CMD ["python", "app.py"]
```

## 安全扫描

### 使用 Trivy 扫描镜像

```bash
# 安装 Trivy
brew install trivy  # macOS
# 或
curl -sfL https://raw.githubusercontent.com/aquasecurity/trivy/main/contrib/install.sh | sh

# 扫描镜像
trivy image myapp:latest

# 只显示高危和严重漏洞
trivy image --severity HIGH,CRITICAL myapp:latest

# 输出 JSON 格式
trivy image --format json --output result.json myapp:latest

# 扫描 Dockerfile
trivy config Dockerfile

# 在 CI 中使用
trivy image --exit-code 1 --severity HIGH,CRITICAL myapp:latest
```

### CI/CD 集成扫描

```yaml
# GitHub Actions 示例
name: Docker Build and Scan

on:
  push:
    branches: [main]

jobs:
  build-and-scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Build Docker image
        run: docker build -t myapp:${{ github.sha }} .
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: myapp:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'
      
      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v2
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'
```

### 使用 Docker Scout

```bash
# Docker Desktop 自带 Scout
docker scout cves myapp:latest

# 快速查看建议
docker scout recommendations myapp:latest

# 比较不同版本
docker scout compare myapp:v1.0 myapp:v2.0
```

## Dockerfile 最佳实践清单

### 安全最佳实践

```dockerfile
# 1. 使用特定版本标签，避免 latest
FROM node:18.17.0-alpine

# 2. 验证下载文件的校验和
RUN wget https://example.com/file.tar.gz && \
    echo "expected_checksum  file.tar.gz" | sha256sum -c - && \
    tar -xzf file.tar.gz

# 3. 不要在镜像中存储密钥
# 不好的做法
ENV API_KEY=secret123

# 好的做法：使用运行时注入
# docker run -e API_KEY=secret123 myapp

# 4. 使用 .env 文件
# .env 文件不进入镜像，通过 -env-file 参数传入

# 5. 多阶段构建分离构建工具
FROM golang:1.21 AS builder
# ... 构建过程 ...

FROM gcr.io/distroless/static
# 只包含最终产物
```

### 性能最佳实践

```dockerfile
# 1. 合理排序 COPY 指令（变化频率从低到高）
COPY package.json package-lock.json ./
RUN npm ci
COPY src/ ./src/
COPY public/ ./public/
COPY tsconfig.json ./

# 2. 使用 .dockerignore 减少构建上下文
# 排除 node_modules, .git, 测试文件等

# 3. 使用 BuildKit 缓存挂载
RUN --mount=type=cache,target=/root/.npm \
    npm ci --production

# 4. 使用多阶段构建减少最终镜像大小
```

### 维护性最佳实践

```dockerfile
# 1. 添加元数据标签
LABEL maintainer="team@example.com"
LABEL version="1.0.0"
LABEL description="My application description"

# 2. 使用 ARG 定义可配置参数
ARG NODE_VERSION=18
FROM node:${NODE_VERSION}-alpine

# 3. 添加注释说明复杂逻辑
# 安装 native dependencies for bcrypt
RUN apk add --no-cache python3 make g++ && \
    npm install bcrypt && \
    apk del python3 make g++

# 4. 使用 ENTRYPOINT 和 CMD 的正确组合
ENTRYPOINT ["node", "server.js"]
CMD ["--port=3000"]
```

## 镜像构建和推送流程

```bash
# 构建镜像
docker build -t myapp:1.0.0 .

# 多平台构建
docker buildx build --platform linux/amd64,linux/arm64 -t myapp:1.0.0 .

# 推送到 Docker Hub
docker tag myapp:1.0.0 username/myapp:1.0.0
docker push username/myapp:1.0.0

# 推送到私有仓库
docker tag myapp:1.0.0 registry.example.com/myapp:1.0.0
docker push registry.example.com/myapp:1.0.0

# 使用 BuildKit 缓存加速构建
DOCKER_BUILDKIT=1 docker build --cache-from myapp:latest -t myapp:1.0.0 .
```
