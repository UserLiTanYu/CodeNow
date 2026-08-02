# Git Flow、GitHub Flow 与分支策略

## 为什么需要分支策略

在多人协作的软件开发项目中，分支策略是团队高效协作的基石。一个清晰的分支策略能够：

- 隔离开发中的功能代码与稳定的生产代码
- 支持并行开发多个功能而互不干扰
- 保证发布流程的可追溯性和可回滚性
- 降低代码冲突的频率和解决难度

没有分支策略的团队往往会遇到"代码覆盖""发布混乱""冲突频发"等问题，最终导致开发效率低下和产品质量下降。

## Git Flow 模型

Git Flow 是由 Vincent Driessen 在 2010 年提出的分支模型，它定义了严格的分支角色和操作流程，适合有固定发布周期的项目。

### 分支类型

Git Flow 定义了五种分支类型：

| 分支类型 | 命名规则 | 生命周期 | 用途 |
|---------|---------|---------|------|
| main | main | 永久 | 存放稳定的生产代码 |
| develop | develop | 永久 | 集成开发分支 |
| feature | feature/* | 临时 | 开发新功能 |
| release | release/* | 临时 | 准备发布 |
| hotfix | hotfix/* | 临时 | 紧急修复生产问题 |

### 分支流程图

```
main    ─●─────────────────●─────────────────●─
          \               /                 / 
hotfix     \             ●                 /  
            \           /                 /   
develop     ●──●──●──●──●──●──●──●──●──●──●─
              \     /       \     /        
feature        ●──●         ●──●           
               feature/     feature/       
               login        search         
```

### Git Flow 操作命令

安装 Git Flow 扩展：

```bash
# macOS
brew install git-flow

# Linux
apt-get install git-flow

# Windows (Git for Windows 自带)
# 无需额外安装
```

初始化 Git Flow：

```bash
# 在项目根目录初始化
git flow init

# 交互式配置各分支前缀
# Production branch: main
# Development branch: develop
# Feature branches: feature/
# Release branches: release/
# Hotfix branches: hotfix/
# Support branches: support/
# Version tag prefix: v
```

功能开发流程：

```bash
# 开始新功能
git flow feature start user-login

# 开发完成后推送到远程
git flow feature publish user-login

# 完成功能并合并到 develop
git flow feature finish user-login
```

发布流程：

```bash
# 创建发布分支
git flow release start v1.2.0

# 在发布分支上进行测试和修复
git commit -m "fix: 修复登录页面样式问题"

# 完成发布：合并到 main 和 develop，打标签
git flow release finish v1.2.0
```

紧急修复流程：

```bash
# 创建 hotfix 分支
git flow hotfix start fix-security-vulnerability

# 修复问题
git commit -m "fix: 修复 SQL 注入漏洞"

# 完成修复：合并到 main 和 develop，打标签
git flow hotfix finish fix-security-vulnerability
```

### Git Flow 的优缺点

**优点：**
- 分支角色清晰，适合大型团队
- 有固定的发布流程，适合版本化发布
- 主分支始终保持稳定状态

**缺点：**
- 分支数量多，流程复杂
- 合并冲突频繁，特别是在 develop 分支上
- 不适合持续部署的项目

## GitHub Flow

GitHub Flow 是 GitHub 推荐的轻量级分支模型，它简化了 Git Flow 的复杂流程，适合持续部署的项目。

### 核心原则

GitHub Flow 只有六条规则：

1. `main` 分支上的代码始终是可部署的
2. 所有新功能开发都从 `main` 分支创建描述性分支
3. 定期向远程推送分支
4. 需要合并时创建 Pull Request
5. PR 必须经过代码审查和 CI 测试
6. 审查通过后合并到 `main` 并立即部署

### 分支命名规范

```bash
# 功能分支
feature/add-user-authentication
feature/improve-search-performance

# 修复分支
fix/login-form-validation
fix/memory-leak-in-cache

# 使用 issue 编号
feature/issue-123-add-export
fix/issue-456-null-pointer
```

### 完整工作流程

```bash
# 1. 从 main 创建新分支
git checkout main
git pull origin main
git checkout -b feature/add-dark-mode

# 2. 开发并提交
git add .
git commit -m "feat: 添加深色模式切换功能"

# 3. 定期推送到远程
git push origin feature/add-dark-mode

# 4. 创建 Pull Request
# 在 GitHub 网页上创建 PR

# 5. 代码审查和 CI 测试
# 团队成员审查代码，CI 自动运行测试

# 6. 合并后删除分支
git checkout main
git pull origin main
git branch -d feature/add-dark-mode
```

### Pull Request 最佳实践

**标题规范：**
```
feat: 添加用户导出功能
fix: 修复登录超时问题
docs: 更新 API 文档
refactor: 重构数据库连接池
```

**PR 描述模板：**

```markdown
## 变更说明
简要描述这个 PR 做了什么

## 变更类型
- [ ] 新功能
- [ ] Bug 修复
- [ ] 重构
- [ ] 文档更新

## 测试说明
描述如何测试这些变更

## 截图（如适用）
添加相关截图

## 关联 Issue
Closes #123
```

## GitLab Flow

GitLab Flow 是 GitLab 推荐的分支策略，它结合了 Git Flow 和 GitHub Flow 的优点，增加了环境分支的概念。

### 环境分支模型

```
main ──────●──────●──────●──────●──────
            \      \      \      \
staging      ●──────●──────●──────
              \      \      \
production    ●──────●──────
```

### 版本分支模型

```
main ────●────●────●────●────●────●────
          \         \         \
8-stable   ●────●    \         \
9-stable            ●────●     \
10-stable                    ●────●
```

### GitLab Flow 操作

```bash
# 功能开发
git checkout -b feature/new-dashboard main
# 开发完成后合并到 main

# 部署到预发布环境
git checkout staging
git merge main
git push origin staging

# 部署到生产环境
git checkout production
git merge staging
git push origin production
```

## Trunk-Based Development

Trunk-Based Development（主干开发）是一种极端简化的分支策略，所有开发者直接在主干上工作，适合高度自动化的 CI/CD 环境。

### 核心特点

- 所有开发在 `main` 分支上进行
- 使用短命的功能分支（不超过 2 天）
- 依赖功能开关（Feature Flags）控制未完成功能的可见性
- 需要强大的自动化测试和 CI/CD 支持

### Feature Flags 实现

```java
// Java 示例
public class FeatureFlags {
    private static final Map<String, Boolean> FLAGS = Map.of(
        "new-checkout-flow", false,
        "dark-mode", true,
        "beta-search", false
    );

    public static boolean isEnabled(String flag) {
        return FLAGS.getOrDefault(flag, false);
    }
}

// 使用示例
if (FeatureFlags.isEnabled("new-checkout-flow")) {
    // 新的结账流程
} else {
    // 旧的结账流程
}
```

```javascript
// JavaScript 示例
const featureFlags = {
  'new-dashboard': process.env.FF_NEW_DASHBOARD === 'true',
  'ai-suggestions': process.env.FF_AI_SUGGESTIONS === 'true'
};

function isFeatureEnabled(flag) {
  return featureFlags[flag] || false;
}

// React 组件中使用
function Dashboard() {
  if (isFeatureEnabled('new-dashboard')) {
    return <NewDashboard />;
  }
  return <LegacyDashboard />;
}
```

### 主干开发的工作流程

```bash
# 1. 从主干拉取最新代码
git checkout main
git pull origin main

# 2. 创建短命分支（可选）
git checkout -b feature/quick-fix

# 3. 小步提交，频繁集成
git add .
git commit -m "feat: 添加表单验证"
git push origin feature/quick-fix

# 4. 立即创建 PR 并合并
# 5. 删除分支
git checkout main
git pull origin main
git branch -d feature/quick-fix
```

## 分支保护规则

分支保护是防止重要分支被意外修改的机制，是代码质量的重要保障。

### GitHub 分支保护配置

在 GitHub 仓库的 Settings → Branches 中配置：

```yaml
# 分支保护规则示例
branch_protection:
  main:
    required_status_checks:
      strict: true
      contexts:
        - "ci/build"
        - "ci/test"
        - "lint"
    required_pull_request_reviews:
      required_approving_review_count: 2
      dismiss_stale_reviews: true
      require_code_owner_reviews: true
    restrictions:
      users: ["admin"]
      teams: ["core-developers"]
    enforce_admins: true
    allow_force_pushes: false
    allow_deletions: false
```

### GitLab 分支保护

```yaml
# .gitlab-ci.yml 中的分支保护
protected_branches:
  - name: main
    push_access_level: maintainer
    merge_access_level: developer
    allow_force_push: false
    
  - name: develop
    push_access_level: developer
    merge_access_level: developer
    allow_force_push: false
```

### 代码所有权（CODEOWNERS）

```bash
# .github/CODEOWNERS 文件
# 全局所有者
* @team-lead

# 前端代码
/src/frontend/ @frontend-team
*.vue @frontend-team
*.css @frontend-team

# 后端代码
/src/backend/ @backend-team
*.java @backend-team

# 数据库相关
/src/main/resources/db/ @dba-team
*.sql @dba-team

# DevOps
/infrastructure/ @devops-team
Dockerfile @devops-team
docker-compose.yml @devops-team
```

## 代码审查流程

代码审查（Code Review）是保证代码质量的关键环节，良好的审查流程能有效发现潜在问题。

### 审查清单

**功能性检查：**
- 代码是否实现了需求描述的功能
- 边界条件是否处理
- 错误处理是否完善

**代码质量检查：**
- 代码是否遵循团队编码规范
- 命名是否清晰有意义
- 是否有重复代码可以提取
- 注释是否充分且有意义

**安全性检查：**
- 是否有 SQL 注入风险
- 是否有 XSS 漏洞
- 敏感信息是否硬编码
- 权限检查是否完整

**性能检查：**
- 是否有不必要的数据库查询
- 是否有内存泄漏风险
- 算法复杂度是否合理

### 审查评论规范

```markdown
# 建设性的评论格式

## 必须修改（Blocking）
> 这里存在 SQL 注入风险，需要使用参数化查询

## 建议修改（Non-blocking）
> 这个变量名可以更具描述性，比如 `userAuthenticationResult`

## 问题（Question）
> 这里的超时时间设置为 30 秒，是否有特定的原因？

## 赞扬（Praise）
> 这个错误处理的实现很优雅，考虑了各种边界情况
```

### 自动化代码审查工具

```yaml
# GitHub Actions 中集成代码审查工具
name: Code Review

on: [pull_request]

jobs:
  review:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Run ESLint
        run: npm run lint
      
      - name: Run SonarQube Scan
        uses: sonarcloud/sonarcloud-github-action@master
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
      
      - name: Run CodeQL Analysis
        uses: github/codeql-action/analyze@v2
```

## 分支策略选择指南

| 团队规模 | 发布频率 | 自动化程度 | 推荐策略 |
|---------|---------|-----------|---------|
| 小型（1-5人） | 持续部署 | 高 | GitHub Flow / Trunk-Based |
| 中型（5-15人） | 每周/每月 | 中等 | GitLab Flow |
| 大型（15+人） | 季度/版本化 | 中等 | Git Flow |
| 企业级 | 固定周期 | 高 | Git Flow + GitLab Flow |

## 常见分支问题及解决方案

### 合并冲突处理

```bash
# 遇到合并冲突时
git merge feature-branch
# CONFLICT (content): Merge conflict in src/app.js

# 查看冲突文件
git status

# 手动解决冲突后
git add src/app.js
git commit -m "merge: 解决 feature-branch 合并冲突"
```

### 误删分支恢复

```bash
# 查看分支操作历史
git reflog | grep branch

# 恢复删除的分支
git checkout -b recovered-branch <commit-hash>
```

### 分支清理

```bash
# 查看已合并的分支
git branch --merged main

# 删除已合并的本地分支
git branch --merged main | grep -v "main" | xargs git branch -d

# 删除已合并的远程分支
git branch -r --merged main | grep -v "main" | sed 's/origin\///' | xargs -I {} git push origin :{}
```
