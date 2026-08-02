# 制品管理与发布策略

## 语义化版本（SemVer）

### 版本格式

语义化版本格式：`主版本号.次版本号.修订号`（MAJOR.MINOR.PATCH）

| 版本类型 | 何时递增 | 示例 |
|---------|---------|------|
| 主版本号（MAJOR） | 不兼容的 API 变更 | 1.0.0 → 2.0.0 |
| 次版本号（MINOR） | 向后兼容的功能新增 | 1.0.0 → 1.1.0 |
| 修订号（PATCH） | 向后兼容的问题修复 | 1.0.0 → 1.0.1 |

### 预发布版本

```bash
# Alpha 版本（内部测试）
1.0.0-alpha.1
1.0.0-alpha.2

# Beta 版本（外部测试）
1.0.0-beta.1
1.0.0-beta.2

# RC 版本（候选发布）
1.0.0-rc.1
1.0.0-rc.2

# 正式版本
1.0.0
```

### 版本比较规则

```bash
# 预发布版本的优先级
1.0.0-alpha < 1.0.0-alpha.1 < 1.0.0-beta < 1.0.0-beta.2 < 1.0.0-rc.1 < 1.0.0
```

### 自动版本管理

```json
// package.json
{
  "name": "my-app",
  "version": "1.0.0",
  "scripts": {
    "version:patch": "npm version patch",
    "version:minor": "npm version minor",
    "version:major": "npm version major",
    "version:prerelease": "npm version prerelease --preid=beta"
  }
}
```

```bash
# 使用 standard-version 自动生成版本和 CHANGELOG
npm install -D standard-version

# package.json
{
  "scripts": {
    "release": "standard-version",
    "release:minor": "standard-version --release-as minor",
    "release:major": "standard-version --release-as major",
    "release:prerelease": "standard-version --prerelease beta"
  }
}
```

## GitHub Release

### 手动创建 Release

```bash
# 使用 GitHub CLI
gh release create v1.0.0 \
  --title "Release v1.0.0" \
  --notes "Initial release" \
  ./dist/app-linux-amd64 \
  ./dist/app-darwin-amd64 \
  ./dist/app-windows-amd64.exe

# 创建预发布
gh release create v1.0.0-beta.1 \
  --prerelease \
  --title "Beta Release v1.0.0-beta.1" \
  --notes "Beta release for testing"

# 创建草稿
gh release create v1.0.0 \
  --draft \
  --title "Release v1.0.0" \
  --notes "Draft release"
```

### 自动化 Release

```yaml
# .github/workflows/release.yml
name: Create Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

jobs:
  release:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Generate changelog
        id: changelog
        run: |
          # 获取上一个标签
          PREV_TAG=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "")
          
          # 生成变更日志
          if [ -z "$PREV_TAG" ]; then
            CHANGELOG=$(git log --oneline --no-merges)
          else
            CHANGELOG=$(git log --oneline --no-merges ${PREV_TAG}..HEAD)
          fi
          
          # 写入多行输出
          echo "changelog<<EOF" >> $GITHUB_OUTPUT
          echo "$CHANGELOG" >> $GITHUB_OUTPUT
          echo "EOF" >> $GITHUB_OUTPUT
      
      - name: Create Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          body: |
            ## Changes
            ${{ steps.changelog.outputs.changelog }}
          draft: false
          prerelease: ${{ contains(github.ref, 'beta') || contains(github.ref, 'alpha') }}
```

### Release 资产管理

```yaml
jobs:
  build:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
        include:
          - os: ubuntu-latest
            artifact_name: app-linux-amd64
          - os: macos-latest
            artifact_name: app-darwin-amd64
          - os: windows-latest
            artifact_name: app-windows-amd64.exe
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Build
        run: |
          go build -o ${{ matrix.artifact_name }} ./cmd/app
      
      - name: Upload artifact
        uses: actions/upload-artifact@v4
        with:
          name: ${{ matrix.artifact_name }}
          path: ${{ matrix.artifact_name }}

  release:
    needs: build
    runs-on: ubuntu-latest
    
    steps:
      - name: Download artifacts
        uses: actions/download-artifact@v4
      
      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            app-linux-amd64
            app-darwin-amd64
            app-windows-amd64.exe
```

## Docker 镜像标签策略

### 标签类型

```bash
# 版本标签
docker tag myapp:latest myapp:1.0.0
docker tag myapp:latest myapp:1.0
docker tag myapp:latest myapp:1

# Git SHA 标签
docker tag myapp:latest myapp:abc1234

# 分支标签
docker tag myapp:latest myapp:main
docker tag myapp:latest myapp:develop

# 环境标签
docker tag myapp:latest myapp:staging
docker tag myapp:latest myapp:production

# 日期标签
docker tag myapp:latest myapp:20240101
```

### 自动化标签生成

```yaml
# .github/workflows/docker.yml
name: Docker Build and Push

on:
  push:
    branches: [main]
    tags: ['v*']
  pull_request:
    branches: [main]

jobs:
  docker:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Login to DockerHub
        if: github.event_name != 'pull_request'
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      
      - name: Docker meta
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: user/myapp
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=semver,pattern={{major}}
            type=sha
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```

### 多架构镜像

```yaml
jobs:
  docker:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up QEMU
        uses: docker/setup-qemu-action@v3
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Login to DockerHub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKERHUB_USERNAME }}
          password: ${{ secrets.DOCKERHUB_TOKEN }}
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          platforms: linux/amd64,linux/arm64,linux/arm/v7
          push: true
          tags: user/myapp:latest
```

## npm/Maven 私有仓库

### npm 私有仓库（Verdaccio）

```bash
# 安装 Verdaccio
npm install -g verdaccio

# 启动服务
verdaccio

# 配置 npm registry
npm set registry http://localhost:4873

# 发布包
npm publish

# 安装私有包
npm install @myorg/my-package
```

```yaml
# docker-compose.yml
version: '3.8'

services:
  verdaccio:
    image: verdaccio/verdaccio
    ports:
      - "4873:4873"
    volumes:
      - verdaccio_storage:/verdaccio/storage
      - ./config.yaml:/verdaccio/conf/config.yaml

volumes:
  verdaccio_storage:
```

```yaml
# config.yaml
storage: /verdaccio/storage
auth:
  htpasswd:
    file: /verdaccio/storage/htpasswd
uplinks:
  npmjs:
    url: https://registry.npmjs.org/
packages:
  '@myorg/*':
    access: $authenticated
    publish: $authenticated
  '**':
    access: $all
    publish: $authenticated
    proxy: npmjs
```

### Maven 私有仓库（Nexus）

```xml
<!-- settings.xml -->
<servers>
  <server>
    <id>nexus-releases</id>
    <username>admin</username>
    <password>${env.NEXUS_PASSWORD}</password>
  </server>
  <server>
    <id>nexus-snapshots</id>
    <username>admin</username>
    <password>${env.NEXUS_PASSWORD}</password>
  </server>
</servers>

<mirrors>
  <mirror>
    <id>nexus</id>
    <mirrorOf>*</mirrorOf>
    <url>http://nexus.example.com/repository/maven-public/</url>
  </mirror>
</mirrors>
```

```xml
<!-- pom.xml -->
<distributionManagement>
  <repository>
    <id>nexus-releases</id>
    <url>http://nexus.example.com/repository/maven-releases/</url>
  </repository>
  <snapshotRepository>
    <id>nexus-snapshots</id>
    <url>http://nexus.example.com/repository/maven-snapshots/</url>
  </snapshotRepository>
</distributionManagement>
```

### GitHub Packages

```yaml
# .github/workflows/publish.yml
name: Publish Package

on:
  release:
    types: [published]

jobs:
  publish-npm:
    runs-on: ubuntu-latest
    permissions:
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '20'
          registry-url: 'https://npm.pkg.github.com'
      - run: npm ci
      - run: npm publish
        env:
          NODE_AUTH_TOKEN: ${{ secrets.GITHUB_TOKEN }}

  publish-maven:
    runs-on: ubuntu-latest
    permissions:
      packages: write
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
      - name: Publish to GitHub Packages
        run: mvn deploy
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

## 制品晋级流程

### 制品晋级策略

```
开发环境 → 测试环境 → 预发布环境 → 生产环境
   ↓           ↓           ↓           ↓
 SNAPSHOT    RC版本      正式版本     正式版本
```

### 自动化晋级流程

```yaml
name: Artifact Promotion

on:
  workflow_dispatch:
    inputs:
      artifact-version:
        description: 'Artifact version to promote'
        required: true
      target-environment:
        description: 'Target environment'
        required: true
        type: choice
        options:
          - staging
          - production

jobs:
  promote:
    runs-on: ubuntu-latest
    environment: ${{ inputs.target-environment }}
    
    steps:
      - name: Pull artifact
        run: |
          docker pull registry.example.com/myapp:${{ inputs.artifact-version }}
      
      - name: Tag for environment
        run: |
          docker tag registry.example.com/myapp:${{ inputs.artifact-version }} \
            registry.example.com/myapp:${{ inputs.target-environment }}
      
      - name: Push to environment
        run: |
          docker push registry.example.com/myapp:${{ inputs.target-environment }}
      
      - name: Deploy
        run: |
          kubectl set image deployment/myapp \
            app=registry.example.com/myapp:${{ inputs.target-environment }}
```

### 制品元数据

```yaml
- name: Build with metadata
  run: |
    # 添加构建元数据
    cat > build-info.json << EOF
    {
      "version": "${{ github.ref_name }}",
      "commit": "${{ github.sha }}",
      "build_time": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
      "build_number": "${{ github.run_number }}",
      "branch": "${{ github.ref_name }}"
    }
    EOF
    
    # 将元数据打包到镜像
    docker build --build-arg BUILD_INFO="$(cat build-info.json)" -t myapp:${{ github.sha }} .
```

## 部署策略对比

### 蓝绿部署（Blue-Green Deployment）

```yaml
# 蓝绿部署配置
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
      version: blue
  template:
    metadata:
      labels:
        app: myapp
        version: blue
    spec:
      containers:
      - name: myapp
        image: myapp:1.0.0
---
apiVersion: v1
kind: Service
metadata:
  name: myapp
spec:
  selector:
    app: myapp
    version: blue  # 切换到 green 实现蓝绿部署
  ports:
  - port: 80
    targetPort: 8080
```

**优点：**
- 快速切换，回滚简单
- 零停机时间
- 生产环境完全隔离

**缺点：**
- 需要双倍资源
- 数据库迁移复杂

### 金丝雀发布（Canary Release）

```yaml
# 金丝雀部署配置
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-stable
spec:
  replicas: 9
  selector:
    matchLabels:
      app: myapp
      version: stable
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp-canary
spec:
  replicas: 1  # 10% 流量
  selector:
    matchLabels:
      app: myapp
      version: canary
---
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: myapp
spec:
  hosts:
  - myapp
  http:
  - route:
    - destination:
        host: myapp
        subset: stable
      weight: 90
    - destination:
        host: myapp
        subset: canary
      weight: 10
```

**优点：**
- 风险可控
- 可以逐步验证新版本
- 资源消耗合理

**缺点：**
- 流量控制复杂
- 需要监控和自动化

### 滚动更新（Rolling Update）

```yaml
# 滚动更新配置
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
spec:
  replicas: 10
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # 最多超出副本数
      maxUnavailable: 0   # 最多不可用副本数
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: myapp
        image: myapp:2.0.0
        readinessProbe:
          httpGet:
            path: /health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

**优点：**
- 资源效率高
- 自动化程度高
- Kubernetes 原生支持

**缺点：**
- 回滚相对较慢
- 新旧版本共存时间长

### 部署策略对比表

| 策略 | 停机时间 | 资源消耗 | 回滚速度 | 复杂度 | 适用场景 |
|------|---------|---------|---------|--------|---------|
| 蓝绿部署 | 零 | 2x | 极快 | 中 | 关键业务 |
| 金丝雀发布 | 零 | 1.1x | 快 | 高 | 渐进验证 |
| 滚动更新 | 零 | 1x | 中 | 低 | 常规部署 |
| 重建部署 | 有 | 1x | 慢 | 低 | 开发环境 |

### 自动化部署流水线

```yaml
name: Deploy

on:
  push:
    branches: [main]

jobs:
  # 构建和测试
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm ci && npm test && npm run build
      - uses: docker/build-push-action@v5
        with:
          push: true
          tags: myapp:${{ github.sha }}

  # 部署到预发布
  deploy-staging:
    needs: build
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - name: Deploy to staging
        run: |
          kubectl set image deployment/myapp \
            app=myapp:${{ github.sha }} -n staging
      
      - name: Run smoke tests
        run: |
          curl -f https://staging.example.com/health

  # 金丝雀发布到生产
  deploy-canary:
    needs: deploy-staging
    runs-on: ubuntu-latest
    environment: production-canary
    steps:
      - name: Deploy canary
        run: |
          kubectl set image deployment/myapp-canary \
            app=myapp:${{ github.sha }} -n production
      
      - name: Monitor canary
        run: |
          # 监控金丝雀指标
          sleep 300
          # 检查错误率、延迟等指标

  # 全量发布
  deploy-production:
    needs: deploy-canary
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy to production
        run: |
          kubectl set image deployment/myapp \
            app=myapp:${{ github.sha }} -n production
```
