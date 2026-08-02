# 前端 CI/CD、Docker 部署与 Nginx 配置

## 前端 CI/CD 流水线

一条标准的前端 CI/CD 流水线包含四个阶段：

```
代码提交 → lint 检查 → 单元测试 → 构建打包 → 部署上线
```

每个阶段的职责：

| 阶段 | 工具 | 失败时的处理 |
|------|------|-------------|
| Lint | ESLint + Prettier + Stylelint | 阻止合并，提示修复 |
| Test | Vitest / Jest | 阻止合并，查看失败用例 |
| Build | Vite / Webpack | 阻止合并，查看构建错误 |
| Deploy | Docker + Nginx / Vercel | 自动回滚到上一版本 |

## GitHub Actions 前端工作流

一个完整的 GitHub Actions 前端 CI/CD 配置：

```yaml
name: Frontend CI/CD

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  quality:
    name: Lint & Test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: "npm"

      - name: Install dependencies
        run: npm ci

      - name: Run linters
        run: npm run lint:check

      - name: Run tests
        run: npm run test:coverage

      - name: Upload coverage
        uses: codecov/codecov-action@v4
        with:
          files: ./coverage/lcov.info

  build:
    name: Build
    needs: quality
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-node@v4
        with:
          node-version: "22"
          cache: "npm"

      - run: npm ci
      - run: npm run build

      - name: Upload build artifacts
        uses: actions/upload-artifact@v4
        with:
          name: dist
          path: dist/
          retention-days: 7

  deploy:
    name: Deploy
    needs: build
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    runs-on: ubuntu-latest
    environment: production
    steps:
      - uses: actions/checkout@v4

      - name: Download build artifacts
        uses: actions/download-artifact@v4
        with:
          name: dist
          path: dist/

      - name: Deploy to server
        run: |
          # 通过 SSH 部署到服务器
          rsync -avz --delete dist/ deploy@server:/var/www/html/
```

### 依赖缓存

使用 `actions/cache` 缓存 `node_modules`，避免每次 CI 都重新安装依赖：

```yaml
- name: Cache node_modules
  uses: actions/cache@v4
  id: cache-node-modules
  with:
    path: node_modules
    key: ${{ runner.os }}-node-${{ hashFiles('package-lock.json') }}
    restore-keys: |
      ${{ runner.os }}-node-

- name: Install dependencies
  if: steps.cache-node-modules.outputs.cache-hit != 'true'
  run: npm ci
```

## Docker 多阶段构建

前端项目的典型 Docker 镜像包含两个阶段：Node 构建阶段和 Nginx 运行阶段。

```dockerfile
# ===== 构建阶段 =====
FROM node:22-alpine AS builder

WORKDIR /app

# 先复制依赖文件，利用 Docker 缓存层
COPY package.json package-lock.json ./
RUN npm ci --production=false

# 复制源码并构建
COPY . .
RUN npm run build

# ===== 运行阶段 =====
FROM nginx:1.27-alpine AS runner

# 复制自定义 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 从构建阶段复制产物
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

使用 `.dockerignore` 排除不必要的文件：

```
node_modules
dist
.git
*.md
.env.local
```

构建并运行：

```bash
docker build -t my-frontend:latest .
docker run -p 8080:80 my-frontend:latest
```

## Nginx 配置详解

### SPA 路由

前端 SPA 应用需要将所有路由指向 `index.html`，由前端路由库处理路径：

```nginx
server {
    listen 80;
    server_name example.com;
    root /usr/share/nginx/html;
    index index.html;

    # SPA 路由回退
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API 反向代理
    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 静态资源缓存
    location /assets/ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # 禁止访问隐藏文件
    location ~ /\. {
        deny all;
    }
}
```

### 反向代理

将 `/api` 请求转发到后端服务：

```nginx
location /api/ {
    proxy_pass http://127.0.0.1:8080/api/;

    # 传递真实客户端信息
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header X-Forwarded-Proto $scheme;

    # WebSocket 支持
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";

    # 超时设置
    proxy_connect_timeout 60s;
    proxy_read_timeout 120s;
    proxy_send_timeout 120s;
}
```

## Nginx 性能优化

### Gzip 压缩

```nginx
gzip on;
gzip_vary on;
gzip_proxied any;
gzip_comp_level 6;
gzip_min_length 1024;
gzip_types
    text/plain
    text/css
    text/javascript
    application/javascript
    application/json
    application/xml
    image/svg+xml;
```

### 缓存控制

```nginx
# HTML 文件 —— 不缓存，始终获取最新版本
location ~* \.html$ {
    add_header Cache-Control "no-cache, must-revalidate";
}

# 带 hash 的静态资源 —— 长期缓存
location ~* \.(js|css|woff2?|ttf|eot)$ {
    expires 1y;
    add_header Cache-Control "public, immutable";
}

# 图片资源
location ~* \.(png|jpg|jpeg|gif|webp|avif|ico)$ {
    expires 30d;
    add_header Cache-Control "public";
}

# ETag 开启（默认已开启）
etag on;
```

Vite 构建的产物文件名自带内容 hash（如 `app.a1b2c3.js`），非常适合配置长期缓存。HTML 不缓存，保证用户总能获取到最新的资源引用。

### HTTP/2

```nginx
server {
    listen 443 ssl http2;
    ssl_certificate /etc/nginx/ssl/cert.pem;
    ssl_certificate_key /etc/nginx/ssl/key.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
}
```

HTTP/2 的多路复用特性可以大幅减少资源加载的并行连接数，配合 Server Push 可以进一步优化首屏速度。

## Nginx 安全头

```nginx
# 防止点击劫持
add_header X-Frame-Options "SAMEORIGIN" always;

# 防止 MIME 类型嗅探
add_header X-Content-Type-Options "nosniff" always;

# XSS 过滤（现代浏览器已内置，但保留作为兜底）
add_header X-XSS-Protection "1; mode=block" always;

# 严格传输安全（HSTS）
add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;

# 内容安全策略
add_header Content-Security-Policy
    "default-src 'self'; script-src 'self' https://cdn.example.com; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' https://fonts.gstatic.com;" always;

# Referrer 策略
add_header Referrer-Policy "strict-origin-when-cross-origin" always;

# 权限策略
add_header Permissions-Policy "camera=(), microphone=(), geolocation=()" always;
```

Content-Security-Policy（CSP）需要根据项目实际情况配置，过于严格的策略可能导致页面功能异常。建议先使用 `Content-Security-Policy-Report-Only` 模式观察：

```nginx
add_header Content-Security-Policy-Report-Only
    "default-src 'self'; report-uri /api/csp-report;" always;
```

## 部署方案对比

| 特性 | 静态托管（Vercel/Netlify） | 容器化（Docker） | CDN |
|------|--------------------------|-----------------|-----|
| 部署难度 | 极低 | 中 | 中 |
| 自定义 Nginx | 不支持 | 完全控制 | 有限 |
| API 代理 | 需要额外配置 | 原生支持 | 不支持 |
| 费用 | 免费额度充足 | 服务器费用 | 按流量计费 |
| 适用场景 | 个人项目、文档站 | 企业项目 | 静态资源加速 |
| 国内访问 | 较慢 | 取决于服务器 | 快 |

## 蓝绿部署与金丝雀发布

### 蓝绿部署

维护两套完全相同的生产环境，切换流量实现零停机部署：

```nginx
upstream blue {
    server 10.0.0.1:3000;
}
upstream green {
    server 10.0.0.2:3000;
}

# 通过符号链接切换
# ln -s /etc/nginx/upstream-active.conf /etc/nginx/upstream-blue.conf
# 或 ln -s /etc/nginx/upstream-active.conf /etc/nginx/upstream-green.conf

include /etc/nginx/upstream-active.conf;

server {
    location / {
        proxy_pass http://active_backend;
    }
}
```

### 金丝雀发布（灰度发布）

按比例逐步将流量切换到新版本：

```nginx
upstream backend {
    server 10.0.0.1:3000 weight=90;  # 旧版本 90% 流量
    server 10.0.0.2:3000 weight=10;  # 新版本 10% 流量
}
```

也可以根据 Cookie 或 Header 灰度：

```nginx
map $cookie_canary $backend {
    "true" canary_backend;
    default stable_backend;
}

server {
    location / {
        proxy_pass http://$backend;
    }
}
```

## 环境变量管理

### 构建时变量

Vite 中的环境变量在构建时被替换：

```bash
# .env
VITE_API_BASE_URL=/api

# .env.production
VITE_API_BASE_URL=https://api.example.com
```

```js
const apiUrl = import.meta.env.VITE_API_BASE_URL;
```

### 运行时注入

构建时变量无法在部署后修改。如果需要在容器启动时注入配置（如不同环境的 API 地址），可以使用运行时注入方案：

```html
<!-- index.html 中预留占位 -->
<script>
  window.__ENV__ = {
    API_BASE_URL: "__API_BASE_URL__",
    SENTRY_DSN: "__SENTRY_DSN__",
  };
</script>
```

Docker 容器启动时替换：

```bash
#!/bin/sh
# entrypoint.sh
envsubst '${API_BASE_URL} ${SENTRY_DSN}' < /usr/share/nginx/html/index.html.template \
  > /usr/share/nginx/html/index.html

exec nginx -g 'daemon off;'
```

```dockerfile
COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]
```

代码中读取：

```js
const config = window.__ENV__ || {};
const apiUrl = config.API_BASE_URL || import.meta.env.VITE_API_BASE_URL;
```

## 版本管理

### 语义化版本

遵循 SemVer 规范：`MAJOR.MINOR.PATCH`

| 变更类型 | 版本号 | 示例 |
|---------|--------|------|
| 不兼容的 API 修改 | MAJOR +1 | 1.0.0 → 2.0.0 |
| 向后兼容的功能新增 | MINOR +1 | 1.0.0 → 1.1.0 |
| 向后兼容的问题修复 | PATCH +1 | 1.0.0 → 1.0.1 |

### Git Tag

```bash
# 打标签
git tag -a v1.2.0 -m "Release v1.2.0: 新增文章搜索功能"

# 推送标签
git push origin v1.2.0
```

### 自动生成 CHANGELOG

使用 `conventional-changelog` 或 `release-it` 自动生成：

```bash
# 安装 release-it
npm install -D release-it

# 执行发布（自动打 tag、生成 CHANGELOG、推送）
npx release-it
```

`release-it` 会根据 Conventional Commits 规范自动生成变更日志。确保团队遵循提交规范：

```
feat: 新增文章搜索功能
fix: 修复移动端侧边栏遮罩层问题
perf: 优化图片懒加载性能
docs: 更新部署文档
```

## 回滚策略

### 镜像标签回滚

使用 Docker 镜像标签管理版本，回滚只需切换标签：

```bash
# 当前版本
docker-compose.yml 中: image: my-frontend:v1.2.0

# 回滚到上一版本
docker-compose.yml 中: image: my-frontend:v1.1.0

# 重新部署
docker compose up -d
```

CI 中保留最近 N 个版本的镜像：

```yaml
- name: Build and push Docker image
  run: |
    docker build -t my-frontend:${{ github.sha }} .
    docker tag my-frontend:${{ github.sha }} my-frontend:latest
    docker push my-frontend:${{ github.sha }}
    docker push my-frontend:latest
```

### CDN 缓存刷新

回滚后需要刷新 CDN 缓存，否则用户可能继续加载到旧版本的资源：

```bash
# 阿里云 CDN 刷新
aliyun cdn RefreshObjectCaches --ObjectPath "https://example.com/assets/" --ObjectType Directory

# 腾讯云 CDN 刷新
tccli cdn PurgePathCache --Paths '["https://example.com/assets/"]' --flushType flush
```

Vite 构建的资源文件名包含 hash，理论上不需要刷新 CDN。但 HTML 文件需要确保不缓存或及时刷新：

```nginx
# HTML 不缓存
location ~* \.html$ {
    add_header Cache-Control "no-cache, must-revalidate";
    add_header X-Content-Type-Options "nosniff";
}
```

回滚操作流程：

```bash
# 1. 切换 Docker 镜像标签到上一版本
# 2. 重启容器
docker compose up -d

# 3. 刷新 CDN 缓存（如有）
# 4. 验证线上版本
curl -I https://example.com | grep X-App-Version
```

完整的回滚预案应该在每次发布前准备好，包括回滚命令、回滚负责人、验证清单。发布和回滚都不应该是临时手忙脚乱的操作。
