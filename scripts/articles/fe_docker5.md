# Docker Compose 编排与网络模式

## Docker Compose 简介

Docker Compose 是一个用于定义和运行多容器 Docker 应用的工具。通过一个 YAML 文件配置应用的服务、网络和卷，然后使用一条命令即可创建和启动所有服务。

### 适用场景

- 开发环境搭建
- 自动化测试
- 单机部署的微服务应用
- CI/CD 流水线中的集成测试

## docker-compose.yml 结构

### 基本结构

```yaml
# docker-compose.yml
version: "3.8"

services:
  web:
    build: ./web
    ports:
      - "8080:80"
    depends_on:
      - api
    networks:
      - frontend

  api:
    build: ./api
    environment:
      - DB_HOST=database
    depends_on:
      database:
        condition: service_healthy
    networks:
      - frontend
      - backend

  database:
    image: mysql:8.0
    volumes:
      - db_data:/var/lib/mysql
    networks:
      - backend

volumes:
  db_data:

networks:
  frontend:
  backend:
```

### 服务配置详解

```yaml
services:
  webapp:
    # 镜像来源
    image: nginx:alpine
    # 或者从 Dockerfile 构建
    build:
      context: .
      dockerfile: Dockerfile
      args:
        - NODE_ENV=production
    
    # 容器名称
    container_name: my-webapp
    
    # 端口映射
    ports:
      - "8080:80"      # host:container
      - "443:443"
      - "127.0.0.1:3000:3000"
    
    # 环境变量
    environment:
      - NODE_ENV=production
      - API_URL=http://api:3000
    # 或者从文件加载
    env_file:
      - .env
      - .env.production
    
    # 数据卷
    volumes:
      - ./html:/usr/share/nginx/html
      - nginx_conf:/etc/nginx/conf.d
    
    # 依赖服务
    depends_on:
      api:
        condition: service_healthy
    
    # 重启策略
    restart: unless-stopped
    
    # 网络
    networks:
      - frontend
    
    # 资源限制
    deploy:
      resources:
        limits:
          cpus: '0.50'
          memory: 512M
        reservations:
          cpus: '0.25'
          memory: 256M
    
    # 日志配置
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
```

## 服务依赖管理

### depends_on 基础用法

```yaml
services:
  web:
    image: nginx
    depends_on:
      - api
  
  api:
    image: node:18
    depends_on:
      - database
      - redis
  
  database:
    image: mysql:8.0
  
  redis:
    image: redis:alpine
```

### 带健康检查的依赖

```yaml
services:
  api:
    build: ./api
    depends_on:
      database:
        condition: service_healthy
      redis:
        condition: service_started
  
  database:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_DATABASE: myapp
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
  
  redis:
    image: redis:alpine
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
```

### 应用级别的健康检查

```yaml
services:
  api:
    build: ./api
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s
    depends_on:
      database:
        condition: service_healthy
```

### 启动顺序脚本

```yaml
services:
  api:
    build: ./api
    entrypoint: ["/bin/sh", "-c"]
    command:
      - |
        echo "等待数据库就绪..."
        until nc -z database 3306; do
          sleep 1
        done
        echo "数据库就绪，启动应用"
        node server.js
    depends_on:
      - database
```

## 网络模式

### Bridge 网络（默认）

```yaml
# 默认 bridge 网络
services:
  web:
    image: nginx
    # 自动连接到默认的 bridge 网络
  
  api:
    image: node:18
    # 同一网络中的服务可以通过服务名访问对方
    environment:
      - DB_HOST=database  # 使用服务名作为主机名
```

### 自定义网络

```yaml
services:
  web:
    image: nginx
    networks:
      - frontend
  
  api:
    image: node:18
    networks:
      - frontend
      - backend
  
  database:
    image: mysql:8.0
    networks:
      - backend

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true  # 内部网络，无法访问外部
```

### 网络配置详解

```yaml
networks:
  frontend:
    driver: bridge
    driver_opts:
      com.docker.network.bridge.name: br-frontend
    ipam:
      driver: default
      config:
        - subnet: 172.20.0.0/16
          gateway: 172.20.0.1
  
  backend:
    driver: bridge
    internal: true
    ipam:
      driver: default
      config:
        - subnet: 172.21.0.0/16
  
  # 使用已存在的外部网络
  existing-network:
    external: true
    name: my-existing-network
```

### Host 网络模式

```yaml
services:
  web:
    image: nginx
    network_mode: host
    # 直接使用宿主机网络，无需端口映射
    # ports 配置将被忽略
```

### Overlay 网络（Swarm 模式）

```yaml
services:
  web:
    image: nginx
    networks:
      - overlay-net
    deploy:
      replicas: 3

networks:
  overlay-net:
    driver: overlay
    attachable: true
```

## 数据卷管理

### Named Volumes

```yaml
services:
  database:
    image: mysql:8.0
    volumes:
      - db_data:/var/lib/mysql
    environment:
      MYSQL_ROOT_PASSWORD: rootpassword

volumes:
  db_data:
    driver: local
    # 使用特定的存储驱动选项
    driver_opts:
      type: none
      device: /path/to/data
      o: bind
```

### Bind Mounts

```yaml
services:
  web:
    image: nginx
    volumes:
      # 绝对路径
      - /host/path:/container/path
      # 相对路径（相对于 docker-compose.yml 所在目录）
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      # 带选项的挂载
      - ./data:/data:rw
      # 只读挂载
      - ./config:/etc/app/config:ro
```

### tmpfs 挂载

```yaml
services:
  app:
    image: node:18
    volumes:
      # tmpfs 挂载（内存中的临时文件系统）
      - type: tmpfs
        target: /app/tmp
        tmpfs:
          size: 100000000  # 100MB
```

### 数据卷示例

```yaml
services:
  # 数据库服务
  postgres:
    image: postgres:15
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    environment:
      POSTGRES_DB: myapp
      POSTGRES_USER: user
      POSTGRES_PASSWORD: password
  
  # Redis 服务
  redis:
    image: redis:alpine
    volumes:
      - redis_data:/data
    command: redis-server --appendonly yes
  
  # MinIO 对象存储
  minio:
    image: minio/minio
    volumes:
      - minio_data:/data
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local
  minio_data:
    driver: local
```

## 环境变量管理

### 直接定义环境变量

```yaml
services:
  api:
    image: node:18
    environment:
      NODE_ENV: production
      API_PORT: 3000
      DB_HOST: database
      DB_PORT: 5432
```

### 使用 .env 文件

```yaml
# docker-compose.yml
services:
  api:
    image: node:18
    env_file:
      - .env
      - .env.local
```

```bash
# .env 文件
NODE_ENV=production
API_PORT=3000
DB_HOST=database
DB_PORT=5432
DB_PASSWORD=secretpassword
```

### 变量替换

```yaml
# docker-compose.yml
services:
  api:
    image: node:${NODE_VERSION:-18}
    ports:
      - "${API_PORT:-3000}:3000"
    environment:
      - NODE_ENV=${NODE_ENV:-development}
```

### 多环境配置

```bash
# .env (默认)
COMPOSE_PROJECT_NAME=myapp
NODE_ENV=development

# .env.production
COMPOSE_PROJECT_NAME=myapp-prod
NODE_ENV=production
```

```yaml
# docker-compose.yml (基础配置)
services:
  api:
    image: node:18
    environment:
      - NODE_ENV=${NODE_ENV}

# docker-compose.prod.yml (生产环境覆盖)
services:
  api:
    restart: always
    deploy:
      resources:
        limits:
          memory: 512M
```

```bash
# 使用生产环境配置
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

## Compose Profiles

Profiles 允许根据不同的使用场景选择性地启动服务。

### 定义 Profiles

```yaml
services:
  # 基础服务（始终启动）
  database:
    image: mysql:8.0
    profiles: ["production", "development", "test"]
  
  redis:
    image: redis:alpine
    profiles: ["production", "development", "test"]
  
  # 应用服务
  api:
    build: ./api
    profiles: ["production", "development"]
  
  # 开发工具（仅开发环境）
  adminer:
    image: adminer
    profiles: ["development"]
    ports:
      - "8081:8080"
  
  mailhog:
    image: mailhog/mailhog
    profiles: ["development"]
    ports:
      - "1025:1025"
      - "8025:8025"
  
  # 测试服务
  test-runner:
    build: ./test
    profiles: ["test"]
```

### 使用 Profiles

```bash
# 只启动默认服务（没有定义 profile 的服务）
docker compose up -d

# 启动开发环境
docker compose --profile development up -d

# 启动测试环境
docker compose --profile test up -d

# 启动多个 profiles
docker compose --profile development --profile test up -d

# 通过环境变量设置 profiles
COMPOSE_PROFILES=development docker compose up -d
```

## 完整应用示例

### Spring Boot + MySQL + Redis + Nginx

```yaml
version: "3.8"

services:
  # Nginx 反向代理
  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/conf.d:/etc/nginx/conf.d:ro
      - ./nginx/ssl:/etc/nginx/ssl:ro
    depends_on:
      - frontend
      - api
    networks:
      - frontend
    restart: unless-stopped

  # Vue.js 前端
  frontend:
    build:
      context: ./frontend
      dockerfile: Dockerfile
    networks:
      - frontend
    restart: unless-stopped

  # Spring Boot API
  api:
    build:
      context: ./api
      dockerfile: Dockerfile
    environment:
      SPRING_PROFILES_ACTIVE: docker
      SPRING_DATASOURCE_URL: jdbc:mysql://database:3306/myapp?useSSL=false&serverTimezone=Asia/Shanghai
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PASSWORD: ${REDIS_PASSWORD}
    depends_on:
      database:
        condition: service_healthy
      redis:
        condition: service_healthy
    networks:
      - frontend
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s

  # MySQL 数据库
  database:
    image: mysql:8.0
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql:ro
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: myapp
      MYSQL_CHARACTER_SET_SERVER: utf8mb4
      MYSQL_COLLATION_SERVER: utf8mb4_unicode_ci
    networks:
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s

  # Redis 缓存
  redis:
    image: redis:alpine
    volumes:
      - redis_data:/data
    command: redis-server --requirepass ${REDIS_PASSWORD} --appendonly yes
    networks:
      - backend
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "redis-cli", "-a", "${REDIS_PASSWORD}", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  mysql_data:
    driver: local
  redis_data:
    driver: local

networks:
  frontend:
    driver: bridge
  backend:
    driver: bridge
    internal: true
```

### docker-compose.prod.yml 覆盖

```yaml
version: "3.8"

services:
  nginx:
    deploy:
      resources:
        limits:
          cpus: '0.50'
          memory: 256M
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"

  api:
    deploy:
      resources:
        limits:
          cpus: '1.00'
          memory: 1G
      replicas: 2
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "5"

  database:
    deploy:
      resources:
        limits:
          cpus: '2.00'
          memory: 4G
```

## Compose 命令参考

### 生命周期命令

```bash
# 启动所有服务
docker compose up -d

# 启动并重新构建
docker compose up -d --build

# 停止所有服务
docker compose down

# 停止并删除卷（慎用！）
docker compose down -v

# 停止并删除镜像
docker compose down --rmi all

# 重启服务
docker compose restart

# 重启特定服务
docker compose restart api
```

### 查看状态

```bash
# 查看运行中的服务
docker compose ps

# 查看服务日志
docker compose logs

# 实时查看日志
docker compose logs -f

# 查看特定服务日志
docker compose logs -f api

# 查看最后 100 行日志
docker compose logs --tail=100 api
```

### 执行命令

```bash
# 在运行中的容器中执行命令
docker compose exec api bash

# 执行单次命令
docker compose exec database mysql -u root -p

# 运行一次性容器
docker compose run --rm api npm test
```

### 扩缩容

```bash
# 扩展服务实例数
docker compose up -d --scale api=3

# 注意：使用 scale 时不能指定固定端口
# 需要使用端口范围或让 Docker 自动分配
```

## Compose 文件版本差异

| 特性 | v2 | v3 |
|-----|-----|-----|
| version | "2.x" | "3.x" |
| depends_on 条件 | 支持 | 不支持（3.0-3.3），支持（3.4+） |
| deploy 配置 | 不支持 | 支持 |
| resources 限制 | 不支持 | 支持 |
| Swarm 模式 | 不支持 | 支持 |
| profiles | 不支持 | 支持（3.9+） |

## 常见问题解决

### 服务启动顺序问题

```yaml
# 使用 depends_on + healthcheck 确保依赖服务就绪
services:
  api:
    depends_on:
      database:
        condition: service_healthy
  
  database:
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 30s
```

### 网络连通性问题

```bash
# 查看网络
docker network ls

# 检查网络详情
docker network inspect myapp_frontend

# 在容器中测试网络连通性
docker compose exec api ping database
docker compose exec api nc -zv database 3306
```

### 数据持久化问题

```bash
# 查看卷
docker volume ls

# 检查卷详情
docker volume inspect myapp_mysql_data

# 备份卷数据
docker compose exec database mysqldump -u root -p myapp > backup.sql

# 恢复数据
docker compose exec -T database mysql -u root -p myapp < backup.sql
```
