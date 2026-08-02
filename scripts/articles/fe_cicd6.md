# CI/CD 流水线设计与安全

## 流水线阶段设计

### 典型流水线阶段

```
代码提交 → 代码检查 → 单元测试 → 构建 → 安全扫描 → 部署
    ↓         ↓         ↓        ↓        ↓        ↓
  Lint      SAST     Tests    Build    Scan    Deploy
```

### 完整流水线示例

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
  # 阶段1：代码检查
  lint:
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
      
      - name: Run ESLint
        run: npm run lint
      
      - name: Run Prettier check
        run: npm run format:check
      
      - name: Run TypeScript check
        run: npm run typecheck

  # 阶段2：单元测试
  test:
    runs-on: ubuntu-latest
    needs: lint
    strategy:
      matrix:
        node-version: [18, 20, 22]
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: ${{ matrix.node-version }}
          cache: 'npm'
      
      - name: Install dependencies
        run: npm ci
      
      - name: Run tests
        run: npm test -- --coverage
      
      - name: Upload coverage
        if: matrix.node-version == 20
        uses: actions/upload-artifact@v4
        with:
          name: coverage
          path: coverage/

  # 阶段3：构建
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
            type=sha
            type=ref,event=branch
      
      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # 阶段4：安全扫描
  security:
    runs-on: ubuntu-latest
    needs: build
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Trivy vulnerability scanner
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:sha-${{ github.sha }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
      
      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v2
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

  # 阶段5：部署到预发布
  deploy-staging:
    runs-on: ubuntu-latest
    needs: security
    if: github.ref == 'refs/heads/main'
    environment: staging
    
    steps:
      - name: Deploy to staging
        run: |
          kubectl set image deployment/myapp \
            app=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:sha-${{ github.sha }}
      
      - name: Run integration tests
        run: |
          npm run test:integration

  # 阶段6：部署到生产
  deploy-production:
    runs-on: ubuntu-latest
    needs: deploy-staging
    if: github.ref == 'refs/heads/main'
    environment: production
    
    steps:
      - name: Deploy to production
        run: |
          kubectl set image deployment/myapp \
            app=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:sha-${{ github.sha }}
```

## 安全扫描（SAST/DAST/SCA）

### SAST（静态应用安全测试）

```yaml
# CodeQL 分析
jobs:
  codeql:
    runs-on: ubuntu-latest
    permissions:
      security-events: write
    
    strategy:
      fail-fast: false
      matrix:
        language: ['javascript', 'python', 'java']
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Initialize CodeQL
        uses: github/codeql-action/init@v2
        with:
          languages: ${{ matrix.language }}
          queries: +security-extended,security-and-quality
      
      - name: Autobuild
        uses: github/codeql-action/autobuild@v2
      
      - name: Perform CodeQL Analysis
        uses: github/codeql-action/analyze@v2
        with:
          category: "/language:${{ matrix.language }}"
```

```yaml
# SonarQube 扫描
jobs:
  sonarqube:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: SonarQube Scan
        uses: SonarSource/sonarqube-scan-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
          SONAR_HOST_URL: ${{ secrets.SONAR_HOST_URL }}
      
      - name: Quality Gate check
        uses: SonarSource/sonarqube-quality-gate-action@master
        timeout-minutes: 5
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
```

### DAST（动态应用安全测试）

```yaml
# OWASP ZAP 扫描
jobs:
  zap-scan:
    runs-on: ubuntu-latest
    needs: deploy-staging
    
    steps:
      - name: ZAP Scan
        uses: zaproxy/action-full-scan@v0.7.0
        with:
          target: 'https://staging.example.com'
          rules_file_name: 'zap-rules.tsv'
          cmd_options: '-a'
```

```yaml
# API 安全测试
jobs:
  api-security:
    runs-on: ubuntu-latest
    needs: deploy-staging
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Run API security tests
        run: |
          # 使用 OWASP ZAP API 扫描
          docker run --rm \
            -v $(pwd):/zap/wrk/:rw \
            -t ghcr.io/zaproxy/zaproxy:stable \
            zap-api-scan.py \
            -t https://staging.example.com/api/openapi.json \
            -f openapi \
            -r api-report.html
```

### SCA（软件成分分析）

```yaml
# 依赖漏洞扫描
jobs:
  dependency-check:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v4
      
      - name: Run Snyk to check for vulnerabilities
        uses: snyk/actions/node@master
        env:
          SNYK_TOKEN: ${{ secrets.SNYK_TOKEN }}
        with:
          args: --severity-threshold=high
      
      - name: Run npm audit
        run: npm audit --audit-level=high
      
      - name: Run OWASP Dependency Check
        uses: dependency-check/Dependency-Check_Action@main
        with:
          project: 'myapp'
          path: '.'
          format: 'HTML'
          out: 'reports'
```

## 依赖漏洞检测

### npm 审计

```bash
# 运行安全审计
npm audit

# 只显示高危漏洞
npm audit --audit-level=high

# 自动修复
npm audit fix

# 强制修复（可能有破坏性变更）
npm audit fix --force

# 输出 JSON 格式
npm audit --json > audit-results.json
```

### Maven 依赖检查

```xml
<!-- pom.xml -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>9.0.7</version>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <suppressionFiles>
            <suppressionFile>dependency-check-suppressions.xml</suppressionFile>
        </suppressionFiles>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Python 依赖检查

```bash
# 使用 safety
pip install safety
safety check

# 使用 pip-audit
pip install pip-audit
pip-audit

# 使用 bandit（代码安全）
pip install bandit
bandit -r src/
```

### Renovate 自动更新

```json
// renovate.json
{
  "$schema": "https://docs.renovatebot.com/renovate-schema.json",
  "extends": [
    "config:base"
  ],
  "vulnerabilityAlerts": {
    "enabled": true
  },
  "schedule": ["before 6am on Monday"],
  "packageRules": [
    {
      "matchUpdateTypes": ["minor", "patch"],
      "automerge": true
    },
    {
      "matchPackagePatterns": ["@angular/*"],
      "groupName": "Angular"
    }
  ]
}
```

## 密钥泄露防护

### GitLeaks 配置

```yaml
# .gitleaks.toml
title = "gitleaks config"

[allowlist]
  description = "Global allowlist"
  paths = []
  regexTarget = "match"
  regexes = []

[[rules]]
  description = "AWS Access Key"
  id = "aws-access-key"
  regex = '''AKIA[0-9A-Z]{16}'''
  tags = ["key", "AWS"]

[[rules]]
  description = "Generic Secret"
  id = "generic-secret"
  regex = '''(?i)(api[_-]?key|apikey|secret|password|passwd|token)\s*[:=]\s*['"]?([a-zA-Z0-9+/=!@#$%^&*]{20,})['"]?'''
  tags = ["key", "Generic"]
```

```yaml
# GitHub Actions 中使用
jobs:
  gitleaks:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with:
          fetch-depth: 0
      
      - name: Gitleaks
        uses: gitleaks/gitleaks-action@v2
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

### 预提交钩子

```yaml
# .pre-commit-config.yaml
repos:
  - repo: https://github.com/gitleaks/gitleaks
    rev: v8.18.0
    hooks:
      - id: gitleaks

  - repo: https://github.com/trufflesecurity/trufflehog
    rev: v3.63.2
    hooks:
      - id: trufflehog
        entry: trufflehog git file://. --only-verified --fail
```

```bash
# 安装 pre-commit
pip install pre-commit

# 安装钩子
pre-commit install

# 手动运行
pre-commit run --all-files
```

### 密钥管理最佳实践

```yaml
# GitHub Secrets 使用
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Use secrets
        env:
          API_KEY: ${{ secrets.API_KEY }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: |
          # 使用环境变量，不要打印密钥
          echo "Deploying..."
```

## 环境审批门禁

### 环境保护规则

```yaml
# GitHub 环境配置
jobs:
  deploy-production:
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://example.com
    
    steps:
      - name: Deploy
        run: echo "Deploying to production"
```

### 手动审批

```yaml
jobs:
  deploy-production:
    runs-on: ubuntu-latest
    environment: production
    
    steps:
      - name: Wait for approval
        uses: trstringer/manual-approval@v1
        with:
          secret: ${{ secrets.GITHUB_TOKEN }}
          approvers: user1,user2
          minimum-approvals: 2
          issue-title: "Deploy to production"
          issue-body: "Please approve or deny the deployment to production"
      
      - name: Deploy
        run: echo "Deploying to production"
```

### 环境变量和密钥分离

```yaml
# 不同环境使用不同密钥
jobs:
  deploy-staging:
    runs-on: ubuntu-latest
    environment: staging
    steps:
      - name: Deploy
        env:
          DB_HOST: ${{ vars.DB_HOST }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: echo "Deploying to staging"

  deploy-production:
    runs-on: ubuntu-latest
    environment: production
    steps:
      - name: Deploy
        env:
          DB_HOST: ${{ vars.DB_HOST }}
          DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
        run: echo "Deploying to production"
```

## GitOps 原则

### GitOps 工作流

```
代码变更 → Git 提交 → CI 构建 → 更新配置仓库 → ArgoCD 同步 → Kubernetes 部署
```

### ArgoCD 配置

```yaml
# argocd-application.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: myapp
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/org/k8s-configs.git
    targetRevision: HEAD
    path: apps/myapp/overlays/production
  destination:
    server: https://kubernetes.default.svc
    namespace: production
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
    syncOptions:
      - CreateNamespace=true
```

### Flux 配置

```yaml
# flux-system/gotk-sync.yaml
apiVersion: source.toolkit.fluxcd.io/v1
kind: GitRepository
metadata:
  name: flux-system
  namespace: flux-system
spec:
  interval: 1m0s
  ref:
    branch: main
  url: https://github.com/org/k8s-configs.git
---
apiVersion: kustomize.toolkit.fluxcd.io/v1
kind: Kustomization
metadata:
  name: flux-system
  namespace: flux-system
spec:
  interval: 10m0s
  path: ./clusters/production
  prune: true
  sourceRef:
    kind: GitRepository
    name: flux-system
```

### GitOps 流水线

```yaml
name: GitOps Deploy

on:
  push:
    branches: [main]

jobs:
  # 构建并推送镜像
  build:
    runs-on: ubuntu-latest
    outputs:
      image-tag: ${{ steps.meta.outputs.version }}
    steps:
      - uses: actions/checkout@v4
      
      - name: Build and push
        id: build
        run: |
          docker build -t myapp:${{ github.sha }} .
          docker push myapp:${{ github.sha }}
          echo "tag=${{ github.sha }}" >> $GITHUB_OUTPUT
  
  # 更新配置仓库
  update-configs:
    needs: build
    runs-on: ubuntu-latest
    steps:
      - name: Checkout config repo
        uses: actions/checkout@v4
        with:
          repository: org/k8s-configs
          token: ${{ secrets.CONFIG_REPO_TOKEN }}
      
      - name: Update image tag
        run: |
          cd apps/myapp/overlays/production
          kustomize edit set image myapp=myapp:${{ needs.build.outputs.image-tag }}
      
      - name: Commit and push
        run: |
          git config user.name "GitHub Actions"
          git config user.email "actions@github.com"
          git add .
          git commit -m "chore: update myapp image to ${{ needs.build.outputs.image-tag }}"
          git push
```

## 流水线安全最佳实践

### 最小权限原则

```yaml
permissions:
  contents: read
  packages: write
  security-events: write
  id-token: write  # 用于 OIDC
```

### 密钥轮换

```yaml
# 定期轮换密钥
name: Rotate Secrets

on:
  schedule:
    - cron: '0 0 1 * *'  # 每月1日

jobs:
  rotate:
    runs-on: ubuntu-latest
    steps:
      - name: Rotate API key
        run: |
          # 生成新密钥
          NEW_KEY=$(openssl rand -hex 32)
          
          # 更新密钥库
          # ...
          
          # 更新 GitHub Secret
          # 使用 gh CLI 或 API
```

### 审计日志

```yaml
# 记录部署审计信息
- name: Audit log
  run: |
    echo "Deployment audit:" >> audit.log
    echo "  User: ${{ github.actor }}" >> audit.log
    echo "  Time: $(date -u +%Y-%m-%dT%H:%M:%SZ)" >> audit.log
    echo "  Commit: ${{ github.sha }}" >> audit.log
    echo "  Environment: production" >> audit.log
    
    # 上传审计日志
    aws s3 cp audit.log s3://audit-logs/deployments/
```

### 安全扫描集成

```yaml
# 综合安全扫描
jobs:
  security:
    runs-on: ubuntu-latest
    steps:
      # SAST
      - name: CodeQL Analysis
        uses: github/codeql-action/analyze@v2
      
      # SCA
      - name: Dependency Check
        run: npm audit --audit-level=high
      
      # 容器扫描
      - name: Trivy Scan
        uses: aquasecurity/trivy-action@master
      
      # 密钥扫描
      - name: Gitleaks
        uses: gitleaks/gitleaks-action@v2
      
      # IaC 扫描
      - name: Checkov
        uses: bridgecrewio/checkov-action@v12
        with:
          directory: .
          framework: kubernetes,dockerfile
```
