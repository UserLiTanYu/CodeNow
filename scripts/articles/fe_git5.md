# Git 高级操作与问题恢复

## git stash 暂存工作现场

在日常开发中，经常会遇到需要临时切换分支处理紧急任务的情况。`git stash` 可以将当前工作区的修改暂存起来，让工作区恢复到干净状态。

### 基本用法

```bash
# 暂存当前修改
git stash

# 暂存时添加描述信息
git stash push -m "正在开发的用户登录功能"

# 暂存指定文件
git stash push -m "暂存配置文件" config/application.yml

# 暂存包括未跟踪的文件
git stash push -u -m "包含新文件的暂存"

# 暂存所有修改（包括忽略的文件）
git stash push -a -m "所有文件暂存"
```

### 查看和恢复

```bash
# 查看暂存列表
git stash list
# 输出示例：
# stash@{0}: On feature/login: 正在开发的用户登录功能
# stash@{1}: WIP on develop: abc123 添加用户模块
# stash@{2}: On feature/search: 搜索功能优化

# 查看暂存内容
git stash show stash@{0}
git stash show -p stash@{0}  # 显示详细差异

# 恢复暂存（保留暂存记录）
git stash apply stash@{0}

# 恢复暂存（删除暂存记录）
git stash pop stash@{0}

# 恢复最近一次暂存
git stash pop
```

### 高级操作

```bash
# 创建分支并应用暂存
git stash branch new-feature-branch stash@{0}

# 删除指定暂存
git stash drop stash@{0}

# 清空所有暂存
git stash clear

# 从暂存中提取指定文件
git checkout stash@{0} -- path/to/file
```

### 实际应用场景

**场景一：紧急修复 bug**

```bash
# 当前正在开发新功能
git status
# modified: src/feature/new-feature.js

# 暂存当前工作
git stash push -m "新功能开发到一半"

# 切换到 main 分支创建修复分支
git checkout main
git checkout -b hotfix/fix-critical-bug

# 修复 bug 并提交
git add .
git commit -m "fix: 修复关键 bug"

# 切换回原分支
git checkout feature/new-feature

# 恢复暂存
git stash pop
```

**场景二：拉取远程更新**

```bash
# 本地有修改，需要拉取远程更新
git stash
git pull origin main
git stash pop
```

## git cherry-pick 摘取提交

`git cherry-pick` 可以将指定的提交应用到当前分支，常用于将特定的修复或功能从一个分支应用到另一个分支。

### 基本用法

```bash
# 摘取单个提交
git cherry-pick abc1234

# 摘取多个提交
git cherry-pick abc1234 def5678 ghi9012

# 摘取提交范围
git cherry-pick abc1234..def5678

# 摘取提交范围（包含起始提交）
git cherry-pick abc1234^..def5678
```

### 高级选项

```bash
# 只应用修改，不自动提交
git cherry-pick --no-commit abc1234

# 保留原始作者信息
git cherry-pick -x abc1234

# 解决冲突后继续
git cherry-pick --continue

# 放弃 cherry-pick
git cherry-pick --abort

# 摘取合并提交（指定父提交）
git cherry-pick -m 1 merge-commit-hash
```

### 实际应用

**场景一：将修复应用到多个版本分支**

```bash
# 在 develop 分支上修复了一个 bug
git checkout develop
git commit -m "fix: 修复用户头像上传失败的问题"

# 记录提交哈希
git log --oneline -1
# abc1234 fix: 修复用户头像上传失败的问题

# 将修复应用到 release/1.0 分支
git checkout release/1.0
git cherry-pick abc1234

# 将修复应用到 release/2.0 分支
git checkout release/2.0
git cherry-pick abc1234
```

**场景二：从功能分支提取部分提交**

```bash
# 查看功能分支的提交历史
git log feature/new-dashboard --oneline
# def5678 添加仪表盘图表
# abc1234 修复数据加载问题
# ghi9012 添加用户权限检查

# 只需要数据加载修复
git checkout main
git cherry-pick abc1234
```

## git bisect 二分查找 bug

`git bisect` 使用二分查找算法快速定位引入 bug 的提交，特别适合在大量提交中找到问题根源。

### 基本流程

```bash
# 开始二分查找
git bisect start

# 标记当前版本有问题
git bisect bad

# 标记一个正常的版本
git bisect good v1.0.0

# Git 会自动检出中间的提交
# 测试当前版本后标记
git bisect good  # 如果这个版本正常
git bisect bad   # 如果这个版本有问题

# 找到问题提交后结束
git bisect reset
```

### 自动化二分查找

```bash
# 使用自动化脚本进行二分查找
git bisect start
git bisect bad HEAD
git bisect good v1.0.0

# 自动执行测试脚本
git bisect run npm test

# 或者使用自定义脚本
git bisect run ./test-script.sh
```

### 测试脚本示例

```bash
#!/bin/bash
# test-script.sh

# 运行测试
npm test

# 根据测试结果返回
# 0: 测试通过（good）
# 1-124, 126-127: 测试失败（bad）
# 125: 跳过此提交
exit $?
```

### 实际案例

```bash
# 场景：发现登录功能有问题，定位问题提交
git bisect start

# 当前版本有问题
git bisect bad

# 一周前的版本是正常的
git bisect good 2024-01-01

# Git 检出中间的提交
# 测试登录功能...

# 如果当前版本有问题
git bisect bad

# 如果当前版本正常
git bisect good

# 继续二分直到找到问题提交
# 最终输出：
# abc1234 is the first bad commit
# commit abc1234
# Author: developer@example.com
# Date:   2024-01-05
# 
#     修改了认证逻辑

# 结束二分查找
git bisect reset
```

### 可视化二分查找

```bash
# 查看二分查找过程
git bisect log

# 重放二分查找过程
git bisect replay bisect-log.txt

# 跳过无法测试的提交
git bisect skip
```

## git reflog 恢复误操作

`git reflog` 记录了 HEAD 和分支引用的所有变更历史，是恢复误操作的最后防线。

### 查看引用日志

```bash
# 查看 HEAD 的引用日志
git reflog

# 查看特定分支的引用日志
git reflog main

# 查看最近 10 条记录
git reflog -10

# 以日期格式显示
git reflog --date=relative
```

### 恢复误删分支

```bash
# 不小心删除了分支
git branch -D important-feature

# 查看引用日志找到删除前的提交
git reflog
# abc1234 HEAD@{5}: checkout: moving from important-feature to main

# 恢复分支
git checkout -b important-feature abc1234
```

### 恢复误操作的提交

```bash
# 不小心重置了提交
git reset --hard HEAD~3

# 查看引用日志
git reflog
# def5678 HEAD@{0}: reset: moving to HEAD~3
# abc1234 HEAD@{1}: commit: 添加重要功能

# 恢复到重置前的状态
git reset --hard abc1234
```

### 恢复误合并

```bash
# 不小心合并了错误的分支
git merge feature-wrong-branch

# 查看引用日志
git reflog
# abc1234 HEAD@{0}: merge feature-wrong-branch: Merge made by the 'ort' strategy.
# def5678 HEAD@{1}: commit: 正常的提交

# 恢复到合并前
git reset --hard def5678
```

### 恢复误 rebase

```bash
# rebase 过程中出现问题
git rebase --abort  # 如果还在 rebase 中

# 如果已经完成了 rebase
git reflog
# 找到 rebase 前的提交
git reset --hard HEAD@{10}  # 根据 reflog 中的位置
```

## git rebase -i 交互式变基

交互式变基可以重新整理提交历史，让提交记录更加清晰。

### 基本用法

```bash
# 对最近 5 个提交进行交互式变基
git rebase -i HEAD~5

# 或者指定起始提交
git rebase -i abc1234
```

### 操作命令

在编辑器中，可以对每个提交执行以下操作：

```bash
pick abc1234 第一个提交      # 保留提交
reword def5678 第二个提交    # 修改提交信息
edit ghi9012 第三个提交      # 编辑提交内容
squash jkl3456 第四个提交    # 压缩到上一个提交
fixup mno7890 第五个提交     # 压缩并丢弃提交信息
drop pqr1234 第六个提交      # 删除提交
exec ./run-tests.sh          # 执行命令
break                        # 暂停变基
```

### 压缩提交

```bash
# 将多个小提交压缩为一个
git rebase -i HEAD~4

# 编辑器内容：
pick abc1234 feat: 添加用户模型
squash def5678 feat: 添加用户验证
squash ghi9012 fix: 修复验证逻辑
squash jkl3456 style: 代码格式化

# 保存后编辑最终的提交信息
feat: 完整的用户认证功能
```

### 重新排序提交

```bash
# 调整提交顺序
git rebase -i HEAD~3

# 编辑器中调整顺序：
pick ghi9012 第三个提交（移到最前）
pick abc1234 第一个提交
pick def5678 第二个提交
```

### 拆分提交

```bash
# 将一个提交拆分为多个
git rebase -i HEAD~3

# 标记要拆分的提交为 edit
edit abc1234 大型提交

# Git 会停在这个提交
git reset HEAD^  # 取消提交但保留修改

# 分多次提交
git add file1.js
git commit -m "feat: 添加文件1"

git add file2.js
git commit -m "feat: 添加文件2"

# 继续变基
git rebase --continue
```

### 解决变基冲突

```bash
# 变基过程中遇到冲突
git rebase -i main
# CONFLICT (content): Merge conflict in src/app.js

# 解决冲突后
git add src/app.js
git rebase --continue

# 如果想放弃变基
git rebase --abort

# 跳过当前提交
git rebase --skip
```

## git worktree 多工作区

`git worktree` 允许在同一仓库中同时检出多个分支，无需频繁切换。

### 基本操作

```bash
# 查看所有工作区
git worktree list

# 添加新工作区
git worktree add ../project-feature feature/new-feature

# 添加新工作区并创建新分支
git worktree add -b hotfix/urgent-fix ../project-hotfix

# 删除工作区
git worktree remove ../project-feature

# 清理无效的工作区引用
git worktree prune
```

### 实际应用场景

**场景一：同时开发和修复**

```bash
# 主工作区在开发新功能
cd /path/to/project
git checkout feature/new-dashboard

# 创建新工作区处理紧急修复
git worktree add ../project-hotfix hotfix/fix-critical-bug

# 在另一个终端中处理修复
cd /path/to/project-hotfix
# 修复 bug...
git add .
git commit -m "fix: 修复关键 bug"
git push origin hotfix/fix-critical-bug

# 删除临时工作区
git worktree remove ../project-hotfix
```

**场景二：代码审查**

```bash
# 创建工作区查看 PR 代码
git fetch origin
git worktree add ../project-pr-review origin/feature/colleague-feature

# 在新工作区中审查代码
cd /path/to/project-pr-review
# 测试代码...

# 审查完成后删除
git worktree remove ../project-pr-review
```

### 工作区管理最佳实践

```bash
# 使用相对路径创建规范的工作区结构
mkdir -p ~/worktrees/myproject
cd ~/worktrees/myproject

# 主工作区
git clone git@github.com:user/repo.git main
cd main

# 功能工作区
git worktree add ../feature-login feature/login
git worktree add ../feature-search feature/search

# 查看所有工作区
git worktree list
# /home/user/worktrees/myproject/main        abc1234 [main]
# /home/user/worktrees/myproject/feature-login def5678 [feature/login]
# /home/user/worktrees/myproject/feature-search ghi9012 [feature/search]
```

## 子模块（submodule）管理

Git 子模块允许在一个仓库中嵌入另一个仓库，常用于管理公共库或组件。

### 添加子模块

```bash
# 添加子模块
git submodule add https://github.com/user/shared-lib.git libs/shared-lib

# 添加到指定目录
git submodule add https://github.com/user/ui-components.git src/components/ui

# 添加指定分支
git submodule add -b develop https://github.com/user/api-client.git libs/api-client
```

### 克隆包含子模块的仓库

```bash
# 方法一：克隆时初始化子模块
git clone --recursive https://github.com/user/project.git

# 方法二：克隆后初始化子模块
git clone https://github.com/user/project.git
cd project
git submodule init
git submodule update

# 方法三：一步完成
git clone https://github.com/user/project.git
cd project
git submodule update --init --recursive
```

### 更新子模块

```bash
# 更新子模块到最新提交
cd libs/shared-lib
git pull origin main
cd ../..
git add libs/shared-lib
git commit -m "chore: 更新 shared-lib 子模块"

# 更新所有子模块
git submodule update --remote

# 更新指定子模块
git submodule update --remote libs/shared-lib

# 批量更新子模块
git submodule foreach git pull origin main
```

### 删除子模块

```bash
# 1. 从 .gitmodules 中删除
git config -f .gitmodules --remove-section submodule.libs/shared-lib

# 2. 从 .git/config 中删除
git config --remove-section submodule.libs/shared-lib

# 3. 从暂存区删除
git rm --cached libs/shared-lib

# 4. 删除子模块目录
rm -rf libs/shared-lib

# 5. 删除 .git/modules 中的子模块
rm -rf .git/modules/libs/shared-lib

# 6. 提交更改
git add .gitmodules
git commit -m "chore: 删除 shared-lib 子模块"
```

### 子模块的 .gitmodules 配置

```ini
# .gitmodules 文件示例
[submodule "libs/shared-lib"]
    path = libs/shared-lib
    url = https://github.com/user/shared-lib.git
    branch = main

[submodule "libs/api-client"]
    path = libs/api-client
    url = https://github.com/user/api-client.git
    branch = develop
    ignore = dirty
```

### 子模块的替代方案：Git Subtree

```bash
# 添加 subtree
git subtree add --prefix=libs/shared-lib https://github.com/user/shared-lib.git main --squash

# 更新 subtree
git subtree pull --prefix=libs/shared-lib https://github.com/user/shared-lib.git main --squash

# 推送更改到子项目
git subtree push --prefix=libs/shared-lib https://github.com/user/shared-lib.git main
```

## 恢复丢失的提交

### 使用 git fsck 查找悬空对象

```bash
# 查找悬空的提交
git fsck --no-reflogs | grep commit

# 查找所有不可达的对象
git fsck --unreachable

# 查找丢失的提交
git log --all --full-history --oneline | head -20
```

### 恢复已删除的提交

```bash
# 方法一：使用 reflog
git reflog
git checkout <commit-hash>
git checkout -b recovered-branch

# 方法二：使用 git fsck
git fsck --no-reflogs | grep "dangling commit"
git show <dangling-commit-hash>
git cherry-pick <dangling-commit-hash>
```

### 恢复已删除的文件

```bash
# 查找文件被删除的提交
git log --diff-filter=D --summary | grep delete

# 恢复文件
git checkout <commit>~1 -- path/to/deleted/file

# 或者使用 restore
git restore --source=<commit>~1 path/to/deleted/file
```

## 高级别名配置

```bash
# 配置常用别名
git config --global alias.st "status -sb"
git config --global alias.co "checkout"
git config --global alias.br "branch"
git config --global alias.ci "commit"
git config --global alias.unstage "reset HEAD --"
git config --global alias.last "log -1 HEAD"
git config --global alias.lg "log --oneline --graph --decorate --all"
git config --global alias.df "diff"
git config --global alias.dfc "diff --cached"
git config --global alias.amend "commit --amend --no-edit"

# 复杂别名
git config --global alias.cleanup "!git branch --merged main | grep -v main | xargs -n 1 git branch -d"
git config --global alias.wip "!git add -A && git commit -m 'WIP'"
git config --global alias.undo "reset --soft HEAD~1"
```

## Git 钩子（Hooks）

```bash
# pre-commit 示例：运行代码检查
#!/bin/bash
# .git/hooks/pre-commit

# 运行 ESLint
npm run lint
if [ $? -ne 0 ]; then
    echo "ESLint 检查失败，请修复后再提交"
    exit 1
fi

# 运行测试
npm test
if [ $? -ne 0 ]; then
    echo "测试失败，请修复后再提交"
    exit 1
fi

exit 0
```

```bash
# commit-msg 示例：检查提交信息格式
#!/bin/bash
# .git/hooks/commit-msg

commit_msg=$(cat "$1")
pattern="^(feat|fix|docs|style|refactor|test|chore)(\(.+\))?: .{1,72}$"

if ! echo "$commit_msg" | grep -qE "$pattern"; then
    echo "提交信息格式错误！"
    echo "格式应为: type(scope): description"
    echo "示例: feat(auth): 添加用户登录功能"
    exit 1
fi

exit 0
```
