# Docker 安全与镜像仓库

## 容器安全原则

### 最小权限原则

容器应该以最小权限运行，避免使用 root 用户。

```dockerfile
# 创建非 root 用户
FROM node:18-alpine

# 创建应用用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

COPY package*.json ./
RUN npm ci --production

COPY . .

# 切换到非 root 用户
USER appuser

CMD ["node", "server.js"]
```

### 只读根文件系统

```yaml
# docker-compose.yml
services:
  app:
    image: myapp:latest
    read_only: true
    tmpfs:
      - /tmp
      - /var/run
    volumes:
      - app-data:/data
```

```bash
# Docker 命令行
docker run --read-only --tmpfs /tmp myapp:latest
```

### 资源限制

```yaml
# docker-compose.yml
services:
  app:
    image: myapp:latest
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
        reservations:
          cpus: '0.5'
          memory: 256M
    pids_limit: 100
    ulimits:
      nofile:
        soft: 65536
        hard: 65536
```

```bash
# Docker 命令行
docker run --cpus=1.0 --memory=512m --pids-limit=100 myapp:latest
```

## seccomp/AppArmor

### seccomp 配置

```json
{
  "defaultAction": "SCMP_ACT_ERRNO",
  "architectures": ["SCMP_ARCH_X86_64"],
  "syscalls": [
    {
      "names": ["accept", "bind", "listen", "socket"],
      "action": "SCMP_ACT_ALLOW"
    },
    {
      "names": ["read", "write", "close"],
      "action": "SCMP_ACT_ALLOW"
    }
  ]
}
```

```bash
# 使用自定义 seccomp 配置
docker run --security-opt seccomp=profile.json myapp:latest

# 禁用 seccomp（不推荐）
docker run --security-opt seccomp=unconfined myapp:latest
```

### AppArmor 配置

```bash
# /etc/apparmor.d/docker-custom
#include <tunables/global>

profile docker-custom flags=(attach_disconnected) {
  #include <abstractions/base>
  
  # 允许读取
  /r/** r,
  /etc/passwd r,
  /etc/group r,
  
  # 禁止写入敏感目录
  deny /etc/** w,
  deny /proc/** w,
  
  # 允许网络访问
  network inet stream,
  network inet dgram,
}
```

```bash
# 加载 AppArmor 配置
sudo apparmor_parser -r /etc/apparmor.d/docker-custom

# 使用 AppArmor 配置
docker run --security-opt apparmor=docker-custom myapp:latest
```

### Docker 默认安全配置

```json
// /etc/docker/daemon.json
{
  "icc": false,
  "userns-remap": "default",
  "no-new-privileges": true,
  "seccomp-profile": "/etc/docker/seccomp-profile.json",
  "log-driver": "json-file",
  "log-opts": {
    "max-size": "10m",
    "max-file": "3"
  }
}
```

## 镜像签名（Docker Content Trust）

### 启用 DCT

```bash
# 启用 Docker Content Trust
export DOCKER_CONTENT_TRUST=1

# 推送签名镜像
docker push myrepo/myimage:latest

# 拉取签名镜像
docker pull myrepo/myimage:latest
```

### 签名密钥管理

```bash
# 生成签名密钥
docker trust key generate my-signer

# 添加签名者
docker trust signer add --key my-signer.pub my-signer myrepo/myimage

# 查看签名信息
docker trust inspect --pretty myrepo/myimage

# 撤销签名
docker trust revoke myrepo/myimage:latest
```

### 自动化签名

```yaml
# GitHub Actions 中签名镜像
name: Sign Docker Image

on:
  push:
    tags: ['v*']

jobs:
  sign:
    runs-on: ubuntu-latest
    steps:
      - name: Sign image
        env:
          DOCKER_CONTENT_TRUST: 1
          DOCKER_CONTENT_TRUST_REPOSITORY_PASSPHRASE: ${{ secrets.DCT_PASSPHRASE }}
        run: |
          docker trust sign myrepo/myimage:${{ github.ref_name }}
```

## 私有镜像仓库（Harbor）

### Harbor 安装

```bash
# 下载 Harbor
wget https://github.com/goharbor/harbor/releases/download/v2.10.0/harbor-offline-installer-v2.10.0.tgz
tar xvf harbor-offline-installer-v2.10.0.tgz
cd harbor

# 复制并编辑配置
cp harbor.yml.tmpl harbor.yml
```

```yaml
# harbor.yml
hostname: harbor.example.com

# HTTPS 配置
https:
  port: 443
  certificate: /data/cert/server.crt
  private_key: /data/cert/server.key

# 管理员密码
harbor_admin_password: Harbor12345

# 数据库配置
database:
  password: root123
  max_idle_conns: 100
  max_open_conns: 900

# 数据存储
data_volume: /data

# 日志配置
log:
  level: info
  local:
    rotate_count: 50
    rotate_size: 200M
    location: /var/log/harbor
```

```bash
# 安装 Harbor
sudo ./install.sh --with-trivy --with-chartmuseum
```

### Harbor 使用

```bash
# 登录 Harbor
docker login harbor.example.com

# 推送镜像
docker tag myapp:latest harbor.example.com/myproject/myapp:latest
docker push harbor.example.com/myproject/myapp:latest

# 拉取镜像
docker pull harbor.example.com/myproject/myapp:latest
```

### Harbor API

```bash
# 创建项目
curl -X POST "https://harbor.example.com/api/v2.0/projects" \
  -H "Content-Type: application/json" \
  -u admin:Harbor12345 \
  -d '{"project_name": "myproject", "public": false}'

# 获取项目列表
curl "https://harbor.example.com/api/v2.0/projects" \
  -u admin:Harbor12345

# 获取镜像列表
curl "https://harbor.example.com/api/v2.0/projects/myproject/repositories" \
  -u admin:Harbor12345
```

### Harbor 复制策略

```yaml
# 配置镜像复制
# 从开发 Harbor 复制到生产 Harbor
source:
  url: https://dev-harbor.example.com
  username: admin
  password: Harbor12345
  
destination:
  url: https://prod-harbor.example.com
  username: admin
  password: Harbor12345

trigger:
  type: event_based
  
filters:
  - type: name
    value: myproject/myapp
  - type: tag
    value: v*
```

## 镜像漏洞扫描

### Trivy 扫描

```bash
# 安装 Trivy
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

### Harbor 集成扫描

```yaml
# Harbor 自动扫描配置
# 在项目设置中启用自动扫描
# 推送镜像时自动触发扫描

# 手动触发扫描
curl -X POST "https://harbor.example.com/api/v2.0/projects/myproject/repositories/myapp/artifacts/latest/scan" \
  -u admin:Harbor12345
```

### 扫描策略

```yaml
# GitHub Actions 扫描流水线
name: Security Scan

on:
  push:
    branches: [main]

jobs:
  scan:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Build image
        run: docker build -t myapp:${{ github.sha }} .
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: myapp:${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: '1'
      
      - name: Upload scan results
        uses: github/codeql-action/upload-sarif@v2
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'
```

### 漏洞管理

```bash
# 查看镜像漏洞报告
trivy image --format table myapp:latest

# 生成 SBOM（软件物料清单）
trivy image --format spdx-json --output sbom.json myapp:latest

# 使用 SBOM 扫描
trivy sbom sbom.json
```

## 容器逃逸防护

### 常见逃逸方式

1. **特权容器**：使用 `--privileged` 标志
2. **挂载宿主机目录**：挂载敏感目录
3. **内核漏洞**：利用内核漏洞逃逸
4. **容器配置错误**：不当的 capabilities 配置

### 防护措施

```dockerfile
# Dockerfile 安全配置
FROM node:18-alpine

# 不要使用 root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# 移除不必要的工具
RUN apk del wget curl

WORKDIR /app
COPY --chown=appuser:appgroup . .

USER appuser
CMD ["node", "server.js"]
```

```yaml
# docker-compose.yml 安全配置
services:
  app:
    image: myapp:latest
    
    # 不要使用特权模式
    # privileged: true
    
    # 只读文件系统
    read_only: true
    
    # 限制 capabilities
    cap_drop:
      - ALL
    cap_add:
      - NET_BIND_SERVICE
    
    # 禁止提权
    security_opt:
      - no-new-privileges:true
    
    # 限制系统调用
    seccomp_profile: ./seccomp-profile.json
    
    # AppArmor 配置
    # apparmor: docker-custom
    
    # 资源限制
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 512M
    
    # 临时文件系统
    tmpfs:
      - /tmp
      - /var/run
```

### 运行时安全监控

```yaml
# Falco 配置
# /etc/falco/falco_rules.yaml
- rule: Terminal shell in container
  desc: A shell was spawned in a container
  condition: >
    spawned_process and container and shell_procs and not user_expected_terminal_shell_in_container
  output: >
    A shell was spawned in a container (user=%user.name container_id=%container.id 
    container_name=%container.name shell=%proc.name parent=%proc.pname)
  priority: WARNING
  tags: [container, shell, mitre_execution]

- rule: Container sensitive file read
  desc: Read sensitive file in container
  condition: >
    open_read and container and sensitive_files
  output: >
    Sensitive file read in container (user=%user.name file=%fd.name container_id=%container.id)
  priority: WARNING
```

### 安全基线检查

```bash
# 使用 Docker Bench Security
docker run --rm --net host --pid host --userns host --cap-add audit_control \
  -v /etc:/etc:ro \
  -v /var/lib:/var/lib:ro \
  -v /var/run/docker.sock:/var/run/docker.sock:ro \
  docker/docker-bench-security
```

## 镜像仓库安全最佳实践

### 镜像签名验证

```json
// /etc/docker/daemon.json
{
  "content-trust": {
    "trust-pinning": {
      "root-keys": {
        "harbor.example.com/myproject/*": ["key-id"]
      }
    },
    "mode": "enforced"
  }
}
```

### 访问控制

```yaml
# Harbor RBAC 配置
# 项目角色：
# - 项目管理员
# - 维护人员
# - 开发人员
# - 访客

# 创建机器人账户
curl -X POST "https://harbor.example.com/api/v2.0/projects/myproject/robots" \
  -H "Content-Type: application/json" \
  -u admin:Harbor12345 \
  -d '{
    "name": "ci-robot",
    "duration": 30,
    "level": "project",
    "permissions": [
      {
        "namespace": "myproject",
        "kind": "project",
        "access": [
          {"resource": "repository", "action": "pull"},
          {"resource": "repository", "action": "push"}
        ]
      }
    ]
  }'
```

### 镜像扫描策略

```yaml
# Harbor 项目配置
# 1. 启用自动扫描
# 2. 设置漏洞严重级别阻止策略
# 3. 配置扫描器（Trivy）

# 阻止高危漏洞镜像
# 项目设置 -> 漏洞阻止策略 -> 高危/严重
```

### 仓库镜像

```bash
# 配置镜像加速器
# /etc/docker/daemon.json
{
  "registry-mirrors": [
    "https://mirror.example.com"
  ]
}

# 配置 Harbor 代理缓存
# 创建代理项目 -> 配置上游仓库
```

## 安全审计

### Docker 审计日志

```bash
# 启用 Docker 审计
# /etc/audit/rules.d/docker.rules
-w /usr/bin/docker -p wa -k docker
-w /var/lib/docker -p wa -k docker
-w /etc/docker -p wa -k docker
-w /usr/lib/systemd/system/docker.service -p wa -k docker
-w /usr/lib/systemd/system/docker.socket -p wa -k docker
-w /etc/docker/daemon.json -p wa -k docker
-w /var/run/docker.sock -p wa -k docker

# 重启审计服务
sudo systemctl restart auditd
```

### 容器活动监控

```bash
# 查看容器事件
docker events --since '2024-01-01' --until '2024-01-02'

# 查看容器详细信息
docker inspect <container_id>

# 查看容器资源使用
docker stats

# 查看容器进程
docker top <container_id>
```

### 安全合规检查

```yaml
# 定期安全扫描作业
name: Security Compliance

on:
  schedule:
    - cron: '0 2 * * *'  # 每天凌晨2点

jobs:
  compliance:
    runs-on: ubuntu-latest
    steps:
      - name: Run Docker Bench Security
        run: |
          docker run --rm --net host --pid host \
            -v /var/run/docker.sock:/var/run/docker.sock \
            docker/docker-bench-security > report.txt
      
      - name: Scan all images
        run: |
          for image in $(docker images --format '{{.Repository}}:{{.Tag}}'); do
            trivy image --severity HIGH,CRITICAL "$image"
          done
      
      - name: Upload reports
        uses: actions/upload-artifact@v4
        with:
          name: security-reports
          path: report.txt
```

## 总结

Docker 安全是一个多层次的体系，需要从以下方面综合考虑：

1. **镜像安全**：使用最小基础镜像、定期扫描漏洞、签名验证
2. **运行时安全**：最小权限、资源限制、安全配置
3. **仓库安全**：访问控制、扫描策略、审计日志
4. **网络安全**：容器网络隔离、加密通信
5. **编排安全**：Kubernetes 安全策略、RBAC 配置
