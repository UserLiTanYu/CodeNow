# GitHub Actions 工作流进阶

## Workflow 语法详解

### 基本结构

```yaml
# .github/workflows/ci.yml
name: CI Pipeline

# 触发条件
on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]
  schedule:
    - cron: '0 2 * * 1'  # 每周一凌晨2点

# 环境变量
env:
  NODE_ENV: test
  REGISTRY: ghcr.io

# 权限设置
permissions:
  contents: read
  packages: write

# 工作流任务
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run tests
        run: npm test
```

### 触发条件详解

```yaml
on:
  # Push 事件
  push:
    branches:
      - main
      - 'release/**'
    tags:
      - 'v*'
    paths:
      - 'src/**'
      - '!src/**/*.test.js'
    paths-ignore:
      - '**.md'

  # Pull Request 事件
  pull_request:
    types: [opened, synchronize, reopened]
    branches:
      - main

  # 定时任务
  schedule:
    - cron: '0 2 * * *'    # 每天凌晨2点
    - cron: '0 */6 * * *'  # 每6小时

  # 手动触发
  workflow_dispatch:
    inputs:
      environment:
        description: '部署环境'
        required: true
        default: 'staging'
        type: choice
        options:
          - staging
          - production
      version:
        description: '版本号'
        required: true
        type: string

  # 其他工作流触发
  workflow_run:
    workflows: ["Build"]
    types: [completed]

  # Issue 事件
  issues:
    types: [opened, labeled]

  # Release 事件
  release:
    types: [published]
```

### 作业和步骤

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    
    # 作业级环境变量
    env:
      CI: true
    
    # 输出变量
    outputs:
      version: ${{ steps.version.outputs.version }}
    
    steps:
      # 使用 Action
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      # 运行命令
      - name: Get version
        id: version
        run: |
          VERSION=$(cat package.json | jq -r .version)
          echo "version=$VERSION" >> $GITHUB_OUTPUT
      
      # 条件步骤
      - name: Deploy
        if: github.ref == 'refs/heads/main' && success()
        run: echo "Deploying..."
      
      # 失败时执行
      - name: Cleanup
        if: failure()
        run: echo "Cleanup on failure"
      
      # 始终执行
      - name: Always run
        if: always()
        run: echo "Always run this step"
```

## 矩阵构建（Matrix）

### 基本矩阵

```yaml
jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest, macos-latest]
        node-version: [18, 20, 22]
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js ${{ matrix.node-version }}
        uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
      
      - run: npm ci
      - run: npm test
```

### 矩阵排除和包含

```yaml
jobs:
  test:
    runs-on: ${{ matrix.os }}
    strategy:
      matrix:
        os: [ubuntu-latest, windows-latest]
        node-version: [18, 20]
        exclude:
          - os: windows-latest
            node-version: 18
        include:
          - os: ubuntu-latest
            node-version: 22
            experimental: true
    
    continue-on-error: ${{ matrix.experimental || false }}
    
    steps:
      - uses: actions/checkout@v4
      - run: npm ci
      - run: npm test
```

### 矩阵中使用对象

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        include:
          - name: "Production"
            env: prod
            url: https://api.example.com
          - name: "Staging"
            env: staging
            url: https://staging-api.example.com
    
    steps:
      - name: Build for ${{ matrix.name }}
        run: |
          echo "Building for ${{ matrix.env }}"
          echo "API URL: ${{ matrix.url }}"
```

## 缓存策略

### npm 缓存

```yaml
steps:
  - uses: actions/checkout@v4
  
  - name: Setup Node.js
    uses: actions/setup-node@v4
    with:
      node-version: '20'
      cache: 'npm'
  
  - run: npm ci
```

### 自定义缓存

```yaml
steps:
  - uses: actions/checkout@v4
  
  - name: Cache node modules
    id: cache-npm
    uses: actions/cache@v4
    with:
      path: node_modules
      key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
      restore-keys: |
        ${{ runner.os }}-node-
  
  - name: Install dependencies
    if: steps.cache-npm.outputs.cache-hit != 'true'
    run: npm ci
```

### Maven 缓存

```yaml
steps:
  - uses: actions/checkout@v4
  
  - name: Setup JDK 21
    uses: actions/setup-java@v4
    with:
      java-version: '21'
      distribution: 'temurin'
      cache: 'maven'
  
  - name: Build with Maven
    run: mvn -B package
```

### Docker 层缓存

```yaml
steps:
  - uses: actions/checkout@v4
  
  - name: Set up Docker Buildx
    uses: docker/setup-buildx-action@v3
  
  - name: Build and push
    uses: docker/build-push-action@v5
    with:
      context: .
      push: true
      tags: user/app:latest
      cache-from: type=gha
      cache-to: type=gha,mode=max
```

## 制品上传下载

### 上传制品

```yaml
steps:
  - uses: actions/checkout@v4
  
  - name: Build
    run: npm run build
  
  - name: Upload build artifact
    uses: actions/upload-artifact@v4
    with:
      name: build-output
      path: dist/
      retention-days: 7
  
  - name: Upload multiple artifacts
    uses: actions/upload-artifact@v4
    with:
      name: test-reports
      path: |
        coverage/
        test-results/
```

### 下载制品

```yaml
jobs:
  deploy:
    needs: build
    runs-on: ubuntu-latest
    
    steps:
      - name: Download artifact
        uses: actions/download-artifact@v4
        with:
          name: build-output
          path: dist/
      
      - name: Deploy
        run: |
          ls -la dist/
          # 部署逻辑
```

### 跨工作流制品

```yaml
# 工作流1：构建并上传
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - run: npm run build
      - uses: actions/upload-artifact@v4
        with:
          name: build
          path: dist/

---
# 工作流2：下载并部署
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Download artifact
        uses: actions/download-artifact@v4
        with:
          name: build
          run-id: ${{ github.event.workflow_run.id }}
          github-token: ${{ secrets.GITHUB_TOKEN }}
```

## 密钥管理（Secrets/Variables）

### 使用 Secrets

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Deploy
        env:
          API_KEY: ${{ secrets.API_KEY }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: |
          echo "Deploying with API key"
          # 使用密钥
```

### 环境级 Secrets

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    environment: production
    
    steps:
      - name: Deploy
        env:
          AWS_ACCESS_KEY_ID: ${{ secrets.AWS_ACCESS_KEY_ID }}
          AWS_SECRET_ACCESS_KEY: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        run: aws s3 sync dist/ s3://my-bucket/
```

### 使用 Variables

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Build
        env:
          APP_VERSION: ${{ vars.APP_VERSION }}
          REGISTRY: ${{ vars.REGISTRY }}
        run: |
          echo "Building version $APP_VERSION"
          echo "Registry: $REGISTRY"
```

### OIDC 身份验证

```yaml
jobs:
  deploy:
    runs-on: ubuntu-latest
    permissions:
      id-token: write
      contents: read
    
    steps:
      - name: Configure AWS credentials
        uses: aws-actions/configure-aws-credentials@v4
        with:
          role-to-assume: arn:aws:iam::123456789012:role/github-actions
          aws-region: us-east-1
      
      - name: Deploy to S3
        run: aws s3 sync dist/ s3://my-bucket/
```

## 复合动作（Composite Action）

### 创建复合动作

```yaml
# .github/actions/setup-project/action.yml
name: 'Setup Project'
description: 'Setup Node.js project with dependencies'

inputs:
  node-version:
    description: 'Node.js version'
    required: false
    default: '20'

outputs:
  node-version:
    description: 'Installed Node.js version'
    value: ${{ steps.setup-node.outputs.node-version }}

runs:
  using: "composite"
  steps:
    - name: Setup Node.js
      id: setup-node
      uses: actions/setup-node@v4
      with:
        node-version: ${{ inputs.node-version }}
        cache: 'npm'
    
    - name: Install dependencies
      shell: bash
      run: npm ci
    
    - name: Cache node_modules
      uses: actions/cache@v4
      with:
        path: node_modules
        key: ${{ runner.os }}-node-${{ hashFiles('**/package-lock.json') }}
```

### 使用复合动作

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup project
        uses: ./.github/actions/setup-project
        with:
          node-version: '20'
      
      - name: Build
        run: npm run build
      
      - name: Test
        run: npm test
```

## 自托管 Runner

### 配置自托管 Runner

```bash
# 下载 Runner
mkdir actions-runner && cd actions-runner
curl -o actions-runner-linux-x64-2.311.0.tar.gz -L https://github.com/actions/runner/releases/download/v2.311.0/actions-runner-linux-x64-2.311.0.tar.gz
tar xzf ./actions-runner-linux-x64-2.311.0.tar.gz

# 配置 Runner
./config.sh --url https://github.com/your-org/your-repo --token YOUR_TOKEN

# 安装为服务
sudo ./svc.sh install
sudo ./svc.sh start
```

### 使用自托管 Runner

```yaml
jobs:
  build:
    runs-on: self-hosted
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Build
        run: |
          # 使用自托管 Runner 的环境
          echo "Running on self-hosted runner"
          npm ci
          npm run build
```

### Runner 标签

```yaml
jobs:
  gpu-job:
    runs-on: [self-hosted, linux, gpu]
  
  arm-job:
    runs-on: [self-hosted, linux, arm64]
  
  windows-job:
    runs-on: [self-hosted, windows]
```

### Runner 组

```yaml
# 在组织级别配置 Runner 组
jobs:
  deploy:
    runs-on: [self-hosted, production]
    group: production-runners
```

## 高级工作流示例

### 完整 CI/CD 流水线

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/setup-project
      - run: npm run lint

  test:
    runs-on: ubuntu-latest
    needs: lint
    strategy:
      matrix:
        node-version: [18, 20, 22]
    
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/setup-project
        with:
          node-version: ${{ matrix.node-version }}
      - run: npm test
      - uses: actions/upload-artifact@v4
        if: matrix.node-version == 20
        with:
          name: coverage
          path: coverage/

  build:
    runs-on: ubuntu-latest
    needs: test
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3
      
      - name: Login to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=sha
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  deploy:
    runs-on: ubuntu-latest
    needs: build
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
      - name: Deploy to production
        env:
          KUBECONFIG: ${{ secrets.KUBECONFIG }}
        run: |
          kubectl set image deployment/myapp \
            app=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:sha-${{ github.sha }}
```

### 发布工作流

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write
  packages: write

jobs:
  release:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          registry-url: 'https://registry.npmjs.org'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Build
        run: npm run build
      
      - name: Run tests
        run: npm test
      
      - name: Create Release
        uses: actions/create-release@v1
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        with:
          tag_name: ${{ github.ref }}
          release_name: Release ${{ github.ref }}
          draft: false
          prerelease: ${{ contains(github.ref, 'beta') || contains(github.ref, 'alpha') }}
      
      - name: Publish to npm
        run: npm publish
        env:
          NODE_AUTH_TOKEN: ${{ secrets.NPM_TOKEN }}
```

### 依赖更新工作流

```yaml
name: Dependency Update

on:
  schedule:
    - cron: '0 2 * * 1'  # 每周一凌晨2点
  workflow_dispatch:

jobs:
  update:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
      
      - name: Update dependencies
        run: |
          npm update
          npm audit fix
      
      - name: Create Pull Request
        uses: peter-evans/create-pull-request@v5
        with:
          token: ${{ secrets.GITHUB_TOKEN }}
          commit-message: 'chore: update dependencies'
          title: 'chore: update dependencies'
          body: |
            This PR updates npm dependencies to their latest versions.
            
            Please review the changes and ensure all tests pass.
          branch: dependency-updates
          delete-branch: true
```
