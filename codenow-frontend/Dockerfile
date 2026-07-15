# 前端 Dockerfile（多阶段构建）
# 第一阶段：Node 构建
FROM node:22-alpine AS builder

WORKDIR /app

# 先复制依赖文件，利用 Docker 缓存层
COPY package.json package-lock.json ./
RUN npm ci

# 复制源码并构建
COPY . .
RUN npm run build

# 第二阶段：Nginx 运行
FROM nginx:alpine

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 复制构建产物
COPY --from=builder /app/dist /usr/share/nginx/html

# 确保 nginx 用户有权限访问静态文件
RUN chown -R nginx:nginx /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 以 nginx 用户运行（non-root）
USER nginx

CMD ["nginx", "-g", "daemon off;"]
