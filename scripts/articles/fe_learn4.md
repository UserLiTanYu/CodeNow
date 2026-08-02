# 高效编程习惯与开发环境优化

工欲善其事，必先利其器。高效的编程习惯和优化的开发环境能显著提升你的开发效率。本文将分享快捷键、命令行、Git、IDE 模板、自动化脚本等方面的最佳实践。

## 快捷键精通

### IntelliJ IDEA 必会快捷键

**编辑类**：

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + D` | 复制当前行 |
| `Ctrl + Y` | 删除当前行 |
| `Ctrl + /` | 注释/取消注释 |
| `Ctrl + Shift + /` | 块注释 |
| `Ctrl + W` | 递增选择代码块 |
| `Ctrl + Shift + Enter` | 自动补全分号、括号 |
| `Alt + Enter` | 快速修复（万能键） |
| `Ctrl + Alt + L` | 格式化代码 |
| `Ctrl + Alt + O` | 优化 import |

**导航类**：

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + N` | 查找类 |
| `Ctrl + Shift + N` | 查找文件 |
| `Ctrl + F` | 当前文件查找 |
| `Ctrl + Shift + F` | 全局查找 |
| `Ctrl + G` | 跳转到行 |
| `Ctrl + B` | 跳转到定义 |
| `Alt + F7` | 查找使用 |
| `Ctrl + Alt + B` | 跳转到实现 |
| `Ctrl + E` | 最近打开的文件 |
| `Ctrl + Shift + E` | 最近编辑的文件 |

**重构类**：

| 快捷键 | 功能 |
|--------|------|
| `Shift + F6` | 重命名 |
| `Ctrl + Alt + M` | 提取方法 |
| `Ctrl + Alt + V` | 提取变量 |
| `Ctrl + Alt + F` | 提取字段 |
| `Ctrl + Alt + C` | 提取常量 |
| `Ctrl + Alt + P` | 提取参数 |

**调试类**：

| 快捷键 | 功能 |
|--------|------|
| `F8` | Step Over |
| `F7` | Step Into |
| `Shift + F8` | Step Out |
| `F9` | Resume |
| `Ctrl + F8` | Toggle Breakpoint |
| `Ctrl + Shift + F8` | 查看所有断点 |

### VS Code 必会快捷键

| 快捷键 | 功能 |
|--------|------|
| `Ctrl + P` | 快速打开文件 |
| `Ctrl + Shift + P` | 命令面板 |
| `Ctrl + `` ` `` | 打开终端 |
| `Ctrl + B` | 切换侧边栏 |
| `Ctrl + D` | 选择下一个相同文本 |
| `Alt + ↑/↓` | 上/下移动行 |
| `Shift + Alt + ↑/↓` | 上/下复制行 |
| `Ctrl + Shift + K` | 删除行 |
| `Ctrl + G` | 跳转到行 |

## 命令行效率

### Zsh + Oh My Zsh

```bash
# 安装 Zsh
sudo apt install zsh  # Ubuntu
brew install zsh      # macOS

# 安装 Oh My Zsh
sh -c "$(curl -fsSL https://raw.github.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"

# 推荐主题：agnoster、powerlevel10k
ZSH_THEME="powerlevel10k/powerlevel10k"

# 推荐插件
plugins=(
    git
    zsh-autosuggestions      # 自动补全建议
    zsh-syntax-highlighting  # 语法高亮
    z                       # 快速跳转目录
    docker
    kubectl
)
```

### 常用 Shell 别名

```bash
# ~/.zshrc 或 ~/.bashrc

# 导航
alias ..="cd .."
alias ...="cd ../.."
alias ....="cd ../../.."
alias ll="ls -alF"
alias la="ls -A"

# Git
alias g="git"
alias gs="git status"
alias ga="git add"
alias gc="git commit"
alias gp="git push"
alias gl="git log --oneline -20"
alias gd="git diff"
alias gco="git checkout"
alias gb="git branch"
alias gba="git branch -a"
alias gm="git merge"
alias gr="git rebase"

# Maven
alias mvn="mvn --batch-mode"
alias mci="mvn clean install"
alias mct="mvn clean test"
alias mcp="mvn clean package"

# Docker
alias d="docker"
alias dc="docker compose"
alias dps="docker ps"
alias dlog="docker logs -f"

# 快速编辑配置
alias zshrc="vim ~/.zshrc"
alias reload="source ~/.zshrc"
```

### Git 别名

```bash
# Git 全局别名
git config --global alias.st status
git config --global alias.co checkout
git config --global alias.br branch
git config --global alias.ci commit
git config --global alias.df diff
git config --global alias.lg "log --oneline --graph --all"
git config --global alias.last "log -1 HEAD"
git config --global alias.unstage "reset HEAD --"
git config --global alias.amend "commit --amend --no-edit"

# 使用
git st        # git status
git co main   # git checkout main
git lg        # 美观的日志
git amend     # 修改最后一次提交
```

### 常用 Git 命令

```bash
# 查看某个文件的修改历史
git log --follow -p -- src/Main.java

# 查看某次提交的详细内容
git show abc1234

# 查看两个分支的差异
git diff main..feature

# 只查看某个文件的差异
git diff main..feature -- src/Main.java

# 暂存当前修改
git stash
git stash pop
git stash list

# 交互式 rebase（整理提交历史）
git rebase -i HEAD~5

# cherry-pick 某个提交
git cherry-pick abc1234

# 查看某行代码是谁写的
git blame src/Main.java

# 搜索包含特定内容的提交
git log -S "functionName" --oneline
```

## IDE 模板

### Live Templates（IntelliJ IDEA）

```
# File → Settings → Editor → Live Templates

# 自定义模板示例

缩写：sysout
模板：System.out.println($END$);

缩写：psvm
模板：
public static void main(String[] args) {
    $END$
}

缩写：tryc
模板：
try {
    $SELECTION$
} catch (Exception e) {
    e.printStackTrace();
}

缩写：fori
模板：
for (int $INDEX$ = 0; $INDEX$ < $LIMIT$; $INDEX$++) {
    $END$
}

缩写：fore
模板：
for ($ELEMENT_TYPE$ $VAR$ : $COLLECTION$) {
    $END$
}

缩写：tc
模板：
@Test
public void $METHOD_NAME$() {
    $END$
}
```

### 文件模板

```java
/**
 * ${DESCRIPTION}
 *
 * @author ${USER}
 * @date ${DATE} ${TIME}
 */
public class ${NAME} {
    $END$
}
```

## 自动化脚本

### 项目初始化脚本

```bash
#!/bin/bash
# init-project.sh

echo "初始化项目环境..."

# 检查必要工具
check_tool() {
    if ! command -v $1 &> /dev/null; then
        echo "错误：$1 未安装"
        exit 1
    fi
    echo "✓ $1 已安装"
}

check_tool java
check_tool mvn
check_tool node
check_tool npm
check_tool git

# 后端初始化
echo "初始化后端..."
cd backend
mvn clean install -DskipTests
cd ..

# 前端初始化
echo "初始化前端..."
cd frontend
npm ci
cd ..

# 数据库初始化
echo "初始化数据库..."
mysql -u root -p123456 < sql/init.sql

# 启动服务
echo "启动后端..."
cd backend
mvn spring-boot:run &
cd ..

echo "启动前端..."
cd frontend
npm run dev &
cd ..

echo "项目初始化完成！"
echo "后端: http://localhost:8080"
echo "前端: http://localhost:5173"
```

### 快速部署脚本

```bash
#!/bin/bash
# deploy.sh

set -e  # 遇到错误立即退出

echo "开始部署..."

# 拉取最新代码
git pull origin main

# 构建后端
echo "构建后端..."
cd backend
mvn clean package -DskipTests
cd ..

# 构建前端
echo "构建前端..."
cd frontend
npm ci
npm run build
cd ..

# 重启服务
echo "重启服务..."
docker compose down
docker compose up -d --build

echo "部署完成！"
```

### Git Hooks

```bash
#!/bin/bash
# .git/hooks/pre-commit

# 运行代码检查
echo "运行代码检查..."

# Java 代码检查
cd backend
mvn checkstyle:check spotbugs:check
if [ $? -ne 0 ]; then
    echo "代码检查失败，请修复后重新提交"
    exit 1
fi
cd ..

# 前端代码检查
cd frontend
npm run lint
if [ $? -ne 0 ]; then
    echo "代码检查失败，请修复后重新提交"
    exit 1
fi
cd ..

echo "代码检查通过"
```

## 开发环境一致性

### DevContainer

```json
// .devcontainer/devcontainer.json
{
  "name": "CodeNow Dev",
  "image": "mcr.microsoft.com/devcontainers/java:21",
  "features": {
    "ghcr.io/devcontainers/features/node:1": {
      "version": "22"
    },
    "ghcr.io/devcontainers/features/docker-in-docker:2": {}
  },
  "forwardPorts": [8080, 5173, 3306, 6379],
  "postCreateCommand": "cd backend && mvn clean install -DskipTests && cd ../frontend && npm ci",
  "customizations": {
    "vscode": {
      "extensions": [
        "vscjava.vscode-java-pack",
        "dbaeumer.vscode-eslint",
        "esbenp.prettier-vscode",
        "ms-azuretools.vscode-docker"
      ]
    }
  }
}
```

### Docker Compose 开发环境

```yaml
# docker-compose.dev.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: 123456
      MYSQL_DATABASE: codenow
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass 123456
    ports:
      - "6379:6379"

volumes:
  mysql_data:
```

## 时间管理

### 番茄工作法

```
工作流程：
1. 选择一个任务
2. 设置 25 分钟计时器（一个番茄钟）
3. 专注工作直到计时器响起
4. 短休息 5 分钟
5. 每 4 个番茄钟后，长休息 15-30 分钟

注意事项：
- 番茄钟不可分割
- 如果被打断，重新开始
- 记录每天完成的番茄钟数量
```

### 任务管理

```markdown
## 今日任务

### 高优先级
- [ ] 完成用户注册接口开发（预计 2 个番茄钟）
- [ ] 修复登录 Bug（预计 1 个番茄钟）

### 中优先级
- [ ] 编写单元测试（预计 2 个番茄钟）
- [ ] 代码评审（预计 1 个番茄钟）

### 低优先级
- [ ] 整理文档（预计 1 个番茄钟）

## 完成情况
完成：6 个番茄钟
实际：5 个番茄钟
原因：下午有个紧急会议
```

### 减少干扰

```
1. 关闭通知
   - 邮件通知
   - 即时消息通知
   - 手机通知

2. 设定专注时间
   - 告知团队你的专注时间段
   - 除非紧急情况，不要打断

3. 使用工具
   - Forest App（种树计时）
   - Toggl（时间追踪）
   - Notion（任务管理）
```

## 代码质量习惯

### 提交前检查清单

```markdown
## 提交前检查

- [ ] 代码编译通过
- [ ] 单元测试通过
- [ ] 代码格式化
- [ ] 没有 TODO 遗留
- [ ] 没有调试代码（System.out.println）
- [ ] 没有硬编码的配置
- [ ] 注释清晰
- [ ] 提交信息规范
```

### Code Review 检查清单

```markdown
## Code Review 检查

### 功能正确性
- [ ] 逻辑是否正确
- [ ] 边界条件处理
- [ ] 异常处理

### 代码质量
- [ ] 命名清晰
- [ ] 方法长度适中
- [ ] 没有重复代码
- [ ] 注释充分

### 性能
- [ ] 没有 N+1 查询
- [ ] 没有内存泄漏风险
- [ ] 缓存使用合理

### 安全
- [ ] SQL 注入防护
- [ ] XSS 防护
- [ ] 敏感信息未暴露
```

高效编程不是一蹴而就的，需要持续练习和积累。从今天开始，选择一两个技巧尝试，逐步养成习惯。当这些技巧成为你的第二天生时，你会发现自己的开发效率有了质的飞跃。
