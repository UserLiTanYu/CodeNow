# 生产部署与 CI 验收报告

验收日期：2026-07-15

## 1. 任务与风险分类

| 分类 | 处理事项 | 主要风险 |
|---|---|---|
| 部署可靠性 | 全新 Compose 构建、服务依赖和健康检查 | 镜像无法构建、服务循环重启、探针误报 |
| 数据一致性 | 升级脚本幂等、结构回滚、全库恢复 | 重复升级失败、字段与应用版本不匹配、数据丢失 |
| 安全与配置 | 非 root 容器、上传卷权限、CORS 白名单 | 越权写文件、上传失败、浏览器请求被错误放行或拒绝 |
| 核心功能 | 登录、文章发布、评论、图片上传与公开读取 | 部署成功但业务链路不可用 |
| 自动化质量 | JUnit、Vitest、覆盖率、Lint、构建、Compose CI | 修复成果缺少持续回归保护 |

## 2. 关键修复

| 问题 | 修复方案 | 关键文件 |
|---|---|---|
| Maven 产物名与 Dockerfile 不一致 | 固定 Maven `finalName`，确保镜像稳定复制 JAR | `codenow-backend/pom.xml` |
| 非 root Nginx 无法写临时目录/PID | 准备可写目录，将 PID 放入 `/tmp`，继续以 `nginx` 用户运行 | `codenow-frontend/Dockerfile` |
| Nginx 健康检查解析到 IPv6 后误报 | 探针改用 `127.0.0.1` | `docker-compose.yml` |
| 本地演练缺少可用上传存储 | 增加条件化本地存储和文件读取端点，OSS 仍可按配置启用 | `LocalStorageServiceImpl.java`、`LocalFileController.java` |
| 持久化上传卷归 root 所有 | 固定后端 UID/GID，并用一次性 `storage-init` 初始化卷权限 | 后端 `Dockerfile`、`docker-compose.yml` |
| 自定义前端端口被 CORS 拒绝 | 将精确来源白名单参数化为 `CORS_ALLOWED_ORIGINS` | `CorsConfig.java`、`.env.example` |
| 上传 URL 被 Nginx 静态正则截获 | 将 `/api/` 改为高优先级前缀，并通过前端代理回读上传文件 | `nginx.conf`、冒烟脚本 |
| 升级 SQL 受编码及重复执行影响 | 使用 `information_schema` + 动态 SQL 实现纯 ASCII 幂等升级；提供幂等回滚 | `migration-medium-priority.sql`、`rollback-medium-priority.sql` |
| 缺少持续质量门禁 | 增加三作业 CI：后端测试、前端质量、完整 Compose 冒烟及回滚 | `.github/workflows/ci.yml` |

## 3. 验证结果

| 验证项 | 结果 | 备注 |
|---|---|---|
| 后端测试 | 成功 | 26 项，0 失败、0 错误 |
| 前端测试 | 成功 | 7 项，0 失败 |
| 前端覆盖率门槛 | 成功 | Statements 81.57%、Branches 85.10%、Functions 61.11%、Lines 82.85% |
| 前端 Lint / 生产构建 | 成功 | ESLint + Oxlint、Vite 构建均通过 |
| Compose 全新部署 | 成功 | MySQL、Redis、后端、前端健康；`storage-init` 正常退出 0 |
| 升级脚本重复执行 | 成功 | 连续执行两次均返回 `migration already applied` |
| 回滚与恢复 | 成功 | 字段计数 `1 → 0 → 1`；恢复后 3 篇既有演练文章保留 |
| 核心 API 冒烟 | 成功 | 登录、分类/标签、发布、待审核评论、上传及文件回读、公开详情 |
| 真实浏览器登录 | 成功 | Playwright 登录后进入文章管理页 |
| 真实浏览器公开页面 | 成功 | 最新文章出现在博客首页，详情页 Markdown 和评论表单正常渲染 |

演练过程中实际发现并修复了 JAR 命名、Nginx 权限/PID、IPv6 健康探针、上传卷权限、迁移 SQL 编码、CORS 自定义端口、上传 URL 代理优先级共七类部署阻断问题。每次修复后均重新构建并复验相关链路。

## 4. CI 与合并门禁

GitHub Actions 检查名称：

- `backend-tests`：JDK 21 + Maven clean test。
- `frontend-quality`：Node 22.22.2 + npm ci + Lint + Vitest 覆盖率 + 构建。
- `compose-smoke`：完整镜像构建、迁移两次、回滚/重放、核心 API 冒烟。

仓库管理员仍需在 GitHub 分支保护中将以上三项设为必需状态检查；此设置属于远端仓库权限配置，无法仅通过代码提交强制启用。

## 5. 遗留与上线建议

- 本地演练采用 `STORAGE_TYPE=local`。正式生产建议改用 OSS，并在预发布环境用真实 OSS 权限再做一次上传/删除验证。
- 评论创建后默认为待审核，因此公开详情显示评论数为 0 属于预期；后台接口已确认评论记录成功写入。
- 本次回滚备份保存在数据库容器临时目录用于演练。生产执行时必须将备份复制到容器外的持久化、受控位置并做恢复抽检。
- 默认管理员密码仅适合初始化，正式上线前必须更换并轮换所有演练密钥。
